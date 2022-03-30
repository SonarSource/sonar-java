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

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.java.annotations.Beta;

/**
 * Common interface for all checks analyzing a java file.
 */
@Beta
public interface JavaFileScanner extends JavaCheck {

  /**
   * Method called after parsing and semantic analysis has been done on file.
   * @param context Context of analysis containing the parsed tree.
   */
  void scanFile(JavaFileScannerContext context);

  /**
   * Returns true if a file needs to be scanned by the rule, false otherwise.
   * This information is deduced from the input file and the read cache.
   * The rule may persist data to the write cache for future analyses.
   *
   * @param inputFile The file thay will eventually be scanned
   * @param readCache The cache of information from previous analyses
   * @param writeCache The cache may persist data for future analyses
   * @return Returns true by default or if the file needs to be scanned again, false otherwise.
   */
  default boolean shouldBeScanned(InputFile inputFile, ReadCache readCache, WriteCache writeCache) {
    return true;
  }
}
