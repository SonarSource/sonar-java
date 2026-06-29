/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.classpath;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;
import org.sonar.java.AnalysisWarningsWrapper;

import static org.sonar.java.classpath.ClasspathProperties.SONAR_JAVA_BINARIES;
import static org.sonar.java.classpath.ClasspathProperties.SONAR_JAVA_LIBRARIES;

public class ClasspathForMain extends AbstractClasspath {

  public ClasspathForMain(Configuration settings, FileSystem fs, AnalysisWarningsWrapper analysisWarnings) {
    super(settings, fs, InputFile.Type.MAIN, SONAR_JAVA_BINARIES, SONAR_JAVA_LIBRARIES, analysisWarnings);
  }

  public ClasspathForMain(Configuration settings, FileSystem fs) {
    this(settings, fs, AnalysisWarningsWrapper.NOOP_ANALYSIS_WARNINGS);
  }

}
