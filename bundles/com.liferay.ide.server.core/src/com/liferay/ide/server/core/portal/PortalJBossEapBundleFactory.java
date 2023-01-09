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

package com.liferay.ide.server.core.portal;

import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.PropertiesUtil;
import com.liferay.ide.server.util.JavaUtil;

import java.io.File;

import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @author Simon Jiang
 * @author Charles Wu
 */
public class PortalJBossEapBundleFactory extends PortalJBossBundleFactory {

	public static String getEAPVersionNoSlotCheck(
		File location, String metaInfPath, String[] versionPrefixs, String releaseName) {

		IPath rootPath = new Path(location.getAbsolutePath());

		IPath eapDir = rootPath.append(metaInfPath);

		if (FileUtil.exists(eapDir)) {
			IPath manifest = eapDir.append("MANIFEST.MF");

			String type = JavaUtil.getManifestProperty(manifest.toFile(), "JBoss-Product-Release-Name");
			String version = JavaUtil.getManifestProperty(manifest.toFile(), "JBoss-Product-Release-Version");

			boolean matchesName = type.contains(releaseName);

			for (String prefixVersion : versionPrefixs) {
				boolean matchesVersion = version.startsWith(prefixVersion);

				if (matchesName && matchesVersion) {
					return version;
				}
			}
		}

		return null;
	}

	@Override
	public PortalBundle create(IPath location) {
		return new PortalJBossEapBundle(location);
	}

	@Override
	public PortalBundle create(Map<String, String> appServerProperties) {
		return new PortalJBossEapBundle(appServerProperties);
	}

	@Override
	protected boolean detectBundleDir(IPath path) {
		if (FileUtil.notExists(path)) {
			return false;
		}

		IPath modulesPath = path.append("modules");
		IPath standalonePath = path.append("standalone");
		IPath binPath = path.append("bin");

		if (FileUtil.exists(modulesPath) && FileUtil.exists(standalonePath) && FileUtil.exists(binPath)) {
			String eapVersion = _getEAPVersion(
				path.toFile(), _EAP_DIR_META_INF, new String[] {"6.", "7."}, "eap", "EAP");

			if (eapVersion != null) {
				return true;
			}

			return super.detectBundleDir(path);
		}

		return false;
	}

	private String _getEAPVersion(
		File location, String metaInfPath, String[] versionPrefix, String slot, String releaseName) {

		IPath rootPath = new Path(location.getAbsolutePath());

		IPath productConf = rootPath.append("bin/product.conf");

		if (FileUtil.exists(productConf)) {
			Properties p = PropertiesUtil.loadProperties(productConf.toFile());

			if (p != null) {
				String product = (String)p.get("slot");

				if (slot.equals(product)) {
					return getEAPVersionNoSlotCheck(location, metaInfPath, versionPrefix, releaseName);
				}
			}
		}

		return null;
	}

	private static final String _EAP_DIR_META_INF = "modules/system/layers/base/org/jboss/as/product/eap/dir/META-INF";

}