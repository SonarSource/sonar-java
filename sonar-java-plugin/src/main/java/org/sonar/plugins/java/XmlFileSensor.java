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
package org.sonar.plugins.java;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.checks.CheckList;
import org.sonarsource.analyzer.commons.xml.ParseException;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck;

public class XmlFileSensor implements Sensor {

  private static final Logger LOG = Loggers.get(XmlFileSensor.class);

  private final Checks<SonarXmlCheck> checks;

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("XML rules for Java projects");
  }

  public XmlFileSensor(CheckFactory checkFactory) {
    this.checks = checkFactory.<SonarXmlCheck>create(CheckList.REPOSITORY_KEY).addAnnotatedChecks((Iterable) CheckList.getXmlChecks());
  }

  @Override
  public void execute(SensorContext context) {
    FileSystem fs = context.fileSystem();
    FilePredicate xmlFilesPredicate = fs.predicates().matchesPathPattern("**/*.xml");
    // TODO: add progress report
    fs.inputFiles(xmlFilesPredicate).forEach(inputFile -> scanFile(context, inputFile));
  }

  private void scanFile(SensorContext context, InputFile inputFile) {
    XmlFile xmlFile;
    try {
      xmlFile = XmlFile.create(inputFile);
    } catch (ParseException | IOException e) {
      LOG.debug("Skipped '{}' due to parsing error", inputFile.toString());
      return;
    }

    checks.all().forEach(check -> {
      RuleKey ruleKey = checks.ruleKey(check);
      scanFile(context, xmlFile, check, ruleKey);
    });
  }

  @VisibleForTesting
  void scanFile(SensorContext context, XmlFile xmlFile, SonarXmlCheck check, RuleKey ruleKey) {
    try {
      check.scanFile(context, ruleKey, xmlFile);
    } catch (Exception e) {
      LOG.error(String.format("Failed to analyze '%s' with rule %s", xmlFile.getInputFile().toString(), ruleKey), e);
    }
  }
}
