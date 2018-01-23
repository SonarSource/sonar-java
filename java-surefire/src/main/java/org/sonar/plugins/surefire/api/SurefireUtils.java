/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.plugins.surefire.api;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * @since 2.4
 */
public final class SurefireUtils {

  private static final Logger LOGGER = Loggers.get(SurefireUtils.class);
  /**
   * @deprecated since 4.11
   */
  @Deprecated
  public static final String SUREFIRE_REPORTS_PATH_PROPERTY = "sonar.junit.reportsPath";
  /**
   * @since 4.11
   */
  public static final String SUREFIRE_REPORT_PATHS_PROPERTY = "sonar.junit.reportPaths";

  private SurefireUtils() {
  }

  /**
   * Find the directories containing the surefire reports.
   * @param settings Analysis settings.
   * @param fs FileSystem containing indexed files.
   * @param pathResolver Path solver.
   * @return The directories containing the surefire reports or default one (target/surefire-reports) if not found (not configured or not found).
   */
  public static List<File> getReportsDirectories(Configuration settings, FileSystem fs, PathResolver pathResolver) {
    File dir = getReportsDirectoryFromDeprecatedProperty(settings, fs, pathResolver);
    List<File> dirs = getReportsDirectoriesFromProperty(settings, fs, pathResolver);
    if (dirs != null) {
      if (dir != null) {
        // both properties are set, deprecated property ignored
        LOGGER.debug("Property '{}' is deprecated and will be ignored, as property '{}' is also set.", SUREFIRE_REPORTS_PATH_PROPERTY, SUREFIRE_REPORT_PATHS_PROPERTY);
      }
      return dirs;
    }
    if (dir != null) {
      LOGGER.info("Property '{}' is deprecated. Use property '{}' instead.", SUREFIRE_REPORTS_PATH_PROPERTY, SUREFIRE_REPORT_PATHS_PROPERTY);
      return Collections.singletonList(dir);
    }
    // both properties are not set
    return Collections.singletonList(new File(fs.baseDir(), "target/surefire-reports"));
  }

  @CheckForNull
  private static List<File> getReportsDirectoriesFromProperty(Configuration settings, FileSystem fs, PathResolver pathResolver) {
    if(settings.hasKey(SUREFIRE_REPORT_PATHS_PROPERTY)) {
      return Arrays.stream(settings.getStringArray(SUREFIRE_REPORT_PATHS_PROPERTY))
        .map(String::trim)
        .map(path -> getFileFromPath(fs, pathResolver, path))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    }
    return null;
  }

  @CheckForNull
  private static File getReportsDirectoryFromDeprecatedProperty(Configuration settings, FileSystem fs, PathResolver pathResolver) {
    if(settings.hasKey(SUREFIRE_REPORTS_PATH_PROPERTY)) {
      String path = settings.get(SUREFIRE_REPORTS_PATH_PROPERTY).orElse(null);
      if (path != null) {
        return getFileFromPath(fs, pathResolver, path);
      }
    }
    return null;
  }

  @CheckForNull
  private static File getFileFromPath(FileSystem fs, PathResolver pathResolver, String path) {
    try {
      return pathResolver.relativeFile(fs.baseDir(), path);
    } catch (Exception e) {
      // exceptions on file not found was only occurring with SQ 5.6 LTS, not with SQ 6.4
      LOGGER.info("Surefire report path: {}/{} not found.", fs.baseDir(), path);
    }
    return null;
  }

}
