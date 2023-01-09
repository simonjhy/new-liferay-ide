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

import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.SapphireContentAccessor;
import com.liferay.ide.project.core.NewLiferayProjectProvider;

import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.sapphire.FilteredListener;
import org.eclipse.sapphire.PropertyContentEvent;
import org.eclipse.sapphire.modeling.Status;
import org.eclipse.sapphire.services.ValidationService;

/**
 * @author Charles Wu
 */
public class DumbStateValidationService extends ValidationService implements SapphireContentAccessor {

	@Override
	public void dispose() {
		super.dispose();

		_op().detach(_listener, "*");
	}

	@Override
	protected Status compute() {
		BaseModuleOp op = _op();

		NewLiferayProjectProvider<BaseModuleOp> provider = get(op.getProjectProvider());

		if (provider == null) {
			return Status.createOkStatus();
		}

		String providerShortName = provider.getShortName();

		if (providerShortName.contains("gradle")) {
			IJobManager jobManager = Job.getJobManager();

			Job[] jobs = jobManager.find("org.eclipse.buildship.core.jobs");

			for (Job job : jobs) {
				if (job.getRule() == CoreUtil.getWorkspaceRoot()) {
					return Status.createWarningStatus(
						"Project won't be resolved completely until all Gradle background jobs finish.");
				}
			}
		}

		return Status.createOkStatus();
	}

	@Override
	protected void initValidationService() {
		super.initValidationService();

		_listener = new FilteredListener<PropertyContentEvent>() {

			@Override
			protected void handleTypedEvent(PropertyContentEvent event) {
				refresh();
			}

		};

		_op().attach(_listener, "*");
	}

	private BaseModuleOp _op() {
		return context(BaseModuleOp.class);
	}

	private FilteredListener<PropertyContentEvent> _listener;

}