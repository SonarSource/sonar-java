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
package org.sonar.plugins.java;

import org.sonar.api.Plugin.Context;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.Version;
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
    context.addExtension(CheckstyleSensor.class);
    context.addExtension(PmdSensor.class);
    context.addExtension(SpotBugsSensor.class);

    boolean externalIssuesSupported = context.getSonarQubeVersion().isGreaterThanOrEqual(Version.create(7, 2));
    if (externalIssuesSupported) {
      context.addExtension(new ExternalRulesDefinition(CheckstyleSensor.RULE_LOADER, CheckstyleSensor.LINTER_KEY));
      context.addExtension(
        PropertyDefinition.builder(CheckstyleSensor.REPORT_PROPERTY_KEY)
          .name("Checkstyle Report Files")
          .description("Paths (absolute or relative) to xml files with Checkstyle issues.")
          .category(EXTERNAL_ANALYZERS_CATEGORY)
          .subCategory(JAVA_SUBCATEGORY)
          .onQualifiers(Qualifiers.PROJECT)
          .multiValues(true)
          .build());

      context.addExtension(new ExternalRulesDefinition(PmdSensor.RULE_LOADER, PmdSensor.LINTER_KEY));
      context.addExtension(
        PropertyDefinition.builder(PmdSensor.REPORT_PROPERTY_KEY)
          .name("PMD Report Files")
          .description("Paths (absolute or relative) to xml files with PMD issues.")
          .category(EXTERNAL_ANALYZERS_CATEGORY)
          .subCategory(JAVA_SUBCATEGORY)
          .onQualifiers(Qualifiers.PROJECT)
          .multiValues(true)
          .build());

      context.addExtension(new ExternalRulesDefinition(SpotBugsSensor.RULE_LOADER, SpotBugsSensor.SPOTBUGS_KEY));
      context.addExtension(new ExternalRulesDefinition(SpotBugsSensor.FINDSECBUGS_LOADER, SpotBugsSensor.FINDSECBUGS_KEY));
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

}
