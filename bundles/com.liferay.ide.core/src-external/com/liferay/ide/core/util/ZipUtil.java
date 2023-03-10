/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.core.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.file.Files;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * Contains a series of static utility methods for working with zip archives.
 *
 * @author <a href="mailto:konstantin.komissarchik@oracle.com">Konstantin
 *         Komissarchik</a>
 */
public final class ZipUtil {

	public static String getFirstZipEntryName(File zipFile) throws Exception {
		ZipFile zip = new ZipFile(zipFile);

		Enumeration<? extends ZipEntry> itr = zip.entries();

		ZipEntry nextEntry = itr.nextElement();

		String name = nextEntry.getName();

		zip.close();

		return name;
	}

	public static ZipEntry getZipEntry(ZipFile zip, String name) {
		String lowerCaseName = name.toLowerCase();

		for (Enumeration<? extends ZipEntry> itr = zip.entries(); itr.hasMoreElements();) {
			ZipEntry zipEntry = itr.nextElement();

			String zipEntryName = zipEntry.getName();

			if (lowerCaseName.equals(zipEntryName.toLowerCase())) {
				return zipEntry;
			}
		}

		return null;
	}

	public static ZipFile open(File file) throws IOException {
		try {
			return new ZipFile(file);
		}
		catch (FileNotFoundException fnfe) {
			FileNotFoundException ecxcption = new FileNotFoundException(file.getAbsolutePath());

			ecxcption.initCause(fnfe);

			throw ecxcption;
		}
	}

	public static void unzip(File file, File destdir) throws IOException {
		unzip(file, destdir, new NullProgressMonitor());
	}

	public static void unzip(File file, File destdir, IProgressMonitor monitor) throws IOException {
		unzip(file, null, destdir, monitor);
	}

