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

package com.liferay.ide.gradle.action;

import com.liferay.ide.core.workspace.LiferayWorkspaceUtil;
import com.liferay.ide.server.util.ServerUtil;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

/**
 * @author Terry Jia
 * @author Charles Wu
 * @author Simon Jiang
 */
public class InitBundleTaskAction extends GradleTaskAction {

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);

		action.setEnabled(LiferayWorkspaceUtil.isValidWorkspace(project));
	}

	protected void afterAction() {
		ServerUtil.addPortalRuntime();
	}

	protected void beforeAction() {
		ServerUtil.deleteWorkspaceServerAndRuntime(project);
	}

	@Override
	protected String getGradleTaskName() {
		return "initBundle";
	}

}