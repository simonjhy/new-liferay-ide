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

package com.liferay.ide.project.ui.action;

import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.ui.LiferayPerspectiveFactory;
import com.liferay.ide.ui.LiferayWorkspacePerspectiveFactory;
import com.liferay.ide.ui.util.UIUtil;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.PlatformUI;

/**
 * @author Gregory Amerson
 * @author Kuo Zhang
 * @author Simon Jiang
 */
public class NewProjectDropDownAction extends Action implements IMenuCreator, IWorkbenchWindowPulldownDelegate2 {

	public static Action getDefaultAction() {
		return getWizardAction(DEFAULT_WIZARD_ID);
	}

	public static NewWizardAction[] getNewProjectActions() {
		ArrayList<NewWizardAction> containers = new ArrayList<>();

		IExtensionPoint extensionPoint = CoreUtil.getExtensionPoint(PlatformUI.PLUGIN_ID, PL_NEW);

		if (extensionPoint != null) {
			IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

			for (IConfigurationElement element : elements) {
				if (TAG_WIZARD.equals(element.getName()) && _isProjectWizard(element, _getAttribute())) {
					containers.add(new NewWizardAction(element));
				}
			}
		}

		NewWizardAction[] actions = (NewWizardAction[])containers.toArray(new NewWizardAction[0]);

		Arrays.sort(actions);

		return actions;
	}

	public static Action getPluginProjectAction() {
		return getWizardAction(DEFAULT_PLUGIN_WIZARD_ID);
	}

	public static Action getWizardAction(final String wizardId) {
		Action[] actions = getNewProjectActions();

		if (ListUtil.isNotEmpty(actions)) {
			for (Action action : actions) {
				if ((action instanceof NewWizardAction) && wizardId.equals(action.getId())) {
					return action;
				}
			}
		}

		return null;
	}

	public NewProjectDropDownAction() {
		fMenu = null;
		setMenuCreator(this);
	}

	public void dispose() {
	}

	public NewWizardAction[] getActionFromDescriptors(String typeAttribute) {
		ArrayList<NewWizardAction> containers = new ArrayList<>();

		IExtensionPoint extensionPoint = CoreUtil.getExtensionPoint(PlatformUI.PLUGIN_ID, PL_NEW);

		if (extensionPoint != null) {
			IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

			for (IConfigurationElement element : elements) {
				if (TAG_WIZARD.equals(element.getName()) && _isLiferayArtifactWizard(element, typeAttribute)) {
					containers.add(new NewWizardAction(element));
				}
			}
		}

		NewWizardAction[] actions = (NewWizardAction[])containers.toArray(new NewWizardAction[0]);

		Arrays.sort(actions);

		return actions;
	}

	public NewWizardAction[] getExtraProjectActions() {
		ArrayList<NewWizardAction> containers = new ArrayList<>();

		IExtensionPoint extensionPoint = CoreUtil.getExtensionPoint(PlatformUI.PLUGIN_ID, PL_NEW);

		if (extensionPoint != null) {
			IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

			for (IConfigurationElement element : elements) {
				if (TAG_WIZARD.equals(element.getName()) && _isProjectWizard(element, _getExtraTypeAttribute())) {
					containers.add(new NewWizardAction(element));
				}
			}
		}

		NewWizardAction[] actions = (NewWizardAction[])containers.toArray(new NewWizardAction[0]);

		Arrays.sort(actions);

		return actions;
	}

