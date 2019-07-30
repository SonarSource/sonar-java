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
package org.sonar.java.externalreport;

import java.io.File;
import java.util.function.BiConsumer;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

public final class ExternalIssueUtils {

  private static final Logger LOG = Loggers.get(ExternalIssueUtils.class);

  private ExternalIssueUtils() {
    // utility class
  }

  public static void importIfExist(String linterName, SensorContext sensorContext,  File reportFile, BiConsumer<File, SensorContext> importFunction) {
    if (!reportFile.exists()) {
      LOG.warn("{} report not found: {}", linterName, reportFile);
      return;
    }
    importFunction.accept(reportFile, sensorContext);
  }

  public static void saveIssue(SensorContext context, ExternalRuleLoader ruleLoader, InputFile inputFile, String engineId, String ruleId, String line, String message) {
    NewExternalIssue newExternalIssue = context.newExternalIssue();

    newExternalIssue
      .type(ruleLoader.ruleType(ruleId))
      .severity(ruleLoader.ruleSeverity(ruleId))
      .remediationEffortMinutes(ruleLoader.ruleConstantDebtMinutes(ruleId));

    NewIssueLocation primaryLocation = newExternalIssue.newLocation()
      .message(message)
      .on(inputFile);

    if (!line.isEmpty() && !line.equals("0")) {
      primaryLocation.at(inputFile.selectLine(Integer.parseInt(line)));
    }

    newExternalIssue
      .at(primaryLocation)
      .engineId(engineId)
      .ruleId(ruleId)
      .save();
  }

}
