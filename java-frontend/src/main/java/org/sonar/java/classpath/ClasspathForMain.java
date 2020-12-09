/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.classpath;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.java.AnalysisException;
import org.sonar.java.AnalysisWarningsWrapper;

public class ClasspathForMain extends AbstractClasspath {

  private static final Logger LOG = Loggers.get(ClasspathForMain.class);

  private final AnalysisWarningsWrapper analysisWarnings;

  public ClasspathForMain(Configuration settings, FileSystem fs, AnalysisWarningsWrapper analysisWarnings) {
    super(settings, fs, InputFile.Type.MAIN);
    this.analysisWarnings = analysisWarnings;
  }

  public ClasspathForMain(Configuration settings, FileSystem fs) {
    this(settings, fs, AnalysisWarningsWrapper.NOOP_ANALYSIS_WARNINGS);
  }

  @Override
  protected void init() {
    if (!initialized) {
      validateLibraries = fs.hasFiles(fs.predicates().all());
      Profiler profiler = Profiler.create(LOG).startInfo("JavaClasspath initialization");
      initialized = true;
      binaries.addAll(getFilesFromProperty(ClasspathProperties.SONAR_JAVA_BINARIES));

      Set<File> libraries = new LinkedHashSet<>(getJdkJars());
      libraries.addAll(getFilesFromProperty(ClasspathProperties.SONAR_JAVA_LIBRARIES));
      if (binaries.isEmpty() && libraries.isEmpty() && useDeprecatedProperties()) {
        throw new AnalysisException(
          "sonar.binaries and sonar.libraries are not supported since version 4.0 of the SonarSource Java Analyzer,"
            + " please use sonar.java.binaries and sonar.java.libraries instead");
      }
      if (binaries.isEmpty() && hasMoreThanOneJavaFile()) {
        if(isSonarLint()) {
          LOG.warn("sonar.java.binaries is empty, please double check your configuration");
        } else {
          throw new AnalysisException("Your project contains .java files, please provide compiled classes with sonar.java.binaries property,"
            + " or exclude them from the analysis with sonar.exclusions property.");
        }
      }
      elements.addAll(binaries);
      if (libraries.isEmpty() && hasJavaSources()) {
        String warning = "Bytecode of dependencies was not provided for analysis of source files, " +
          "you might end up with less precise results. Bytecode can be provided using sonar.java.libraries property.";
        LOG.warn(warning);
        analysisWarnings.addUnique(warning);
      }
      elements.addAll(libraries);
      profiler.stopInfo();
    }
  }

  protected boolean isSonarLint() {
    return false;
  }

  private boolean useDeprecatedProperties() {
    return isNotNullOrEmpty(settings.get("sonar.binaries").orElse(null)) && isNotNullOrEmpty(settings.get("sonar.libraries").orElse(null));
  }

  private static boolean isNotNullOrEmpty(@Nullable String string) {
    return string != null && !string.isEmpty();
  }
}
