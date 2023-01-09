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

package com.liferay.ide.project.core.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.core.JavaFacet;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetInstallDataModelProperties;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetProjectCreationDataModelProperties.FacetDataModelMap;
import org.eclipse.wst.common.componentcore.internal.operation.IArtifactEditOperationDataModelProperties;
import org.eclipse.wst.common.componentcore.internal.util.IModuleConstants;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.eclipse.wst.common.frameworks.datamodel.DataModelPropertyDescriptor;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IPreset;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.internal.BridgedRuntime;
import org.osgi.framework.Constants;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.liferay.ide.core.ILiferayProject;
import com.liferay.ide.core.IWebProject;
import com.liferay.ide.core.IWorkspaceProject;
import com.liferay.ide.core.LiferayCore;
import com.liferay.ide.core.ProductInfo;
import com.liferay.ide.core.adapter.NoopLiferayProject;
import com.liferay.ide.core.util.CoreUtil;
import com.liferay.ide.core.util.FileUtil;
import com.liferay.ide.core.util.ListUtil;
import com.liferay.ide.core.util.PropertiesUtil;
import com.liferay.ide.core.util.StringPool;
import com.liferay.ide.core.util.StringUtil;
import com.liferay.ide.core.workspace.LiferayWorkspaceUtil;
import com.liferay.ide.project.core.ProjectCore;
import com.liferay.ide.project.core.modules.BndProperties;
import com.liferay.ide.project.core.modules.BndPropertiesValue;
import com.liferay.ide.server.util.ServerUtil;

/**
 * @author Gregory Amerson
 * @author Kuo Zhang
 * @author Terry Jia
 * @author Simon Jiang
 * @author Seiphon Wang
 */
@SuppressWarnings("restriction")
public class ProjectUtil {

	public static final String METADATA_FOLDER = ".metadata";

	public static void collectProjectsFromDirectory(List<IProject> result, File location) {
		File[] children = location.listFiles();

		if (children == null) {
			return;
		}

		for (File child : children) {
			if (child.isFile() && IProjectDescription.DESCRIPTION_FILE_NAME.equals(child.getName())) {
				IWorkspace workspace = CoreUtil.getWorkspace();
				IProjectDescription projectDescription;

				try {
					projectDescription = workspace.loadProjectDescription(new Path(child.getAbsolutePath()));

					IProject project = CoreUtil.getProject(projectDescription.getName());

					if (FileUtil.exists(project)) {
						result.add(project);
					}
				}
				catch (CoreException ce) {
					ProjectCore.logError("loadProjectDescription error", ce);
				}
			}
			else {
				collectProjectsFromDirectory(result, child);
			}
		}
	}

	public static void createDefaultWebXml(File webxmlFile, String expectedContainingProjectName) {
		StringBuilder sb = new StringBuilder();

		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<web-app id=\"WebApp_ID\" ");
		sb.append("version=\"2.5\" ");
		sb.append("xmlns=\"http://java.sun.com/xml/ns/javaee\" ");
		sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
		sb.append("xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee ");
		sb.append("http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\">\n");
		sb.append("</web-app>");

