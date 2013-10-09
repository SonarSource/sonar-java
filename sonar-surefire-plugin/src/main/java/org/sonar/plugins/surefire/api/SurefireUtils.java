/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.surefire.api;

import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenSurefireUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import java.io.File;

/**
 * @since 2.4
 */
public final class SurefireUtils {

  public static final String SUREFIRE_REPORTS_PATH_PROPERTY = "sonar.junit.reportsPath";

  public static File getReportsDirectory(Settings settings, Project project) {
    File dir = getReportsDirectoryFromProperty(settings, project);
    if (dir == null) {
      dir = getReportsDirectoryFromPluginConfiguration(project);
    }
    if (dir == null) {
      dir = getReportsDirectoryFromDefaultConfiguration(project);
    }
    return dir;
  }

  private static File getReportsDirectoryFromProperty(Settings settings, Project project) {
    String path = settings.getString(SUREFIRE_REPORTS_PATH_PROPERTY);
    if (path != null) {
      return project.getFileSystem().resolvePath(path);
    }
    return null;
  }

  private static File getReportsDirectoryFromPluginConfiguration(Project project) {
    MavenPlugin plugin = MavenPlugin.getPlugin(project.getPom(), MavenSurefireUtils.GROUP_ID, MavenSurefireUtils.ARTIFACT_ID);
    if (plugin != null) {
      String path = plugin.getParameter("reportsDirectory");
      if (path != null) {
        return project.getFileSystem().resolvePath(path);
      }
    }
    return null;
  }

  private static File getReportsDirectoryFromDefaultConfiguration(Project project) {
    return new File(project.getFileSystem().getBuildDir(), "surefire-reports");
  }

  private SurefireUtils() {
  }

}
