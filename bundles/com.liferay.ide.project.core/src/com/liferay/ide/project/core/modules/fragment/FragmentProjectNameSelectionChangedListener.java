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

package com.liferay.ide.project.core.modules.fragment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.sapphire.Element;
import org.eclipse.sapphire.ElementList;
import org.eclipse.sapphire.Property;
import org.eclipse.sapphire.PropertyContentEvent;
import org.eclipse.sapphire.platform.PathBridge;

import com.liferay.ide.core.ILiferayProject;
import com.liferay.ide.core.IProjectBuilder;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.modules.BndProperties;
import com.liferay.ide.project.core.modules.BndPropertiesValue;


/**
 * @author Seiphon Wang
 * @author Ethan Sun
 */
public class FragmentProjectNameSelectionChangedListener extends FragmentProjectNameListener {

	@Override
	protected void handleTypedEvent(PropertyContentEvent event) {
		_updateProject(op(event));
	}

	@Override
	protected NewModuleFragmentFilesOp op(PropertyContentEvent event) {
		Property property = event.property();

		Element element = property.element();

		return element.nearest(NewModuleFragmentFilesOp.class);
	}

	public static Map<String, String> getFragmentProjectInfo(IProject project) {
		IFile bndFile = project.getFile("bnd.bnd");

		if (FileUtil.notExists(bndFile)) {
			return null;
		}

		try {
			Map<String, String> fragmentProjectInfo = new HashMap<>();

			BndProperties bndProperty = new BndProperties();

			bndProperty.load(FileUtil.getFile(bndFile));

			BndPropertiesValue portalBundleVersion = (BndPropertiesValue)bndProperty.get("Portal-Bundle-Version");

			if (portalBundleVersion != null) {
				fragmentProjectInfo.put("Portal-Bundle-Version", portalBundleVersion.getOriginalValue());
			}
			else {
				fragmentProjectInfo.put("Portal-Bundle-Version", null);
			}

			BndPropertiesValue fragmentHostValue = (BndPropertiesValue)bndProperty.get("Fragment-Host");

			if (fragmentHostValue != null) {
				String fragmentHost = fragmentHostValue.getOriginalValue();

				String[] hostOSGiBundleArray = fragmentHost.split(";");

				if (ListUtil.isNotEmpty(hostOSGiBundleArray) && (hostOSGiBundleArray.length > 1)) {
					String[] f = hostOSGiBundleArray[1].split("=");

					String version = f[1].substring(1, f[1].length() - 1);

					fragmentProjectInfo.put("HostOSGiBundleName", hostOSGiBundleArray[0] + "-" + version);
				}

				return fragmentProjectInfo;
			}

			return null;
		}
		catch (IOException ioe) {
			ProjectCore.logError("Failed to parsed bnd.bnd for project " + project.getName(), ioe);
		}

		return null;
	}
	
	private void _updateProject(NewModuleFragmentFilesOp op) {
		IProject project = CoreUtil.getProject(get(op.getProjectName()));

		ILiferayProject fragmentProject = LiferayCore.create(ILiferayProject.class, project);

		IProjectBuilder projectBuilder = fragmentProject.adapt(IProjectBuilder.class);

		if (projectBuilder == null) {
			ProjectCore.logWarning("Please wait for synchronized jobs to finish.");

			return;
		}

		Map<String, String> fragmentProjectInfo = getFragmentProjectInfo(project);

		op.setHostOsgiBundle(fragmentProjectInfo.get("HostOSGiBundleName"));
		op.setLiferayRuntimeName(fragmentProjectInfo.get("LiferayRuntimeName"));

		IPath projectLocation = project.getLocation();

		op.setLocation(PathBridge.create(projectLocation.removeLastSegments(1)));

		ElementList<OverrideFilePath> overrideFiles = op.getOverrideFiles();

		overrideFiles.clear();
	}

}