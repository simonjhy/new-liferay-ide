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

package com.liferay.ide.project.ui.modules.fragment;

import com.liferay.ide.core.IBundleProject;
import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.workspace.LiferayWorkspaceUtil;
import com.liferay.ide.project.core.modules.fragment.NewModuleFragmentFilesOp;
import com.liferay.ide.server.core.portal.docker.PortalDockerRuntime;

import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.sapphire.platform.PathBridge;
import org.eclipse.sapphire.ui.def.DefinitionLoader;
import org.eclipse.sapphire.ui.forms.swt.SapphireWizard;
import org.eclipse.sapphire.ui.forms.swt.SapphireWizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.wst.server.core.ServerCore;

/**
 * @author Terry Jia
 * @author Simon Jiang
 */
public class NewModuleFragmentFilesWizard
	extends SapphireWizard<NewModuleFragmentFilesOp> implements INewWizard, IWorkbenchWizard {

	public NewModuleFragmentFilesWizard() {
		super(_createDefaultOp(), DefinitionLoader.sdef(NewModuleFragmentFilesWizard.class).wizard());
	}

	@Override
	public IWizardPage[] getPages() {
		final IWizardPage[] wizardPages = super.getPages();

		if (wizardPages != null) {
			final SapphireWizardPage wizardPage = (SapphireWizardPage)wizardPages[0];

			IWorkspaceProject liferayWorkspaceProject = LiferayWorkspaceUtil.getGradleWorkspaceProject();

			if (Objects.nonNull(liferayWorkspaceProject)) {
				boolean hasPortalDockerRuntime = Stream.of(
					ServerCore.getRuntimes()
				).filter(
					runtime -> Objects.nonNull(runtime)
				).filter(
					runtime -> Objects.nonNull(
						(PortalDockerRuntime)runtime.loadAdapter(PortalDockerRuntime.class, new NullProgressMonitor()))
				).findAny(
				).isPresent();

				if (hasPortalDockerRuntime) {
					wizardPage.setMessage(
						"Docker Server can not be used for new Fragment Project Wizard", SapphireWizardPage.WARNING);
				}
			}
		}

		return wizardPages;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if ((selection != null) && !selection.isEmpty()) {
			final Object element = selection.getFirstElement();

			if (element instanceof IResource) {
				IResource resourceElement = (IResource)element;

				_initialProject = resourceElement.getProject();
			}
			else if (element instanceof IJavaProject) {
				IJavaProject javaProjectElement = (IJavaProject)element;

				_initialProject = javaProjectElement.getProject();
			}
			else if (element instanceof IPackageFragment) {
				IPackageFragment packageFragmentElement = (IPackageFragment)element;

				IResource resource = packageFragmentElement.getResource();

				_initialProject = resource.getProject();
			}
			else if (element instanceof IJavaElement) {
				IJavaElement javaElement = (IJavaElement)element;

				IResource resource = javaElement.getResource();

				_initialProject = resource.getProject();
			}

			if (_initialProject != null) {
				final IBundleProject bundleProject = LiferayCore.create(IBundleProject.class, _initialProject);

				if ((bundleProject != null) && bundleProject.isFragmentBundle()) {
					IPath projectLocation = _initialProject.getLocation();

					element().setLocation(PathBridge.create(projectLocation.removeLastSegments(1)));

					element().setProjectName(_initialProject.getName());
				}
			}
		}
	}

	private static NewModuleFragmentFilesOp _createDefaultOp() {
		_op = NewModuleFragmentFilesOp.TYPE.instantiate();

		return _op;
	}

	private static NewModuleFragmentFilesOp _op;

	private IProject _initialProject;

}