		try {
			FileUtil.writeFile(webxmlFile, sb.toString(), expectedContainingProjectName);
		}
		catch (Exception e) {
			ProjectCore.logError("Unable to create default web xml", e);
		}
	}
	
	public static boolean isWorkspaceWars(IProject project) {
		if (LiferayWorkspaceUtil.hasWorkspace() && FileUtil.exists(project.getFolder("src"))) {
			File wsRootDir = LiferayWorkspaceUtil.getWorkspaceProjectFile();

			IWorkspaceProject liferayWorkspaceProject = LiferayWorkspaceUtil.getLiferayWorkspaceProject();

			String[] warsNames = liferayWorkspaceProject.getWorkspaceWarDirs();

			if (Objects.isNull(warsNames)) {
				return false;
			}

			File[] warsDirs = new File[warsNames.length];

			for (int i = 0; i < warsNames.length; i++) {
				warsDirs[i] = new File(wsRootDir, warsNames[i]);
			}

			IPath location = project.getLocation();

			File projectDir = location.toFile();

			File parentDir = projectDir.getParentFile();

			if (parentDir == null) {
				return false;
			}

			while (true) {
				for (File dir : warsDirs) {
					if (parentDir.equals(dir)) {
						return true;
					}
				}

				parentDir = parentDir.getParentFile();

				if (parentDir == null) {
					return false;
				}
			}
		}

		return false;
	}

	public static IStatus validatePath(String currentPath) {
		if (!Path.EMPTY.isValidPath(currentPath)) {
			return ProjectCore.createErrorStatus("\"" + currentPath + "\" is not a valid path.");
		}

		IPath osPath = Path.fromOSString(currentPath);

		File file = osPath.toFile();

		if (!file.isAbsolute()) {
			return ProjectCore.createErrorStatus("\"" + currentPath + "\" is not an absolute path.");
		}

		if (FileUtil.notExists(osPath)) {
			return ProjectCore.createErrorStatus("Directory does not exist.");
		}

		return Status.OK_STATUS;
	}

	public static IFile createEmptyProjectFile(String fileName, IFolder folder) throws CoreException {
		IFile emptyFile = folder.getFile(fileName);

		if (FileUtil.exists(emptyFile)) {
			return emptyFile;
		}

		try (InputStream inputStream = new ByteArrayInputStream(StringPool.EMPTY.getBytes())) {
			emptyFile.create(inputStream, true, null);
		}
		catch (IOException ioe) {
			throw new CoreException(ProjectCore.createErrorStatus(ioe));
		}

		return emptyFile;
	}

	public static void fixExtProjectSrcFolderLinks(IProject extProject) throws JavaModelException {
		if (extProject == null) {
			return;
		}

		IJavaProject javaProject = JavaCore.create(extProject);

		if (javaProject == null) {
			return;
		}

		IVirtualComponent c = ComponentCore.createComponent(extProject, false);

		if (c == null) {
			return;
		}

		IVirtualFolder rootFolder = c.getRootFolder();

		IVirtualFolder jsrc = rootFolder.getFolder("/WEB-INF/classes");

		if (jsrc == null) {
			return;
		}

		IClasspathEntry[] cp = javaProject.getRawClasspath();

		for (IClasspathEntry cpe : cp) {
			if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath path = cpe.getPath();

				path = path.removeFirstSegments(1);

				if (path.segmentCount() > 0) {
					try {
						IWorkspaceRoot workspaceRoot = CoreUtil.getWorkspaceRoot();

						IFolder srcFolder = workspaceRoot.getFolder(cpe.getPath());

						IVirtualResource[] virtualResource = ComponentCore.createResources(srcFolder);

						// create link for source folder only when it is not mapped

						if (virtualResource.length == 0) {
							IPath p = cpe.getPath();

							jsrc.createLink(p.removeFirstSegments(1), 0, null);
						}
					}
					catch (Exception e) {
						ProjectCore.logError(e);
					}
				}
			}
		}
	}

	public static String getBundleSymbolicNameFromBND(IProject project) {
		String retVal = null;

		IFile bndFile = project.getFile("bnd.bnd");

		if (bndFile.exists()) {
			File file = FileUtil.getFile(bndFile);

			Properties prop = PropertiesUtil.loadProperties(file);

			retVal = prop.getProperty(Constants.BUNDLE_SYMBOLICNAME);
		}

		return retVal;
	}

	public static IFacetedProject getFacetedProject(IProject project) {
		try {
			return ProjectFacetsManager.create(project);
		}
		catch (CoreException ce) {
			return null;
		}
	}

	public static Set<IProjectFacetVersion> getFacetsForPreset(String presetId) {
		IPreset preset = ProjectFacetsManager.getPreset(presetId);

		return preset.getProjectFacets();
	}

	public static Map<String, String> getFragmentProjectInfo(IProject project) {
		IFile bndFile = project.getFile("bnd.bnd");

		if (FileUtil.notExists(bndFile)) {
			return null;
		}

		try {
			Map<String, String> fragmentProjectInfo = new HashMap<>();

			BndProperties bndProperty = new BndProperties();

			bndProperty.load(FileUtil.getFile(bndFile));

			BndPropertiesValue portalBundleVersion = (BndPropertiesValue)bndProperty.get("Portal-Bundle-Version");

			if (portalBundleVersion != null) {
				fragmentProjectInfo.put("Portal-Bundle-Version", portalBundleVersion.getOriginalValue());
			}
			else {
				fragmentProjectInfo.put("Portal-Bundle-Version", null);
			}

			BndPropertiesValue fragmentHostValue = (BndPropertiesValue)bndProperty.get("Fragment-Host");

			if (fragmentHostValue != null) {
				String fragmentHost = fragmentHostValue.getOriginalValue();

				String[] hostOSGiBundleArray = fragmentHost.split(";");

				if (ListUtil.isNotEmpty(hostOSGiBundleArray) && (hostOSGiBundleArray.length > 1)) {
					String[] f = hostOSGiBundleArray[1].split("=");

					String version = f[1].substring(1, f[1].length() - 1);

					fragmentProjectInfo.put("HostOSGiBundleName", hostOSGiBundleArray[0] + "-" + version);
				}

				return fragmentProjectInfo;
			}

			return null;
		}
		catch (IOException ioe) {
			ProjectCore.logError("Failed to parsed bnd.bnd for project " + project.getName(), ioe);
		}

		return null;
	}

	public static IProjectFacet getLiferayFacet(IFacetedProject facetedProject) {
		for (IProjectFacetVersion projectFacet : facetedProject.getProjectFacets()) {
			if (isLiferayFacet(projectFacet.getProjectFacet())) {
				return projectFacet.getProjectFacet();
			}
		}

		return null;
	}

	public static IFile getPortletXmlFile(IProject project) {
		if ((project == null) || !isLiferayFacetedProject(project)) {
			return null;
		}

		IWebProject webProject = LiferayCore.create(IWebProject.class, project);

		if (webProject == null) {
			return null;
		}

		IFolder defaultDocrootFolder = webProject.getDefaultDocrootFolder();

		if (defaultDocrootFolder == null) {
			return null;
		}

		IFile portletXml = defaultDocrootFolder.getFile(new Path("WEB-INF/portlet.xml"));

		if (FileUtil.exists(portletXml)) {
			return portletXml;
		}

		return null;
	}

	public static Map<String, ProductInfo> getProductInfos() {
		try (JsonReader jsonReader = new JsonReader(Files.newBufferedReader(_workspaceCacheFile.toPath()))) {
			Gson gson = new Gson();

			TypeToken<Map<String, ProductInfo>> typeToken = new TypeToken<Map<String, ProductInfo>>() {
			};

			return gson.fromJson(jsonReader, typeToken.getType());
		}
		catch (Exception ce) {
			ProjectCore.logError("Cannot Find Product Info", ce);
		}

		return null;
	}

	// IDE-270

	public static IProject getProject(IDataModel model) {
		if (model == null) {
			return null;
		}

		String projectName = model.getStringProperty(IArtifactEditOperationDataModelProperties.PROJECT_NAME);

		return CoreUtil.getProject(projectName);
	}

	public static IProject getProject(String projectName) {
		return CoreUtil.getProject(projectName);
	}

	public static String getRelativePathFromDocroot(IWebProject lrproject, String path) {
		IFolder docroot = lrproject.getDefaultDocrootFolder();

		IPath pathValue = new Path(path);

		IPath relativePath = pathValue.makeRelativeTo(docroot.getFullPath());

		String retval = relativePath.toPortableString();

		if (retval.startsWith("/")) {
			return retval;
		}

		return "/" + retval;
	}

	public static boolean hasFacet(IProject project, IProjectFacet checkProjectFacet) {
		boolean retval = false;

		if ((project == null) || (checkProjectFacet == null)) {
			return retval;
		}

		try {
			IFacetedProject facetedProject = ProjectFacetsManager.create(project);

			if ((facetedProject != null) && (checkProjectFacet != null)) {
				for (IProjectFacetVersion facet : facetedProject.getProjectFacets()) {
					if (checkProjectFacet.equals(facet.getProjectFacet())) {
						retval = true;

						break;
					}
				}
			}
		}
		catch (CoreException ce) {
		}

		return retval;
	}

	public static boolean hasFacet(IProject project, String facetId) {
		return hasFacet(project, ProjectFacetsManager.getProjectFacet(facetId));
	}

	public static boolean hasProperty(IDataModel model, String propertyName) {
		boolean retval = false;

		if ((model == null) || CoreUtil.isNullOrEmpty(propertyName)) {
			return retval;
		}

		for (Object property : model.getAllProperties()) {
			if (propertyName.equals(property)) {
				retval = true;

				break;
			}
		}

		return retval;
	}

	public static boolean isDynamicWebFacet(IProjectFacet facet) {
		if ((facet != null) && IModuleConstants.JST_WEB_MODULE.equals(facet.getId())) {
			return true;
		}

		return false;
	}

	public static boolean isDynamicWebFacet(IProjectFacetVersion facetVersion) {
		if ((facetVersion != null) && isDynamicWebFacet(facetVersion.getProjectFacet())) {
			return true;
		}

		return false;
	}

	public static boolean isFacetedGradleBundleProject(IProject project) {
		if (_checkGradleThemePlugin(project) || _checkGradleWarPlugin(project)) {
			return true;
		}

		return false;
	}

	public static boolean isFragmentProject(Object resource) throws Exception {
		IProject project = null;

		if (resource instanceof IFile) {
			IFile file = (IFile)resource;

			project = file.getProject();
		}
		else if (resource instanceof IProject) {
			project = (IProject)resource;
		}

		IFile bndfile = project.getFile("bnd.bnd");

		if (bndfile.exists()) {
			try (InputStream inputStream = bndfile.getContents();
				InputStreamReader inputReader = new InputStreamReader(inputStream);
				BufferedReader reader = new BufferedReader(inputReader)) {

				String fragName;

				while ((fragName = reader.readLine()) != null) {
					if (fragName.contains("Fragment-Host:")) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static boolean isGradleProject(IProject project) {
		boolean retval = false;

		try {
			retval = FileUtil.exists(project) && project.hasNature("org.eclipse.buildship.core.gradleprojectnature");
		}
		catch (Exception e) {
		}

		return retval;
	}

	public static boolean isJavaFacet(IProjectFacet facet) {
		if (facet == null) {
			return false;
		}

		if (JavaFacet.ID.equals(facet.getId()) || IModuleConstants.JST_JAVA.equals(facet.getId())) {
			return true;
		}

		return false;
	}

	public static boolean isJavaFacet(IProjectFacetVersion facetVersion) {
		if (facetVersion == null) {
			return false;
		}

		if (isJavaFacet(facetVersion.getProjectFacet())) {
			return true;
		}

		return false;
	}

	public static boolean isLiferayFacet(IProjectFacet projectFacet) {
		if ((projectFacet != null) && StringUtil.startsWith(projectFacet.getId(), "liferay.")) {
			return true;
		}

		return false;
	}

	public static boolean isLiferayFacet(IProjectFacetVersion projectFacetVersion) {
		if ((projectFacetVersion != null) && isLiferayFacet(projectFacetVersion.getProjectFacet())) {
			return true;
		}

		return false;
	}

	public static boolean isLiferayFacetedProject(IProject project) {
		boolean retval = false;

		if (project == null) {
			return retval;
		}

		try {
			IFacetedProject facetedProject = ProjectFacetsManager.create(project);

			if (facetedProject != null) {
				for (IProjectFacetVersion facet : facetedProject.getProjectFacets()) {
					if (isLiferayFacet(facet.getProjectFacet())) {
						retval = true;

						break;
					}
				}
			}
		}
		catch (Exception e) {
		}

		return retval;
	}

	public static boolean isMavenProject(IProject project) {
		if (project == null) {
			return false;
		}

		boolean retval = false;

		try {
			retval =
				project.hasNature("org.eclipse.m2e.core.maven2Nature") && FileUtil.exists(project.getFile("pom.xml"));
		}
		catch (Exception e) {
		}

		return retval;
	}

	public static boolean isModuleExtProject(Object resource) {
		IProject project = null;

		if (resource instanceof IFile) {
			IFile file = (IFile)resource;

			project = file.getProject();
		}
		else if (resource instanceof IProject) {
			project = (IProject)resource;
		}

		if (FileUtil.notExists(project)) {
			return false;
		}

		IFile gradleFile = project.getFile("build.gradle");

		if (FileUtil.notExists(gradleFile)) {
			return false;
		}

		String contents = FileUtil.readContents(gradleFile);

		if (!contents.contains("originalModule")) {
			return false;
		}

		return true;
	}

	public static boolean isParent(IFolder folder, IResource resource) {
		if ((folder == null) || (resource == null)) {
			return false;
		}

		if ((resource.getParent() != null) && folder.equals(resource.getParent())) {
			return true;
		}

		boolean retval = isParent(folder, resource.getParent());

		if (retval) {
			return true;
		}

		return false;
	}

	public static boolean isServiceBuilderProject(IProject project) {
		if (project == null) {
			return false;
		}

		if (!project.isOpen()) {
			return false;
		}

		try {
			if (project.hasNature("org.eclipse.jdt.core.javanature") &&
				!LiferayWorkspaceUtil.isValidWorkspace(project) &&
				Optional.ofNullable(
					project).filter(
						p -> LiferayCore.create(IWorkspaceProject.class, project) == null).map(
							p -> LiferayCore.create(ILiferayProject.class, p)).filter(
								Objects::nonNull).filter(
									p -> !(p instanceof NoopLiferayProject)).isPresent()) {

				SearchFilesVisitor searchFilesVisitor = new SearchFilesVisitor();

				List<IFile> serviceXmls = searchFilesVisitor.searchFiles(project, "service.xml");

				return !serviceXmls.isEmpty();
			}

			return false;
		}
		catch (CoreException ce) {
			return false;
		}
	}

	public static void refreshLocalProject(IProject project) {
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		}
		catch (Exception exception) {
		}
	}

	public static void setDefaultRuntime(IDataModel dataModel) {
		DataModelPropertyDescriptor[] validDescriptors = dataModel.getValidPropertyDescriptors(
			IFacetProjectCreationDataModelProperties.FACET_RUNTIME);

		for (DataModelPropertyDescriptor desc : validDescriptors) {
			Object runtime = desc.getPropertyValue();

			if ((runtime instanceof BridgedRuntime) && ServerUtil.isLiferayRuntime((BridgedRuntime)runtime)) {
				dataModel.setProperty(IFacetProjectCreationDataModelProperties.FACET_RUNTIME, runtime);

				break;
			}
		}
	}

	public static void setGenerateDD(IDataModel model, boolean generateDD) {
		IDataModel ddModel = null;

		if (hasProperty(model, IJ2EEFacetInstallDataModelProperties.GENERATE_DD)) {
			ddModel = model;
		}
		else if (hasProperty(model, IFacetProjectCreationDataModelProperties.FACET_DM_MAP)) {
			FacetDataModelMap map = (FacetDataModelMap)model.getProperty(
				IFacetProjectCreationDataModelProperties.FACET_DM_MAP);

			ddModel = map.getFacetDataModel(IJ2EEFacetConstants.DYNAMIC_WEB_FACET.getId());
		}

		if (ddModel != null) {
			ddModel.setBooleanProperty(IJ2EEFacetInstallDataModelProperties.GENERATE_DD, generateDD);
		}
	}

	private static boolean _checkGradleThemePlugin(IProject project) {
		IFile buildGradleFile = project.getFile("build.gradle");

		if (!buildGradleFile.exists()) {
			return false;
		}

		try (InputStream ins = buildGradleFile.getContents()) {
			String content = FileUtil.readContents(ins);

			Matcher matcher = _themeBuilderPlugin.matcher(content);

			if ((content != null) && matcher.matches()) {
				return true;
			}

			return false;
		}
		catch (Exception e) {
			return false;
		}
	}

	private static boolean _checkGradleWarPlugin(IProject project) {
		IFile buildGradleFile = project.getFile("build.gradle");

		if (!buildGradleFile.exists()) {
			return false;
		}

		try (InputStream ins = buildGradleFile.getContents()) {
			String content = FileUtil.readContents(ins);

			Matcher matcher = _warPlugin.matcher(content);

			if ((content != null) && matcher.matches()) {
				return true;
			}

			return false;
		}
		catch (Exception e) {
			return false;
		}
	}

	private static final String _DEFAULT_WORKSPACE_CACHE_FILE = ".liferay/workspace/.product_info.json";

	private static final Pattern _themeBuilderPlugin = Pattern.compile(
		".*apply.*plugin.*:.*[\'\"]com\\.liferay\\.portal\\.tools\\.theme\\.builder[\'\"].*",
		Pattern.MULTILINE | Pattern.DOTALL);
	private static final Pattern _warPlugin = Pattern.compile(".*apply.*war.*", Pattern.MULTILINE | Pattern.DOTALL);
	private static final File _workspaceCacheFile = new File(
		System.getProperty("user.home"), _DEFAULT_WORKSPACE_CACHE_FILE);

	private static class Msgs extends NLS {

		public static String checking;

		static {
			initializeMessages(ProjectUtil.class.getName(), Msgs.class);
		}

	}

}