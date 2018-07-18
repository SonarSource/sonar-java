/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalReportProvider;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

public class PmdSensor implements Sensor {

  private static final Logger LOG = Loggers.get(PmdSensor.class);

  public static final String REPORT_PROPERTY_KEY = "sonar.java.pmd.reportPaths";

  public static final String LINTER_KEY = "pmd";
  private static final String LINTER_NAME = "PMD";
  private static final String LANGUAGE_KEY = "java";

  public static final ExternalRuleLoader RULE_LOADER = new ExternalRuleLoader(
    PmdSensor.LINTER_KEY,
    PmdSensor.LINTER_NAME,
    "org/sonar/l10n/java/rules/pmd/rules.json",
    PmdSensor.LANGUAGE_KEY);

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PROPERTY_KEY))
      .name("Import of PMD issues");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, REPORT_PROPERTY_KEY);
    reportFiles.forEach(report -> importReport(report, context));
  }

  private static void importReport(File reportFile, SensorContext context) {
    try {
      LOG.info("Importing {}", reportFile);
      PmdXmlReportReader.read(context, reportFile, RULE_LOADER);
    } catch (FileNotFoundException e) {
      LOG.error("Can't find PMD XML report: {}", reportFile);
    } catch (XMLStreamException | IOException | RuntimeException e) {
      LOG.error("Can't read PMD XML report: {}", reportFile, e);
    }
  }

}
