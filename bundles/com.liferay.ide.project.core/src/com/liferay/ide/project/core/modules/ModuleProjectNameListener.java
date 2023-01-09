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

package com.liferay.ide.project.core.modules;

import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.SapphireContentAccessor;
import com.liferay.ide.core.util.SapphireUtil;
import com.liferay.ide.core.util.StringUtil;
import com.liferay.ide.core.workspace.LiferayWorkspaceUtil;
import com.liferay.ide.project.core.NewLiferayProjectProvider;
import com.liferay.ide.project.core.ProjectCore;

import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.sapphire.Element;
import org.eclipse.sapphire.FilteredListener;
import org.eclipse.sapphire.PropertyContentEvent;
import org.eclipse.sapphire.modeling.Path;
import org.eclipse.sapphire.platform.PathBridge;

/**
 * @author Simon Jiang
 * @author Andy Wu
 * @author Joye Luo
 */
public class ModuleProjectNameListener
	extends FilteredListener<PropertyContentEvent> implements SapphireContentAccessor {

	@Override
	protected void handleTypedEvent(PropertyContentEvent event) {
		_updateLocation(op(event));
	}

	protected NewLiferayModuleProjectOp op(PropertyContentEvent event) {
		Element element = SapphireUtil.getElement(event);

		return element.nearest(NewLiferayModuleProjectOp.class);
	}

	private void _updateLocation(NewLiferayModuleProjectOp op) {
		String currentProjectName = get(op.getProjectName());

		if (CoreUtil.isNullOrEmpty(currentProjectName)) {
			return;
		}

		boolean useDefaultLocation = get(op.getUseDefaultLocation());

		if (useDefaultLocation) {
			Path newLocationBase = null;

			boolean hasLiferayWorkspace = false;

			try {
				hasLiferayWorkspace = LiferayWorkspaceUtil.hasWorkspace();
			}
			catch (Exception e) {
				ProjectCore.logError("Failed to check LiferayWorkspace project.");
			}

			if (hasLiferayWorkspace) {
				IProject workspaceProject = LiferayWorkspaceUtil.getWorkspaceProject();

				if (FileUtil.exists(workspaceProject)) {
					boolean warProject = false;

					String projectTemplateName = get(op.getProjectTemplateName());

					for (String projectType : _WAR_TYPE_PROJECT) {
						if (projectType.equals(projectTemplateName)) {
							warProject = true;
						}
					}

					IPath workspaceLocation = workspaceProject.getLocation();

					IWorkspaceProject liferayWorkspaceProject = LiferayWorkspaceUtil.getLiferayWorkspaceProject();

					if (warProject) {
						String[] defaultWarDirs = liferayWorkspaceProject.getWorkspaceWarDirs();

						if (Objects.nonNull(defaultWarDirs)) {
							NewLiferayProjectProvider<BaseModuleOp> projectProvider = get(op.getProjectProvider());

							if (StringUtil.equals(projectProvider.getDisplayName(), "Maven")) {
								newLocationBase = Stream.of(
									defaultWarDirs
								).map(
									warDir -> PathBridge.create(workspaceLocation.append(warDir))
								).filter(
									warDirPath -> FileUtil.exists(warDirPath.toFile())
								).findAny(
								).orElseGet(
									() -> PathBridge.create(workspaceLocation)
								);
							}
							else {
								newLocationBase = PathBridge.create(workspaceLocation.append(defaultWarDirs[0]));
							}
						}
						else {
							newLocationBase = PathBridge.create(workspaceLocation);
						}
					}
					else if (projectTemplateName.equals("war-core-ext")) {
						String extName = LiferayWorkspaceUtil.getExtDir(workspaceProject);

						newLocationBase = PathBridge.create(workspaceLocation.append(extName));
					}
					else {
						String[] defaultModuleDirs = liferayWorkspaceProject.getWorkspaceModuleDirs();

						if (Objects.nonNull(defaultModuleDirs)) {
							newLocationBase = PathBridge.create(workspaceLocation.append(defaultModuleDirs[0]));
						}
						else {
							newLocationBase = PathBridge.create(workspaceLocation);
						}
					}
				}
			}
			else {
				newLocationBase = PathBridge.create(CoreUtil.getWorkspaceRootLocation());
			}

			if (newLocationBase != null) {
				op.setLocation(newLocationBase);
			}
		}
	}

	private static final String[] _WAR_TYPE_PROJECT = {"layout-template", "theme", "war-hook", "war-mvc-portlet"};

}