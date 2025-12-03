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

import java.io.File;
import javax.annotation.CheckForNull;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.plugins.java.api.caching.CacheContext;

public interface ModuleScannerContext {
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
   * @deprecated use {@link #getRootProjectWorkingDirectory()} instead
   */
  @Deprecated(since = "7.12")
  File getWorkingDirectory();

  /**
   * Java version defined for the analysis using {@code sonar.java.version} parameter.
   *
   * @return JavaVersion object with API to act on it.
   */
  JavaVersion getJavaVersion();

  /**
   * To be used to know if the current file is in an android context or not.
   * This value is determined thanks to the presence of android classes in the classpath.
   *
   * @return true if the current file is in an android context.
   */
  boolean inAndroidContext();

  /**
   * @return the {@link CacheContext} applicable to this scan.
   */
  CacheContext getCacheContext();

  /**
   * @return The working directory used by the analysis on project-level. Even if the project contains multiple modules, this method
   * will always return the same value.
   */
  File getRootProjectWorkingDirectory();

  /**
   * @return A key that uniquely identifies the current module, provided that this project consists of multiple modules.
   */
  String getModuleKey();

  /**
   * @return The Sonar product (SONARQUBE/SONARLINT) which forms the current execution context of the scan.
   * See also {@link SonarRuntime#getProduct()}.
   * In a production environment, this method never returns null but in testing contexts, it may happen.
   */
  @CheckForNull
  SonarProduct sonarProduct();
}
