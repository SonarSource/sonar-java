/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonarsource.analyzer.commons.xml.SafeStaxParserFactory;

class CheckstyleXmlReportReader {

  private static final Logger LOG = LoggerFactory.getLogger(CheckstyleXmlReportReader.class);

  private static final QName CHECKSTYLE = new QName("checkstyle");
  private static final QName FILE = new QName("file");
  private static final QName ERROR = new QName("error");
  private static final QName NAME = new QName("name");
  private static final QName SOURCE = new QName("source");
  private static final QName LINE = new QName("line");
  private static final QName MESSAGE = new QName("message");

  private static final String CHECKSTYLE_PREFIX = "com.puppycrawl.tools.checkstyle.checks.";

  private final SensorContext context;
  private final IssueConsumer consumer;

  private int level = 0;

  @Nullable
  private InputFile inputFile = null;

  @FunctionalInterface
  interface IssueConsumer {
    void onError(SensorContext context, InputFile inputFile, String key, String line, String message);
  }

  private CheckstyleXmlReportReader(SensorContext context, IssueConsumer consumer) {
    this.context = context;
    this.consumer = consumer;
  }

  static void read(SensorContext context, InputStream in, IssueConsumer consumer) throws XMLStreamException, IOException {
    new CheckstyleXmlReportReader(context, consumer).read(in);
  }

  private void read(InputStream in) throws XMLStreamException, IOException {
    XMLEventReader reader = SafeStaxParserFactory.createXMLInputFactory().createXMLEventReader(in);

    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        level++;
        onElement(event.asStartElement());
      } else if (event.isEndElement()) {
        level--;
      }
    }
  }

  private void onElement(StartElement element) throws IOException {
    if (level == 1 && !CHECKSTYLE.equals(element.getName())) {
      throw new IOException("Unexpected document root '" + element.getName().getLocalPart() + "' instead of 'checkstyle'.");
    } else if (level == 2 && FILE.equals(element.getName())) {
      onFileElement(element);
    } else if (level == 3 && ERROR.equals(element.getName())) {
      onErrorElement(element);
    }
  }

  private void onFileElement(StartElement element) {
    String filePath = getAttributeValue(element, NAME);
    if (filePath.isEmpty()) {
      inputFile = null;
      return;
    }
    FilePredicates predicates = context.fileSystem().predicates();
    inputFile = context.fileSystem().inputFile(predicates.hasPath(filePath));
    if (inputFile == null) {
      LOG.warn("No input file found for '{}'. No checkstyle issues will be imported on this file.", filePath);
    }
  }

  private void onErrorElement(StartElement element) {
    if (inputFile == null) {
      return;
    }
    String source = getAttributeValue(element, SOURCE);
    String line = getAttributeValue(element, LINE);
    String message = getAttributeValue(element, MESSAGE);
    if (!source.startsWith(CHECKSTYLE_PREFIX)) {
      LOG.debug("Unexpected rule key without '{}' prefix: '{}'", CHECKSTYLE_PREFIX, source);
      return;
    }
    if (message.isEmpty()) {
      LOG.debug("Unexpected error without message for rule: '{}'", source);
      return;
    }
    String key = source.substring(CHECKSTYLE_PREFIX.length());
    consumer.onError(context, inputFile, key, line, message);
  }

  private static String getAttributeValue(StartElement element, QName attributeName) {
    Attribute attribute = element.getAttributeByName(attributeName);
    return attribute != null ? attribute.getValue() : "";
  }

}
