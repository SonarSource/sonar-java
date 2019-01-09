/*
 * SonarQube Java
 * Copyright (C) 2013-2019 SonarSource SA
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
