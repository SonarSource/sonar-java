/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.annotations.Beta;
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
   * Classpath configured for the project.
   * This classpath method is used by the findbugs plugin to configure the analysis.
   * @return the list of jar and class files constituting the classpath of the analyzed project.
   */
  Collection<File> classpath();

}
