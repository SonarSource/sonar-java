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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;
import org.sonarsource.analyzer.commons.xml.SafetyFactory;

public class SpotBugsXmlReportReader {

  private static final Logger LOG = Loggers.get(SpotBugsXmlReportReader.class);

  private static final QName TYPE_ATTRIBUTE = new QName("type");
  private static final QName SOURCE_PATH_ATTRIBUTE = new QName("sourcepath");
  private static final QName START_ATTRIBUTE = new QName("start");

  private final SensorContext context;
  private final ExternalRuleLoader defaultRuleLoader;
  private final Map<String, ExternalRuleLoader> otherLoaders;

  private List<String> sourceDirs = new ArrayList<>();
  private String bugInstanceType = "";
  private String bugInstanceLongMessage = "";
  private String sourceLinePath = "";
  private String sourceLineStart = "";
  private StringBuilder textBuilder = null;

  private SpotBugsXmlReportReader(SensorContext context, ExternalRuleLoader defaultRuleLoader, Map<String, ExternalRuleLoader> otherLoaders) {
    this.context = context;
    this.defaultRuleLoader = defaultRuleLoader;
    this.otherLoaders = otherLoaders;
  }

  static void read(SensorContext context, InputStream in, ExternalRuleLoader defaultRuleLoader, Map<String, ExternalRuleLoader> otherLoaders)
    throws XMLStreamException, IOException {
    new SpotBugsXmlReportReader(context, defaultRuleLoader, otherLoaders).read(in);
  }

  private void read(InputStream in) throws XMLStreamException, IOException {
    XMLEventReader reader = SafetyFactory.createXMLInputFactory().createXMLEventReader(in);
    Deque<String> elementStack = new LinkedList<>();
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        String elementName = event.asStartElement().getName().getLocalPart();
        if (elementStack.isEmpty() && !"BugCollection".equals(elementName)) {
          throw new IOException("Unexpected document root '" + elementName + "' instead of 'BugCollection'.");
        }
        elementStack.add(elementName);
        onStartElement(xpath(elementStack), event.asStartElement());
      } else if (event.isEndElement()) {
        onEndElement(xpath(elementStack));
        elementStack.removeLast();
      } else if (event.isCharacters() && textBuilder != null) {
        textBuilder.append(event.asCharacters().getData());
      }
    }
  }

  private static String xpath(Deque<String> elementStack) {
    return String.join("/", elementStack);
  }

  private void onStartElement(String xpath, StartElement element) {
    switch (xpath) {
      case "BugCollection/BugInstance":
        bugInstanceType = getAttributeValue(element, TYPE_ATTRIBUTE);
        bugInstanceLongMessage = "";
        sourceLinePath = "";
        sourceLineStart = "";
        break;
      case "BugCollection/BugInstance/SourceLine":
        sourceLinePath = getAttributeValue(element, SOURCE_PATH_ATTRIBUTE);
        sourceLineStart = getAttributeValue(element, START_ATTRIBUTE);
        break;
      case "BugCollection/BugInstance/LongMessage":
      case "BugCollection/Project/SrcDir":
        textBuilder = new StringBuilder();
        break;
      default:
        // ignore
        break;
    }
  }

  private void onEndElement(String xpath) {
    switch (xpath) {
      case "BugCollection/BugInstance":
        consumeBugInstance();
        break;
      case "BugCollection/BugInstance/LongMessage":
        if(textBuilder != null) {
          bugInstanceLongMessage = textBuilder.toString();
          textBuilder = null;
        }
        break;
      case "BugCollection/Project/SrcDir":
        if(textBuilder != null) {
          sourceDirs.add(textBuilder.toString());
          textBuilder = null;
        }
        break;
      default:
        // ignore
        break;
    }
  }

  private void consumeBugInstance() {
    if (sourceDirs.isEmpty()) {
      LOG.debug("Unexpected missing 'BugCollection/Project/SrcDir/text()'.");
      return;
    }
    if (bugInstanceType.isEmpty()) {
      LOG.debug("Unexpected empty 'BugCollection/BugInstance/@type'.");
      return;
    }
    if (sourceLinePath.isEmpty()) {
      LOG.debug("Unexpected empty 'BugCollection/BugInstance/SourceLine/@sourcepath' for bug '{}'.", bugInstanceType);
      return;
    }
    if (bugInstanceLongMessage.isEmpty()) {
      LOG.debug("Unexpected empty 'BugCollection/BugInstance/LongMessage/text()' for bug '{}'", bugInstanceType);
      return;
    }
    InputFile inputFile = findInputFile(context, sourceDirs, sourceLinePath);
    if (inputFile == null) {
      LOG.warn("No input file found for '{}'. No SpotBugs issues will be imported on this file.", sourceLinePath);
      return;
    }

    String engineId = SpotBugsSensor.SPOTBUGS_KEY;
    ExternalRuleLoader ruleLoader = defaultRuleLoader;
    for (Map.Entry<String, ExternalRuleLoader> otherLoader : otherLoaders.entrySet()) {
      if (otherLoader.getValue().ruleKeys().contains(bugInstanceType)) {
        ruleLoader = otherLoader.getValue();
        engineId = otherLoader.getKey();
      }
    }
    ExternalIssueUtils.saveIssue(context, ruleLoader, inputFile, engineId, bugInstanceType, sourceLineStart, bugInstanceLongMessage);
  }

  private static String getAttributeValue(StartElement element, QName attributeName) {
    Attribute attribute = element.getAttributeByName(attributeName);
    return attribute != null ? attribute.getValue() : "";
  }

  private static InputFile findInputFile(SensorContext context, List<String> sourceDirs, String relativeLinuxPath) {
    FilePredicates predicates = context.fileSystem().predicates();
    InputFile inputFile = null;
    for (String sourceDir : sourceDirs) {
      File sourceFile = new File(sourceDir, relativeLinuxPath);
      inputFile = context.fileSystem().inputFile(predicates.hasPath(sourceFile.toString()));
      if (inputFile != null) {
        break;
      }
    }
    return inputFile;
  }

}
