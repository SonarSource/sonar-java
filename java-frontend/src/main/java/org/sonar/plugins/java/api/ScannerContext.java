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
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.plugins.java.api.caching.CacheContext;

public interface ScannerContext {
  /**
   * Report an issue at the project level.
   *
   * @param check   The check raising the issue.
   * @param message Message to display to the user
   * @since SonarJava 5.12: Dropping support of file-related methods
   */
  void addIssueOnProject(JavaCheck check, String message);

  /**
   * {@link InputComponent} representing the project being analyzed
   *
   * @return the project component
   * @since SonarJava 5.12: Dropping support of file-related methods
   */
  InputComponent getProject();

  /**
   * The working directory used by the analysis.
   *
   * @return the current working directory.
   */
  File getWorkingDirectory();

  /**
   * Java version defined for the analysis using {@code sonar.java.version} parameter.
   *
   * @return JavaVersion object with API to act on it.
   */
  JavaVersion getJavaVersion();

  /**
   * @return the {@link CacheContext} applicable to this scan.
   */
  CacheContext getCacheContext();
}
