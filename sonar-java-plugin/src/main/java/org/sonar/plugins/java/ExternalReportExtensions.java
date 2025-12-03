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
package org.sonar.plugins.java;

import org.sonar.api.Plugin.Context;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.java.externalreport.CheckstyleSensor;
import org.sonar.java.externalreport.ExternalRulesDefinition;
import org.sonar.java.externalreport.PmdSensor;
import org.sonar.java.externalreport.SpotBugsSensor;

public final class ExternalReportExtensions {

  private static final String EXTERNAL_ANALYZERS_CATEGORY = "External Analyzers";
  private static final String JAVA_SUBCATEGORY = "Java";

  private ExternalReportExtensions() {
    // utility class
  }

  public static void define(Context context) {
    var checkstyleSensor = new CheckstyleSensor(context.getRuntime());
    var pmdSensor = new PmdSensor(context.getRuntime());
    var spotBugsSensor = new SpotBugsSensor(context.getRuntime());
    context.addExtension(checkstyleSensor);
    context.addExtension(pmdSensor);
    context.addExtension(spotBugsSensor);

    context.addExtension(new ExternalRulesDefinition(checkstyleSensor.ruleLoader(), CheckstyleSensor.LINTER_KEY));
    context.addExtension(
      PropertyDefinition.builder(CheckstyleSensor.REPORT_PROPERTY_KEY)
        .name("Checkstyle Report Files")
        .description("Paths (absolute or relative) to xml files with Checkstyle issues.")
        .category(EXTERNAL_ANALYZERS_CATEGORY)
        .subCategory(JAVA_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build());

    context.addExtension(new ExternalRulesDefinition(pmdSensor.ruleLoader(), PmdSensor.LINTER_KEY));
    context.addExtension(
      PropertyDefinition.builder(PmdSensor.REPORT_PROPERTY_KEY)
        .name("PMD Report Files")
        .description("Paths (absolute or relative) to xml files with PMD issues.")
        .category(EXTERNAL_ANALYZERS_CATEGORY)
        .subCategory(JAVA_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build());

    context.addExtension(new ExternalRulesDefinition(spotBugsSensor.ruleLoader(), SpotBugsSensor.SPOTBUGS_KEY));
    context.addExtension(new ExternalRulesDefinition(spotBugsSensor.findSecBugsLoader(), SpotBugsSensor.FINDSECBUGS_KEY));
    context.addExtension(new ExternalRulesDefinition(spotBugsSensor.fbContribLoader(), SpotBugsSensor.FBCONTRIB_KEY));
    context.addExtension(
      PropertyDefinition.builder(SpotBugsSensor.REPORT_PROPERTY_KEY)
        .name("SpotBugs Report Files")
        .description("Paths (absolute or relative) to xml files with SpotBugs issues.")
        .category(EXTERNAL_ANALYZERS_CATEGORY)
        .subCategory(JAVA_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build());
  }


}
