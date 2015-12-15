/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TimeProfiler;

import java.io.File;
import java.util.List;

public class JavaTestClasspath extends AbstractJavaClasspath {

  private static final Logger LOG = LoggerFactory.getLogger(JavaTestClasspath.class);

  public JavaTestClasspath(Project project, Settings settings, FileSystem fs) {
    super(project, settings, fs, InputFile.Type.TEST);
  }

  @Override
  protected void init() {
    if (!initialized) {
      TimeProfiler profiler = new TimeProfiler(getClass()).start("JavaTestClasspath initialization");
      initialized = true;
      validateLibraries = project.getModules().isEmpty();
      binaries = getFilesFromProperty(JavaClasspathProperties.SONAR_JAVA_TEST_BINARIES);
      List<File> libraries = getFilesFromProperty(JavaClasspathProperties.SONAR_JAVA_TEST_LIBRARIES);
      if(libraries.isEmpty()) {
        LOG.warn("Bytecode of dependencies was not provided for analysis of test files, you might end up with less precise results. " +
            "Bytecode can be provided using sonar.java.test.libraries property");
      }
      elements = Lists.newArrayList(binaries);
      elements.addAll(libraries);
      profiler.stop();
    }
  }

}
