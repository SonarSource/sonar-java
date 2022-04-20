/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.plugins.java.api;

import java.io.File;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.tree.Tree;

public interface InputFileScannerContext {

  /**
   * Report an issue at file level.
   * @param check The check raising the issue.
   * @param message Message to display to the user
   */
  void addIssueOnFile(JavaCheck check, String message);

  /**
   * Report an issue on a specific line. Prefer {@link JavaFileScannerContext#reportIssue(JavaCheck, Tree, String)} for more precise reporting.
   * @param line line on which to report the issue
   * @param check The check raising the issue.
   * @param message Message to display to the user
   */
  void addIssue(int line, JavaCheck check, String message);

  /**
   * Report an issue on a specific line. Prefer {@link JavaFileScannerContext#reportIssue(JavaCheck, Tree, String, List, Integer)} for more precise reporting.
   * @param line line on which to report the issue
   * @param check The check raising the issue.
   * @param message Message to display to the user
   * @param cost computed remediation cost if applicable, null if not.
   */
  void addIssue(int line, JavaCheck check, String message, @Nullable Integer cost);

  /**
   * Report an issue at at the project level.
   * @param check The check raising the issue.
   * @param message Message to display to the user
   * @since SonarJava 5.12: Dropping support of file-related methods
   */
  void addIssueOnProject(JavaCheck check, String message);


  /**
   * InputFile under analysis.
   * @return the currently analyzed {@link InputFile}.
   * @since SonarJava 5.12: Dropping support of file-related methods
   */
  InputFile getInputFile();

  /**
   * {@link InputComponent} representing the project being analyzed
   * @return the project component
   * @since SonarJava 5.12: Dropping support of file-related methods
   */
  InputComponent getProject();

  /**
   * The working directory used by the analysis.
   * @return the current working directory.
   */
  File getWorkingDirectory();

  /**
   * Java version defined for the analysis using {@code sonar.java.version} parameter.
   * @return JavaVersion object with API to act on it.
   */
  JavaVersion getJavaVersion();

  /**
   * To be used to know if the current file is in an android context or not.
   * This value is determined thanks to the presence of android classes in the classpath.
   * @return true if the current file is in an android context.
   */
  boolean inAndroidContext();

  /**
   * @return the {@link CacheContext} applicable to this scan.
   */
  CacheContext getCacheContext();
}
