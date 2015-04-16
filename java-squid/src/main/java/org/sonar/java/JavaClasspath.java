/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java;

import com.google.common.collect.Lists;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

public class JavaClasspath extends AbstractJavaClasspath {


  @Nullable
  private final MavenProject pom;
  private static final Logger LOG = LoggerFactory.getLogger(JavaClasspath.class);

  public JavaClasspath(Project project, Settings settings, FileSystem fs) {
    this(project, settings, fs, null);
  }

  public JavaClasspath(Project project, Settings settings, FileSystem fs, @Nullable MavenProject pom) {
    super(project, settings, fs, InputFile.Type.MAIN);
    this.pom = pom;
  }

  @Override
  protected void init() {
    if (!initialized) {
      initialized = true;
      validateLibraries = project.getModules().isEmpty();
      binaries = getFilesFromProperty(JavaClasspathProperties.SONAR_JAVA_BINARIES);
      List<File> libraries = getFilesFromProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES);
      boolean useDeprecatedProperties = binaries.isEmpty() && libraries.isEmpty();
      if (useDeprecatedProperties) {
        binaries = getFilesFromProperty("sonar.binaries");
        libraries = getFilesFromProperty("sonar.libraries");
      }
      if (pom != null && libraries.isEmpty()) {
        //check mojo
        elements = getLibrariesFromMaven(pom);
      } else {
        elements = Lists.newArrayList(binaries);
        elements.addAll(libraries);
        if (useDeprecatedProperties && !elements.isEmpty()) {
          LOG.warn("sonar.binaries and sonar.libraries are deprecated since version 2.5 of sonar-java-plugin, please use sonar.java.binaries and sonar.java.libraries instead");
        }
      }
    }
  }

  private List<File> getLibrariesFromMaven(MavenProject pom) {
    try {
      List<File> files = Lists.newArrayList();
      if (pom.getCompileClasspathElements() != null) {
        for (String classPathString : (List<String>) pom.getCompileClasspathElements()) {
          files.add(new File(classPathString));
        }
      }
      if (pom.getBuild().getOutputDirectory() != null) {
        File outputDirectoryFile = new File(pom.getBuild().getOutputDirectory());
        if (outputDirectoryFile.exists()) {
          files.add(outputDirectoryFile);
        }
      }
      return files;
    } catch (DependencyResolutionRequiredException e) {
      throw new SonarException("Fail to create the project classloader", e);
    }
  }

}
