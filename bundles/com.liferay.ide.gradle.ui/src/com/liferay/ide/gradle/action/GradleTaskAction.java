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

import com.liferay.ide.core.ILiferayProjectProvider;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.gradle.core.GradleUtil;
import com.liferay.ide.gradle.ui.LiferayGradleUI;
import com.liferay.ide.ui.action.AbstractObjectAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;

/**
 * @author Lovett Li
 * @author Terry Jia
 * @author Charles Wu
 * @author Simon Jiang
 * @author Seiphon Wang
 */
public abstract class GradleTaskAction extends AbstractObjectAction {

	public GradleTaskAction() {
	}

	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			final List<String> gradleTasks = getGradleTasks();

			if (ListUtil.isEmpty(gradleTasks)) {
				return;
			}

			Job job = new Job(project.getName() + " - " + getGradleTaskName()) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						monitor.beginTask(getGradleTaskName(), 100);

						monitor.worked(20);

						GradleUtil.runGradleTask(
							project, gradleTasks.toArray(new String[0]), getGradleTaskArguments(), false, monitor);

						monitor.done();
					}
					catch (Exception e) {
						return LiferayGradleUI.createErrorStatus("Error running Gradle goal " + getGradleTaskName(), e);
					}

					return Status.OK_STATUS;
				}

			};

			job.addJobChangeListener(
				new JobChangeAdapter() {

					@Override
					public void aboutToRun(IJobChangeEvent event) {
						beforeAction();
					}

					@Override
					public void done(IJobChangeEvent event) {
						try {
							project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

							afterAction();
						}
						catch (CoreException ce) {
							LiferayGradleUI.logError(ce);
						}
					}

				});

			job.setProperty(ILiferayProjectProvider.LIFERAY_PROJECT_JOB, new Object());

			job.schedule();
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);

		if (Objects.isNull(selection)) {
			return;
		}

		if (fSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection)fSelection;

			Object[] elems = structuredSelection.toArray();

			if (ListUtil.isNotEmpty(elems)) {
				Object elem = elems[0];

				if (elem instanceof IFile) {
					gradleBuildFile = (IFile)elem;

					project = gradleBuildFile.getProject();
				}
				else if (elem instanceof IProject) {
					project = (IProject)elem;

					gradleBuildFile = project.getFile("build.gradle");
				}
			}
		}
	}

	protected String[] getGradleTaskArguments() {
		return new String[0];
	}

	protected abstract String getGradleTaskName();

	protected List<String> getGradleTasks() {
		GradleProject gradleProject = GradleUtil.getGradleProject(project);

		if (gradleProject == null) {
			return Collections.emptyList();
		}

		List<GradleTask> gradleTasks = new ArrayList<>();

		_fetchModelTasks(gradleProject, getGradleTaskName(), gradleTasks);

		Stream<GradleTask> gradleTaskStream = gradleTasks.stream();

		return gradleTaskStream.map(
			task -> task.getPath()
		).collect(
			Collectors.toList()
		);
	}

	protected boolean verifyTask(GradleTask gradleTask) {
		return true;
	}

	protected IFile gradleBuildFile = null;
	protected IProject project = null;

	private void _fetchModelTasks(GradleProject gradleProject, String taskName, List<GradleTask> tasks) {
		if (gradleProject == null) {
			return;
		}

		DomainObjectSet<? extends GradleTask> gradleTasks = gradleProject.getTasks();

		boolean parentHasTask = false;

		for (GradleTask gradleTask : gradleTasks) {
			if (Objects.equals(gradleTask.getName(), taskName) && verifyTask(gradleTask)) {
				tasks.add(gradleTask);

				parentHasTask = true;

				break;
			}
		}

		if (parentHasTask) {
			return;
		}

		DomainObjectSet<? extends GradleProject> childGradleProjects = gradleProject.getChildren();

		for (GradleProject childGradleProject : childGradleProjects) {
			_fetchModelTasks(childGradleProject, taskName, tasks);
		}
	}

}