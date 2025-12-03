/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.externalreport;

import java.io.File;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

public final class ExternalIssueUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ExternalIssueUtils.class);

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

    if (!line.isEmpty() && !"0".equals(line)) {
      primaryLocation.at(inputFile.selectLine(Integer.parseInt(line)));
    }

    newExternalIssue
      .at(primaryLocation)
      .engineId(engineId)
      .ruleId(ruleId)
      .save();
  }

}
