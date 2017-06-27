/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import com.google.common.base.Strings;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.squidbridge.api.AnalysisException;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

public class JavaClasspath extends AbstractJavaClasspath {

  private static final Logger LOG = Loggers.get(JavaClasspath.class);

  public JavaClasspath(Settings settings, FileSystem fs) {
    super(settings, fs, InputFile.Type.MAIN);
  }

  @Override
  protected void init() {
    if (!initialized) {
      validateLibraries = fs.hasFiles(fs.predicates().all());
      Profiler profiler = Profiler.create(LOG).startInfo("JavaClasspath initialization");
      initialized = true;
      binaries = new ArrayList<>(getFilesFromProperty(JavaClasspathProperties.SONAR_JAVA_BINARIES));
      Set<File> libraries = getFilesFromProperty(JavaClasspathProperties.SONAR_JAVA_LIBRARIES);
      if (binaries.isEmpty() && libraries.isEmpty() && useDeprecatedProperties()) {
        throw new AnalysisException(
          "sonar.binaries and sonar.libraries are not supported since version 4.0 of sonar-java-plugin, please use sonar.java.binaries and sonar.java.libraries instead");
      }
      elements = new ArrayList<>(binaries);
      if (libraries.isEmpty() && hasJavaSources()) {
        LOG.warn("Bytecode of dependencies was not provided for analysis of source files, " +
          "you might end up with less precise results. Bytecode can be provided using sonar.java.libraries property");
      }
      elements.addAll(libraries);
      profiler.stopInfo();
    }
  }

  private boolean useDeprecatedProperties() {
    return !Strings.isNullOrEmpty(settings.getString("sonar.binaries")) && !Strings.isNullOrEmpty(settings.getString("sonar.libraries"));
  }

}
