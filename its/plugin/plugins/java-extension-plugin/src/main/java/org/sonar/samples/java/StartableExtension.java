/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource Sàrl
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
package org.sonar.samples.java;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.plugins.java.api.JavaResourceLocator;

@ScannerSide
public class StartableExtension {

  private final FileSystem fileSystem;
  private final JavaResourceLocator javaResourceLocator;

  /**
   * Used to check compatibility of extensions, by requiring java resource locator and filesystem,
   * which would trigger exceptions such as:
   * 
   *   Caused by: org.apache.maven.plugin.MojoExecutionException: 
   *   Accessing the filesystem before the Sensor phase is not supported. Please update your plugin.
   * 
   * @param fileSystem
   * @param javaResourceLocator
   */
  public StartableExtension(FileSystem fileSystem, JavaResourceLocator javaResourceLocator) {
    this.fileSystem = fileSystem;
    this.javaResourceLocator = javaResourceLocator;
  }

  public void start() {
    // do nothing, just allow recognition of the class a being a startable extension
  }

}
