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

package com.liferay.ide.project.ui.modules.ext;

import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.JobUtil;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.modules.ext.NewModuleExtOp;
import com.liferay.ide.project.ui.BaseProjectWizard;
import com.liferay.ide.project.ui.ProjectUI;
import com.liferay.ide.project.ui.RequireLiferayWorkspaceProject;
import com.liferay.ide.ui.util.UIUtil;

import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.sapphire.ui.def.DefinitionLoader;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * @author Charles Wu
 * @author Seiphon Wang
 */
@SuppressWarnings("restriction")
public class NewModuleExtWizard extends BaseProjectWizard<NewModuleExtOp> implements RequireLiferayWorkspaceProject {

	public NewModuleExtWizard() {
		super(_createDefaultOp(), DefinitionLoader.sdef(NewModuleExtWizard.class).wizard());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);

		promptIfLiferayWorkspaceNotExists("Liferay Module Ext Project");
	}

	@Override
	protected void performPostFinish() {
		super.performPostFinish();

		final NewModuleExtOp newModuleExtOp = element().nearest(NewModuleExtOp.class);

		final IProject project = CoreUtil.getProject(get(newModuleExtOp.getProjectName()));

		try {
			addToWorkingSets(project);
		}
		catch (Exception ex) {
			ProjectUI.logError("Unable to add project to working set", ex);
		}

		openLiferayPerspective(project);

		if (get(newModuleExtOp.getCreateModuleExtFiles())) {
			_openNewModuleExtFilesWizard(project);
		}
	}

	private static NewModuleExtOp _createDefaultOp() {
		return NewModuleExtOp.TYPE.instantiate();
	}

	private void _openNewModuleExtFilesWizard(IProject project) {
		IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();

		IExtension extension = extensionRegistry.getExtension("com.liferay.ide.project.ui.newWizards");

		IConfigurationElement[] configurationElements = extension.getConfigurationElements();

		for (IConfigurationElement configurationElement : configurationElements) {
			if (Objects.equals("wizard", configurationElement.getName()) &&
				Objects.equals(
					configurationElement.getAttribute("id"), "com.liferay.ide.project.ui.newModuleExtFilesWizard")) {

				UIUtil.async(
					() -> {
						try {
							JobUtil.waitForLiferayProjectJob();

							INewWizard newWizard = (INewWizard)CoreUtility.createExtension(
								configurationElement, "class");

							IWorkbenchWindow activeWorkbenchWindow = UIUtil.getActiveWorkbenchWindow();

							Shell shell = activeWorkbenchWindow.getShell();

							newWizard.init(PlatformUI.getWorkbench(), new StructuredSelection(project));

							WizardDialog wizardDialog = new WizardDialog(shell, newWizard);

							wizardDialog.create();

							wizardDialog.open();
						}
						catch (CoreException ce) {
							ProjectCore.createErrorStatus(ce);
						}
					});

				break;
			}
		}
	}

}