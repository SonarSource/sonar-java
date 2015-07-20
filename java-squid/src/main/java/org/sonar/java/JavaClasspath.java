/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import java.io.File;
import java.util.List;

public class JavaClasspath extends AbstractJavaClasspath {

  private static final Logger LOG = LoggerFactory.getLogger(JavaClasspath.class);

  public JavaClasspath(Project project, Settings settings, FileSystem fs) {
    super(project, settings, fs, InputFile.Type.MAIN);
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
      elements = Lists.newArrayList(binaries);
      elements.addAll(libraries);
      if (useDeprecatedProperties && !elements.isEmpty()) {
        LOG.warn("sonar.binaries and sonar.libraries are deprecated since version 2.5 of sonar-java-plugin, please use sonar.java.binaries and sonar.java.libraries instead");
      }
    }
  }

}
