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
package org.sonar.plugins.java.api;

import org.sonar.java.annotations.Beta;
import java.io.File;
import java.util.Collection;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * Interface to get the mapping between java classes and files (as multiple classes can be declared in one file).
 */
@Beta
@ScannerSide
@SonarLintSide
public interface JavaResourceLocator extends JavaFileScanner {

  /**
   * Retrieve a SonarQube resource by the class name.
   * @param className fully qualified name of the analyzed class
   * @return null if not found
   */
  @CheckForNull
  InputFile findResourceByClassName(String className);

  /**
   * .class files to be analyzed.
   * Used by the findbugs plugin.
   * @return a list of .class files corresponding to the source files to be analyzed.
   */
  Collection<File> classFilesToAnalyze();

  /**
   * The folders containing the binary .class files.
   * @return a list of folders.
   */
  Collection<File> binaryDirs();

  /**
   * The folders containing the binary .class files corresponding to the tests.
   * @return a list of folders.
   * @since SonarJava 7.15
   */
  Collection<File> testBinaryDirs();

  /**
   * Classpath configured for the project.
   * @return the list of jar and class files constituting the classpath of the analyzed project.
   */
  Collection<File> classpath();

  /**
   * Classpath configured for the project tests.
   * @return the list of jar and class files constituting the classpath of the analyzed project.
   * @since SonarJava 7.15
   */
  Collection<File> testClasspath();
}
