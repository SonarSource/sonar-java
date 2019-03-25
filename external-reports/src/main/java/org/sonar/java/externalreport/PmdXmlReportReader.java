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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;
import org.sonarsource.analyzer.commons.xml.SafetyFactory;

public class PmdXmlReportReader {

  private static final Logger LOG = Loggers.get(PmdXmlReportReader.class);

  private static final Map<Integer, Severity> SEVERITIES = severities();

  private final SensorContext context;
  private final File reportFile;
  private final ExternalRuleLoader ruleLoader;

  private InputFile inputFile = null;
  private NewExternalIssue issue = null;
  private NewIssueLocation issueLocation = null;
  private StringBuilder issueMessage = new StringBuilder();

  public PmdXmlReportReader(SensorContext context, File reportFile, ExternalRuleLoader ruleLoader) {
    this.context = context;
    this.reportFile = reportFile;
    this.ruleLoader = ruleLoader;
  }

  public static void read(SensorContext context, File reportFile, ExternalRuleLoader ruleLoader) throws XMLStreamException, IOException {
    new PmdXmlReportReader(context, reportFile, ruleLoader).parse();
  }

  private void parse() throws XMLStreamException, IOException {
    try (InputStream inputStream = new FileInputStream(reportFile)) {
      XMLEventReader reader = SafetyFactory.createXMLInputFactory().createXMLEventReader(inputStream);
      while (reader.hasNext()) {
        onXmlEvent(reader.nextEvent());
      }
    }
  }

  private void onXmlEvent(XMLEvent event) {
    if (event.isStartElement()) {
      StartElement element = event.asStartElement();
      String elementName = element.getName().getLocalPart();
      if ("file".equals(elementName)) {
        String filePath = getAttributeValue(element, "name");
        FilePredicates predicates = context.fileSystem().predicates();
        inputFile = context.fileSystem().inputFile(predicates.hasPath(filePath));
        if (inputFile == null) {
          LOG.warn("No input file found for {}. No PMD issue will be imported on this file.", filePath);
        }
      } else if ("violation".equals(elementName) && inputFile != null) {
        onViolationStartElement(element);
      }

    } else if (event.isCharacters()) {
      issueMessage.append(event.asCharacters().getData());

    } else if (event.isEndElement()
      && "violation".equals(event.asEndElement().getName().getLocalPart())
      && inputFile != null
      && issue != null) {
      issueLocation.message(issueMessage.toString());
      issue.at(issueLocation).save();
    }
  }

  private void onViolationStartElement(StartElement element) {
    try {
      TextRange textRange = textRange(element);
      String ruleId = getAttributeValue(element, "rule");
      issue = context.newExternalIssue()
        .engineId(PmdSensor.LINTER_KEY)
        .ruleId(ruleId)
        .type(RuleType.CODE_SMELL)
        .severity(SEVERITIES.get(getAttributeAsInt(element, "priority")))
        .remediationEffortMinutes(ruleLoader.ruleConstantDebtMinutes(ruleId));
      issueLocation = issue.newLocation()
        .on(inputFile)
        .at(textRange);
      issueMessage = new StringBuilder();
    } catch (RuntimeException e) {
      int lineNumber = element.getLocation().getLineNumber();
      LOG.warn("Can't import issue at line " + lineNumber + " in " + reportFile + ": " + e.getMessage());
      issue = null;
    }
  }

  private TextRange textRange(StartElement violationElement) {
    Integer beginLine = getAttributeAsInt(violationElement, "beginline");
    try {
      Integer endLine = getAttributeAsInt(violationElement, "endline");
      Integer beginColumn = getAttributeAsInt(violationElement, "begincolumn");
      Integer endColumn = getAttributeAsInt(violationElement, "endcolumn");
      return inputFile.newRange(beginLine, beginColumn - 1, endLine, endColumn);
    } catch (RuntimeException e) {
      // Some PMD rules seem to report invalid line offsets, e.g. TooManyStaticImports
      return inputFile.selectLine(beginLine);
    }
  }

  private static String getAttributeValue(StartElement startElement, String attributeName) {
    Attribute attribute = startElement.getAttributeByName(new QName(attributeName));
    return attribute == null ? "" : attribute.getValue();
  }

  private static Integer getAttributeAsInt(StartElement startElement, String attributeName) {
    return Integer.parseInt(getAttributeValue(startElement, attributeName));
  }

  private static Map<Integer, Severity> severities() {
    Map<Integer, Severity> map = new HashMap<>();
    map.put(1, Severity.BLOCKER);
    map.put(2, Severity.CRITICAL);
    map.put(3, Severity.MAJOR);
    map.put(4, Severity.MINOR);
    map.put(5, Severity.INFO);
    return map;
  }

}
