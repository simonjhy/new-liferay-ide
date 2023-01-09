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

package com.liferay.ide.gradle.core;

import com.liferay.blade.gradle.tooling.ProjectInfo;
import com.liferay.ide.core.LiferayNature;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.project.core.util.ProjectUtil;

import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.Set;

import org.eclipse.buildship.core.InitializationContext;
import org.eclipse.buildship.core.ProjectConfigurator;
import org.eclipse.buildship.core.ProjectContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * @author Simon Jiang
 */
public class LiferayProjectConfigurator implements ProjectConfigurator {

	@Override
	public void configure(ProjectContext context, IProgressMonitor monitor) {
		IProject project = context.getProject();

		try {
			_configureIfLiferayProject(project);
		}
		catch (Exception e) {
			LiferayGradleCore.logError(e);
		}
	}

	@Override
	public void init(InitializationContext arg0, IProgressMonitor arg1) {
	}

	@Override
	public void unconfigure(ProjectContext arg0, IProgressMonitor arg1) {
	}

	private void _configureIfLiferayProject(final IProject project) throws CoreException {
		if (project.hasNature("org.eclipse.buildship.core.gradleprojectnature") && !LiferayNature.hasNature(project)) {
			final boolean[] needAddNature = new boolean[1];

			needAddNature[0] = false;

			IFile bndFile = project.getFile("bnd.bnd");

			// case 1: has bnd file

			if (FileUtil.exists(bndFile)) {
				needAddNature[0] = true;
			}
			else if (ProjectUtil.isWorkspaceWars(project)) {
				needAddNature[0] = true;
			}
			else {
				IFile gulpFile = project.getFile("gulpfile.js");

				if (FileUtil.exists(gulpFile)) {
					String gulpFileContent;

					File file = FileUtil.getFile(gulpFile);

					try {
						gulpFileContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

						// case 2: has gulpfile.js with some content

						if (gulpFileContent.contains("require('liferay-theme-tasks')")) {
							needAddNature[0] = true;
						}
					}
					catch (IOException ioe) {
						LiferayGradleCore.logError("read gulpfile.js file fail", ioe);
					}
				}
			}

			try {
				IProgressMonitor monitor = new NullProgressMonitor();

				if (needAddNature[0]) {
					LiferayNature.addLiferayNature(project, monitor);

					return;
				}

				final ProjectInfo projectInfo = LiferayGradleCore.getToolingModel(ProjectInfo.class, project);

				if (projectInfo == null) {
					throw new CoreException(
						LiferayGradleCore.createErrorStatus("Unable to get read gradle configuration"));
				}

				Set<String> pluginClassNames = projectInfo.getPluginClassNames();

				if (projectInfo.isLiferayProject() || pluginClassNames.contains("org.gradle.api.plugins.WarPlugin") ||
					pluginClassNames.contains("com.liferay.gradle.plugins.theme.builder.ThemeBuilderPlugin")) {

					LiferayNature.addLiferayNature(project, monitor);
				}
			}
			catch (Exception e) {
				LiferayGradleCore.logError("Unable to get tooling model", e);
			}
		}
	}

}