/*
 * Copyright (c) 2016 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.liferay.ide.core.workspace;

import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.ListenerRegistry;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * An {@link IResourceChangeListener} implementation which sends events about project change events
 * via {@link CorePlugin#listenerRegistry()}.
 *
 * @author Donat Csikos
 */
public final class ProjectChangeListener implements IResourceChangeListener {

	public static ProjectChangeListener createAndRegister() {
		ProjectChangeListener listener = new ProjectChangeListener();

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);

		return listener;
	}

	public void close() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		workspace.removeResourceChangeListener(this);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();

		if (delta != null) {
			try {
				_visitDelta(delta);
			}
			catch (CoreException ce) {
				LiferayCore.logError("Failed to detect project changes", ce);
			}
		}
	}

	private ProjectChangeListener() {
	}

	private void _collectAffectedFilePaths(Set<IPath> paths, IProject project, IResourceDelta[] resourceDeltas) {
		for (IResourceDelta resourceDelta : resourceDeltas) {
			IResource resource = resourceDelta.getResource();

			if ((resource instanceof IFile) && project.contains(resource)) {
				paths.add(resource.getFullPath());
			}

			_collectAffectedFilePaths(paths, project, resourceDelta.getAffectedChildren());
		}
	}

	private Set<IPath> _collectProjectAffectedFiles(IProject project, IResourceDelta[] resourceDeltas) {
		Set<IPath> result = new HashSet<>();

		_collectAffectedFilePaths(result, project, resourceDeltas);

		return result;
	}

	private boolean _doVisitDelta(IResourceDelta delta) throws Exception {
		IResource resource = delta.getResource();

		if (resource instanceof IProject) {
			IProject project = (IProject)resource;

			IPath fromPath = delta.getMovedFromPath();

			IPath toPath = delta.getMovedToPath();

			ListenerRegistry listenerRegistry = LiferayCore.listenerRegistry();

			if (delta.getKind() == IResourceDelta.REMOVED) {
				if ((fromPath == null) && (toPath == null)) {
					listenerRegistry.dispatch(new ProjectDeletedEvent(project));
				}
			}
			else if (delta.getKind() == IResourceDelta.ADDED) {
				if ((fromPath == null) && (toPath == null)) {
					listenerRegistry.dispatch(new ProjectCreatedEvent(project));
				}
				else if (fromPath != null) {
					listenerRegistry.dispatch(new ProjectMovedEvent(project, fromPath.lastSegment()));
				}
			}
			else if (delta.getKind() == IResourceDelta.CHANGED) {
				Set<IPath> affectedFiles = _collectProjectAffectedFiles(project, delta.getAffectedChildren());

				listenerRegistry.dispatch(new ProjectChangedEvent(project, affectedFiles));
			}
			else if (delta.getFlags() == IResourceDelta.OPEN) {
				if (project.isOpen()) {
					listenerRegistry.dispatch(new ProjectOpenedEvent(project));
				}
				else {
					listenerRegistry.dispatch(new ProjectClosedEvent(project));
				}
			}

			return false;
		}

		return resource instanceof IWorkspaceRoot; // don't traverse deeper than the project level
	}

	private void _visitDelta(IResourceDelta delta) throws CoreException {
		delta.accept(
			new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					try {
						return _doVisitDelta(delta);
					}
					catch (Exception e) {
						throw new CoreException(
							new Status(IStatus.WARNING, LiferayCore.PLUGIN_ID, "ProjectChangeListener failed", e));
					}
				}

			});
	}

}