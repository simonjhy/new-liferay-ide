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

import com.liferay.ide.core.ILiferayProjectProvider;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.workspace.LiferayWorkspaceUtil;
import com.liferay.ide.project.core.ProjectCore;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.sapphire.DefaultValueService;

/**
 * @author Terry Jia
 * @author Seiphon Wang
 */
public class FragmentProjectProviderDefaultValueService extends DefaultValueService {

	@Override
	protected String compute() {
		String retval = "gradle-module-fragment";

		try {
			if (LiferayWorkspaceUtil.hasGradleWorkspace()) {
				return retval;
			}

			if (LiferayWorkspaceUtil.hasMavenWorkspace()) {
				return "maven-module-fragment";
			}
		}
		catch (Exception e) {
		}

		IScopeContext[] prefContexts = {DefaultScope.INSTANCE, InstanceScope.INSTANCE};

		IPreferencesService preferencesService = Platform.getPreferencesService();

		String defaultProjectBuildType = preferencesService.getString(
			ProjectCore.PLUGIN_ID, ProjectCore.PREF_DEFAULT_MODULE_FRAGMENT_PROJECT_BUILD_TYPE_OPTION, null,
			prefContexts);

		if (defaultProjectBuildType != null) {
			ILiferayProjectProvider provider = LiferayCore.getProvider(defaultProjectBuildType);

			if (provider != null) {
				retval = defaultProjectBuildType;
			}
		}

		return retval;
	}

}