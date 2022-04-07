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
import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.caching.CacheContext;

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
   * Scan based on the raw file and cached data (ie: No tree is available at this stage).
   * The rule should leverage data from the read cache.
   * The rule should persist data to the write cache for future analyses.
   *
   * @param inputFile The file that will eventually be scanned
   * @param cacheContext Provides all necessary information to use the caches
   * @return True if successful (ie: no further scanning is required). False by default or if the file cannot be scanned exhaustively without contents.
   */
  @Beta
  default boolean scanWithoutParsing(InputFile inputFile, CacheContext cacheContext) {
    return false;
  }
}
