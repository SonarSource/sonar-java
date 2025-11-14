/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

import static org.sonar.java.externalreport.ExternalIssueUtils.importIfExist;

public class SpotBugsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(SpotBugsSensor.class);

  public static final String SPOTBUGS_KEY = "spotbugs";
  private static final String SPOTBUGS_NAME = "SpotBugs";
  public static final String FINDSECBUGS_KEY = "findsecbugs";
  private static final String FINDSECBUGS_NAME = "FindSecBugs";
  public static final String FBCONTRIB_KEY = "fbcontrib";
  private static final String FBCONTRIB_NAME = "fb-contrib";

  private static final String LANGUAGE_KEY = "java";
  public static final String REPORT_PROPERTY_KEY = "sonar.java.spotbugs.reportPaths";

  private final ExternalRuleLoader ruleLoader;

  private final ExternalRuleLoader findSecBugsLoader;

  private final ExternalRuleLoader fbContribLoader;

  public SpotBugsSensor(SonarRuntime sonarRuntime) {
    ruleLoader = new ExternalRuleLoader(
      SpotBugsSensor.SPOTBUGS_KEY,
      SpotBugsSensor.SPOTBUGS_NAME,
      "org/sonar/l10n/java/rules/spotbugs/spotbugs-rules.json",
      SpotBugsSensor.LANGUAGE_KEY,
      sonarRuntime);

    findSecBugsLoader = new ExternalRuleLoader(
      SpotBugsSensor.FINDSECBUGS_KEY,
      SpotBugsSensor.FINDSECBUGS_NAME,
      "org/sonar/l10n/java/rules/spotbugs/findsecbugs-rules.json",
      SpotBugsSensor.LANGUAGE_KEY,
      sonarRuntime);

    fbContribLoader = new ExternalRuleLoader(
      SpotBugsSensor.FBCONTRIB_KEY,
      SpotBugsSensor.FBCONTRIB_NAME,
      "org/sonar/l10n/java/rules/spotbugs/fbcontrib-rules.json",
      SpotBugsSensor.LANGUAGE_KEY,
      sonarRuntime);
  }

  public ExternalRuleLoader ruleLoader() {
    return ruleLoader;
  }

  public ExternalRuleLoader findSecBugsLoader() {
    return findSecBugsLoader;
  }

  public ExternalRuleLoader fbContribLoader() {
    return fbContribLoader;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(SpotBugsSensor.LANGUAGE_KEY)
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PROPERTY_KEY))
      .name("Import of SpotBugs issues");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, REPORT_PROPERTY_KEY);
    reportFiles.forEach(report -> importIfExist(SPOTBUGS_NAME, context, report, this::importReport));
  }

  private void importReport(File reportPath, SensorContext context) {
    try (InputStream in = new FileInputStream(reportPath)) {
      LOG.info("Importing {}", reportPath);

      Map<String, ExternalRuleLoader> otherLoaders = new HashMap<>();
      otherLoaders.put(FINDSECBUGS_KEY, findSecBugsLoader);
      otherLoaders.put(FBCONTRIB_KEY, fbContribLoader);
      SpotBugsXmlReportReader.read(context, in, ruleLoader, otherLoaders);
    } catch (Exception e) {
      LOG.error("Failed to import external issues report: {}", reportPath, e);
    }
  }

}
