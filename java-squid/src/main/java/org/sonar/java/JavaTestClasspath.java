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
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import java.io.File;
import java.util.List;

public class JavaTestClasspath extends AbstractJavaClasspath {


  public JavaTestClasspath(Project project, Settings settings, FileSystem fs) {
    super(project, settings, fs);
  }

  protected void init() {
    if (!initalized) {
      initalized = true;
      validateLibraries = project.getModules().isEmpty();
      FilePredicates predicates = fs.predicates();
      hasJavaSources = fs.hasFiles(predicates.and(predicates.hasLanguage("java"), predicates.hasType(InputFile.Type.TEST)));
      binaries = getFilesFromProperty(JavaClasspathProperties.SONAR_JAVA_TEST_BINARIES);
      List<File> libraries = getFilesFromProperty(JavaClasspathProperties.SONAR_JAVA_TEST_LIBRARIES);
      elements = Lists.newArrayList(binaries);
      elements.addAll(libraries);
    }
  }

}
