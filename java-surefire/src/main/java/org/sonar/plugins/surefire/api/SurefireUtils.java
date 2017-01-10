/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;

import java.io.File;

/**
 * @since 2.4
 */
public final class SurefireUtils {

  private static final Logger LOGGER = Loggers.get(SurefireUtils.class);
  public static final String SUREFIRE_REPORTS_PATH_PROPERTY = "sonar.junit.reportsPath";

  private SurefireUtils() {
  }

  /**
   * Find the directory containing the surefire reports.
   * @param settings Analysis settings.
   * @param fs FileSystem containing indexed files.
   * @param pathResolver Path solver.
   * @return The directory containing the surefire reports or default one (target/surefire-reports) if not found (not configured or not found).
   */
  public static File getReportsDirectory(Settings settings, FileSystem fs, PathResolver pathResolver) {
    File dir = getReportsDirectoryFromProperty(settings, fs, pathResolver);
    if (dir == null) {
      dir = new File(fs.baseDir(), "target/surefire-reports");
    }
    return dir;
  }

  @CheckForNull
  private static File getReportsDirectoryFromProperty(Settings settings, FileSystem fs, PathResolver pathResolver) {
    if(settings.hasKey(SUREFIRE_REPORTS_PATH_PROPERTY)) {
      String path = settings.getString(SUREFIRE_REPORTS_PATH_PROPERTY);
      if (path != null) {
        try {
          return pathResolver.relativeFile(fs.baseDir(), path);
        } catch (Exception e) {
          LOGGER.info("Surefire report path: "+fs.baseDir()+"/"+path +" not found.");
        }
      }
    }
    return null;
  }

}
