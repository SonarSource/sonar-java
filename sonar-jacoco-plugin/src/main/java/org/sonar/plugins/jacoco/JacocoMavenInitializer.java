/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
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
package org.sonar.plugins.jacoco;

import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;

/**
 * Should be executed before {@link JaCoCoSensor}.
 */
@Phase(name = Phase.Name.PRE)
@SupportedEnvironment("maven")
public class JacocoMavenInitializer implements Sensor, CoverageExtension, DependsUponMavenPlugin {

  private JaCoCoMavenPluginHandler handler;
  private JacocoConfiguration configuration;

  public JacocoMavenInitializer(JaCoCoMavenPluginHandler handler, JacocoConfiguration configuration) {
    this.handler = handler;
    this.configuration = configuration;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return configuration.isEnabled(project)
      && project.getAnalysisType().equals(Project.AnalysisType.DYNAMIC)
      && !project.getFileSystem().testFiles(Java.KEY).isEmpty();
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    // nothing to do
  }

  public MavenPluginHandler getMavenPluginHandler(Project project) {
    return handler;
  }

}
