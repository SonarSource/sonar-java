/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.plugins.surefire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.surefire.api.SurefireUtils;

import java.io.File;

@DependedUpon("surefire-java")
public class SurefireSensor implements Sensor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SurefireSensor.class);

  private final SurefireJavaParser surefireJavaParser;
  private final Settings settings;
  private final FileSystem fs;
  private final PathResolver pathResolver;

  public SurefireSensor(SurefireJavaParser surefireJavaParser, Settings settings, FileSystem fs, PathResolver pathResolver) {
    this.surefireJavaParser = surefireJavaParser;
    this.settings = settings;
    this.fs = fs;
    this.pathResolver = pathResolver;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return fs.hasFiles(fs.predicates().hasLanguage("java"));
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    File dir = SurefireUtils.getReportsDirectory(settings, fs, pathResolver);
    collect(context, dir);
  }

  protected void collect(SensorContext context, File reportsDir) {
    LOGGER.info("parsing {}", reportsDir);
    surefireJavaParser.collect(context, reportsDir);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