	public static void unzip(File file, File destDir, PathFilter pathFilter) throws IOException {
		ZipFile zipFile = open(file);

		try {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			Map<String, File> folders = new HashMap<>();

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();

				String entryName = entry.getName();

				if (!folders.isEmpty()) {
					boolean hasCopied = false;

					for (Map.Entry<String, File> e : folders.entrySet()) {
						if (entryName.startsWith(e.getKey())) {
							//if the entry folder is accepted that means the sub-nodes should be accepted too

							_copyEntry(zipFile, entry, e.getValue());
							hasCopied = true;

							break;
						}
					}

					if (hasCopied) {
						continue;
					}
				}

				if (pathFilter != null) {
					Pair<Boolean, File> pair = pathFilter.accept(entryName);

					if (pair.first()) {
						if (entry.isDirectory()) {
							folders.put(entryName, pair.second());
						}

						_copyEntry(zipFile, entry, pair.second());
					}
				}
				else {
					_copyEntry(zipFile, entry, destDir);
				}
			}
		}
		finally {
			try {
				zipFile.close();
			}
			catch (IOException ioe) {
			}
		}
	}

	public static void unzip(File file, String entryToStart, File destdir, IProgressMonitor monitor)
		throws IOException {

		ZipFile zip = open(file);

		try {
			Enumeration<? extends ZipEntry> entries = zip.entries();

			int totalWork = zip.size();

			monitor.beginTask(Resources.progressUnzipping, totalWork);

			int c = 0;

			boolean foundStartEntry = false;

			if (entryToStart == null) {
				foundStartEntry = true;
			}

			while (entries.hasMoreElements()) {
				if (monitor.isCanceled()) {
					break;
				}

				ZipEntry entry = entries.nextElement();

				String name = entry.getName();

				if (!foundStartEntry) {
					foundStartEntry = entryToStart.equals(name);

					continue;
				}

				monitor.worked(1);

				String taskMsg = NLS.bind(Resources.progressUnzipped, new Object[] {file.getName(), c++, totalWork});

				monitor.subTask(taskMsg);

				String entryName = null;

				if (entryToStart == null) {
					entryName = name;
				}
				else {
					entryName = name.replaceFirst(entryToStart, "");
				}

				if (entry.isDirectory()) {
					File emptyDir = new File(destdir, entryName);

					_mkdir(emptyDir);

					continue;
				}

				File f = new File(destdir, entryName);

				File dir = f.getParentFile();

				_mkdir(dir);

				try (InputStream in = zip.getInputStream(entry); OutputStream out = Files.newOutputStream(f.toPath())) {
					byte[] bytes = new byte[1024];

					int count = in.read(bytes);

					while (count != -1) {
						out.write(bytes, 0, count);

						count = in.read(bytes);
					}

					out.flush();
				}
			}
		}
		finally {
			try {
				zip.close();
			}
			catch (IOException ioe) {
			}
		}
	}

	public static void unzip(InputStream inputStream, File destinationDir) throws IOException {
		try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
			ZipEntry zipEntry = null;

			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				String entryName = zipEntry.getName();

				if (zipEntry.isDirectory()) {
					continue;
				}

				final File f = new File(destinationDir, entryName);

				if (f.exists()) {
					Files.delete(f.toPath());

					if (f.exists()) {
						throw new IOException("Could not delete " + f.getAbsolutePath());
					}
				}

				final File dir = f.getParentFile();

				if (!dir.exists() && !dir.mkdirs()) {
					final String msg = "Could not create dir: " + dir.getPath();

					throw new IOException(msg);
				}

				try (final FileOutputStream out = new FileOutputStream(f)) {
					final byte[] bytes = new byte[1024];

					int count = zipInputStream.read(bytes);

					while (count != -1) {
						out.write(bytes, 0, count);
						count = zipInputStream.read(bytes);
					}

					out.flush();
				}

				zipInputStream.closeEntry();
			}
		}
	}

	public static void zip(File dir, File target) throws IOException {
		zip(dir, null, target);
	}

	public static void zip(File dir, FilenameFilter filenameFilter, File target) throws IOException {
		if (target.exists()) {
			_delete(target);
		}

		try (OutputStream output = Files.newOutputStream(target.toPath());
			ZipOutputStream zip = new ZipOutputStream(output)) {

			_zipDir(target, zip, dir, filenameFilter, "");
		}
	}

	@FunctionalInterface
	public interface PathFilter {

		/**
		 * A filter for zip entry
		 *
		 * @return a pair of return values, if the input entry path is accepted then
		 * return true and the expected directory, otherwise return false and null.
		 */
		public Pair<Boolean, File> accept(String entryPath);

	}

	private static void _copyEntry(ZipFile zip, ZipEntry entry, File destDir) throws IOException {
		String entryName = entry.getName();

		if (entry.isDirectory()) {
			File emptyDir = new File(destDir, entryName);

			_mkdir(emptyDir);

			return;
		}

		File file = new File(destDir, entryName);

		File dir = file.getParentFile();

		_mkdir(dir);

		try (InputStream in = zip.getInputStream(entry); FileOutputStream out = new FileOutputStream(file)) {
			byte[] bytes = new byte[1024];

			int count = in.read(bytes);

			while (count != -1) {
				out.write(bytes, 0, count);
				count = in.read(bytes);
			}

			out.flush();
		}
	}

	private static void _delete(File f) throws IOException {
		if (f.isDirectory()) {
			for (File child : f.listFiles()) {
				_delete(child);
			}
		}

		if (!f.delete()) {
			String msg = "Could not delete " + f.getPath() + ".";

			throw new IOException(msg);
		}
	}

	private static void _mkdir(File dir) throws IOException {
		if (!dir.exists() && !dir.mkdirs()) {
			String msg = "Could not create dir: " + dir.getPath();

			throw new IOException(msg);
		}
	}

	private static void _zipDir(File target, ZipOutputStream zip, File dir, FilenameFilter filter, String path)
		throws IOException {

		for (File f : (filter != null) ? dir.listFiles(filter) : dir.listFiles()) {
			String cpath = path + f.getName();

			if (f.isDirectory()) {
				_zipDir(target, zip, f, filter, cpath + "/");
			}
			else {
				_zipFile(target, zip, f, cpath);
			}
		}
	}

	private static void _zipFile(File target, ZipOutputStream zip, File file, String path) throws IOException {
		if (file.equals(target)) {
			return;
		}

		ZipEntry ze = new ZipEntry(path);

		ze.setTime(file.lastModified() + 1999);

		ze.setMethod(ZipEntry.DEFLATED);

		zip.putNextEntry(ze);

		try (InputStream in = Files.newInputStream(file.toPath())) {
			int bufsize = 8 * 1024;

			long flength = file.length();

			if (flength == 0) {
				return;
			}
			else if (flength < bufsize) {
				bufsize = (int)flength;
			}

			byte[] buffer = new byte[bufsize];

			int count = in.read(buffer);

			while (count != -1) {
				zip.write(buffer, 0, count);

				count = in.read(buffer);
			}
		}
	}

	private ZipUtil() {
	}

	private static final class Resources extends NLS {

		public static String progressUnzipped;
		public static String progressUnzipping;

		static {
			initializeMessages(ZipUtil.class.getName(), Resources.class);
		}

	}

}