	public Menu getMenu(Control parent) {
		fMenu = new Menu(parent);

		switch (_getPerspectiveID()) {
			case LiferayWorkspacePerspectiveFactory.ID:

				// add non project items

				NewWizardAction[] nonProjectActions = getActionFromDescriptors(getNonprojectTypeAttribute());

				for (NewWizardAction action : nonProjectActions) {
					action.setShell(fWizardShell);

					ActionContributionItem nonProjectItem = new ActionContributionItem(action);

					nonProjectItem.fill(fMenu, -1);
				}

				new Separator().fill(fMenu, -1);

				NewWizardAction[] actions = getNewProjectActions();

				// only do the first project action (not the 5 separate ones)

				for (NewWizardAction action : actions) {
					action.setShell(fWizardShell);

					ActionContributionItem projectItem = new ActionContributionItem(action);

					projectItem.fill(fMenu, -1);
				}

				break;

		}

		new Separator().fill(fMenu, -1);

		NewWizardAction[] kaleoActions = getActionFromDescriptors(getKaleoTypeAttribute());

		for (NewWizardAction action : kaleoActions) {
			action.setShell(fWizardShell);

			ActionContributionItem kaleoItem = new ActionContributionItem(action);

			kaleoItem.fill(fMenu, -1);
		}

		Action[] sdkActions = getServerActions(parent.getShell());

		for (Action action : sdkActions) {
			ActionContributionItem sdkItem = new ActionContributionItem(action);

			sdkItem.fill(fMenu, -1);
		}

		return fMenu;
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	public void init(IWorkbenchWindow window) {
		fWizardShell = window.getShell();
	}

	public void run(IAction action) {
		if (LiferayPerspectiveFactory.ID.equals(_getPerspectiveID())) {
			getPluginProjectAction().run();
		}
		else {
			getDefaultAction().run();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	protected String getKaleoTypeAttribute() {
		return "liferay_kaleo_workflow";
	}

	protected String getNonprojectExtraTypeAttribute() {
		return "liferay_extra_artifact";
	}

	protected String getNonprojectTypeAttribute() {
		return "liferay_artifact";
	}

	protected String getPluginNonprojectTypeAttribute() {
		return "liferay_plugin_artifact";
	}

	protected Action[] getServerActions(Shell shell) {
		return new Action[] {new NewServerAction(shell)};
	}

	protected static final String DEFAULT_PLUGIN_WIZARD_ID = "com.liferay.ide.project.ui.newPluginProjectWizard";

	protected static final String DEFAULT_WIZARD_ID = "com.liferay.ide.project.ui.newModuleProjectWizard";

	protected static final String PL_NEW = "newWizards";

	protected static final String TAG_CLASS = "class";

	protected static final String TAG_NAME = "name";

	protected static final String TAG_PARAMETER = "parameter";

	protected static final String TAG_VALUE = "value";

	protected static final String TAG_WIZARD = "wizard";

	protected Menu fMenu;
	protected Shell fWizardShell;

	private static String _getAttribute() {
		if (LiferayPerspectiveFactory.ID.equals(_getPerspectiveID())) {
			return _getPluginProjectTypeAttribute();
		}

		return _getTypeAttribute();
	}

	private static String _getExtraTypeAttribute() {
		return "liferay_extra_project";
	}

	private static String _getPerspectiveID() {
		IWorkbenchPage activePage = UIUtil.getActivePage();

		IPerspectiveDescriptor perspective = activePage.getPerspective();

		return perspective.getId();
	}

	private static String _getPluginProjectTypeAttribute() {
		return "liferay_plugin_project";
	}

	private static String _getTypeAttribute() {
		return "liferay_project";
	}

	private static boolean _isProjectWizard(IConfigurationElement element, String typeAttribute) {
		IConfigurationElement[] classElements = element.getChildren(TAG_CLASS);

		if (!CoreUtil.isNullOrEmpty(typeAttribute) && ListUtil.isNotEmpty(classElements)) {
			for (IConfigurationElement classElement : classElements) {
				IConfigurationElement[] paramElements = classElement.getChildren(TAG_PARAMETER);

				for (IConfigurationElement paramElement : paramElements) {
					if (typeAttribute.equals(paramElement.getAttribute(TAG_NAME))) {
						return Boolean.valueOf(paramElement.getAttribute(TAG_VALUE));
					}
				}
			}
		}

		// old way, deprecated

		return Boolean.valueOf(element.getAttribute(_getTypeAttribute()));
	}

	private boolean _isLiferayArtifactWizard(IConfigurationElement element, String typeAttribute) {
		IConfigurationElement[] classElements = element.getChildren(TAG_CLASS);

		if (ListUtil.isNotEmpty(classElements)) {
			for (IConfigurationElement classElement : classElements) {
				IConfigurationElement[] paramElements = classElement.getChildren(TAG_PARAMETER);

				for (IConfigurationElement paramElement : paramElements) {
					String tagName = paramElement.getAttribute(TAG_NAME);

					if (typeAttribute.equals(tagName)) {
						return Boolean.valueOf(paramElement.getAttribute(TAG_VALUE));
					}
				}
			}
		}

		// old way, deprecated

		return Boolean.valueOf(element.getAttribute(_getTypeAttribute()));
	}

}