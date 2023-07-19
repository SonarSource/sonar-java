/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.plugins.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.notifications.AnalysisWarnings;

public class DroppedPropertiesSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(DroppedPropertiesSensor.class);

  private static final String REPORT_PATHS_PROPERTY = "sonar.jacoco.reportPaths";
  private static final String REPORT_PATH_PROPERTY = "sonar.jacoco.reportPath";
  private static final List<String> REMOVED_REPORT_PATH_PROPERTIES = Arrays.asList(REPORT_PATH_PROPERTY, REPORT_PATHS_PROPERTY);

  private static final String IT_REPORT_PATH_PROPERTY = "sonar.jacoco.itReportPath";
  private static final String REPORT_MISSING_FORCE_ZERO = "sonar.jacoco.reportMissing.force.zero";

  private static final List<String> OTHER_REMOVED_PROPERTIES = Arrays.asList(REPORT_MISSING_FORCE_ZERO, IT_REPORT_PATH_PROPERTY);

  private static final List<String> ALL_REMOVED_PROPERTIES = new ArrayList<>();

  private final AnalysisWarnings analysisWarnings;

  public DroppedPropertiesSensor(AnalysisWarnings analysisWarnings) {
    this.analysisWarnings = analysisWarnings;
    ALL_REMOVED_PROPERTIES.addAll(REMOVED_REPORT_PATH_PROPERTIES);
    ALL_REMOVED_PROPERTIES.addAll(OTHER_REMOVED_PROPERTIES);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage("java")
      .onlyWhenConfiguration(configuration ->
        ALL_REMOVED_PROPERTIES.stream().anyMatch(configuration::hasKey))
      .name("Removed properties sensor");
  }

  @Override
  public void execute(SensorContext context) {
    if (!(context.config().hasKey(REPORT_PATHS_PROPERTY) && context.config().hasKey(REPORT_PATH_PROPERTY))) {
      // For compatibility reasons, SonarQube gradle plugin set these two properties, we want to report
      // only when one of the two is set. See SONARJAVA-3300.
      warnIfConfigHasKey(REMOVED_REPORT_PATH_PROPERTIES, context);
    }

    warnIfConfigHasKey(OTHER_REMOVED_PROPERTIES, context);
  }

  private void warnIfConfigHasKey(List<String> properties, SensorContext context) {
    properties.forEach(prop -> {
      if (context.config().hasKey(prop)) {
        String msg = "Property '" + prop + "' is no longer supported. Use JaCoCo's xml report and sonar-jacoco plugin.";
        analysisWarnings.addUnique(msg);
        LOG.warn(msg);
      }
    });
  }

}
