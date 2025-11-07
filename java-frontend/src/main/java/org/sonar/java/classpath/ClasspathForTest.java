/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;

public class ClasspathForTest extends AbstractClasspath {

  private static final Logger LOG = LoggerFactory.getLogger(ClasspathForTest.class);

  private boolean hasSuspiciousEmptyLibraries = false;
  private boolean alreadyReported = false;

  public ClasspathForTest(Configuration settings, FileSystem fs) {
    super(settings, fs, InputFile.Type.TEST);
  }

  @Override
  protected void init() {
    if (!initialized) {
      validateLibraries = fs.hasFiles(fs.predicates().all());
      initialized = true;
      binaries.addAll(getFilesFromProperty(ClasspathProperties.SONAR_JAVA_TEST_BINARIES));

      Set<File> libraries = new LinkedHashSet<>(getJdkJars());
      Set<File> extraLibraries = getFilesFromProperty(ClasspathProperties.SONAR_JAVA_TEST_LIBRARIES);
      logResolvedFiles(ClasspathProperties.SONAR_JAVA_TEST_LIBRARIES, extraLibraries);
      libraries.addAll(extraLibraries);
      hasSuspiciousEmptyLibraries = libraries.isEmpty() && hasJavaSources();

      elements.addAll(binaries);
      elements.addAll(libraries);
    }
  }

  @Override
  public void logSuspiciousEmptyLibraries() {
    if (hasSuspiciousEmptyLibraries && !alreadyReported) {
      String warning = String.format(ClasspathProperties.EMPTY_LIBRARIES_WARNING_TEMPLATE, "TEST", ClasspathProperties.SONAR_JAVA_TEST_LIBRARIES);
      LOG.warn(warning);
      alreadyReported = true;
    }
  }

}
