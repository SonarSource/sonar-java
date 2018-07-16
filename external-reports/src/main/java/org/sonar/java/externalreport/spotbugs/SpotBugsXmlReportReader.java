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
package org.sonar.java.externalreport.spotbugs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class SpotBugsXmlReportReader {

  private static final Logger LOG = Loggers.get(SpotBugsXmlReportReader.class);

  private static final QName TYPE_ATTRIBUTE = new QName("type");
  private static final QName SOURCE_PATH_ATTRIBUTE = new QName("sourcepath");
  private static final QName START_ATTRIBUTE = new QName("start");
  public static final String XPATH_SEPARATOR = "/";

  private final SensorContext context;
  private final IssueConsumer consumer;

  private List<String> sourceDirs = new ArrayList<>();
  private String bugInstanceType = "";
  private String bugInstanceLongMessage = "";
  private String sourceLinePath = "";
  private String sourceLineStart = "";
  private StringBuilder characterEvents = null;

  @FunctionalInterface
  interface IssueConsumer {
    void onIssue(SensorContext context, List<String> sourceDirs,
                 String bugInstanceType, String sourceLinePath, String sourceLineStart, String bugInstanceLongMessage)
      throws IOException;
  }

  private SpotBugsXmlReportReader(SensorContext context, IssueConsumer consumer) {
    this.context = context;
    this.consumer = consumer;
  }

  static void read(SensorContext context, InputStream in, IssueConsumer consumer) throws XMLStreamException, IOException {
    new SpotBugsXmlReportReader(context, consumer).read(in);
  }

  private void read(InputStream in) throws XMLStreamException, IOException {
    XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(in);
    Deque<String> elementStack = new LinkedList<>();
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        String localPart = event.asStartElement().getName().getLocalPart();
        String xpath = elementStack.isEmpty() ? localPart : (elementStack.peek() + XPATH_SEPARATOR + localPart);
        if (elementStack.isEmpty() && !"BugCollection".equals(xpath)) {
          throw new IOException("Unexpected document root '" + xpath + "' instead of 'BugCollection'.");
        }
        elementStack.push(xpath);
        onStartElement(xpath, event.asStartElement());
      } else if (event.isEndElement()) {
        onEndElement(elementStack.peek());
        elementStack.pop();
      } else if (event.isCharacters() && characterEvents != null) {
        characterEvents.append(event.asCharacters().getData());
      }
    }
  }

  private void onStartElement(String xpath, StartElement element) {
    switch (xpath) {
      case "BugCollection/BugInstance":
        bugInstanceType = getAttributeValue(element, TYPE_ATTRIBUTE);
        break;
      case "BugCollection/BugInstance/SourceLine":
        if (sourceLinePath.isEmpty()) {
          sourceLinePath = getAttributeValue(element, SOURCE_PATH_ATTRIBUTE);
          sourceLineStart = getAttributeValue(element, START_ATTRIBUTE);
        }
        break;
      case "BugCollection/BugInstance/LongMessage":
      case "BugCollection/Project/SrcDir":
        characterEvents = new StringBuilder();
        break;
      default:
        // ignore
        break;
    }
  }

  private void onEndElement(String xpath) throws IOException {
    switch (xpath) {
      case "BugCollection/BugInstance":
        consumeBugInstance();
        bugInstanceType = "";
        bugInstanceLongMessage = "";
        sourceLinePath = "";
        sourceLineStart = "";
        break;
      case "BugCollection/BugInstance/LongMessage":
        if(characterEvents != null) {
          bugInstanceLongMessage = characterEvents.toString();
          characterEvents = null;
        }
        break;
      case "BugCollection/Project/SrcDir":
        if(characterEvents != null) {
          sourceDirs.add(characterEvents.toString());
          characterEvents = null;
        }
        break;
      default:
        // ignore
        break;
    }
  }

  private void consumeBugInstance()  throws IOException {
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
    consumer.onIssue(context, sourceDirs, bugInstanceType, sourceLinePath, sourceLineStart, bugInstanceLongMessage);
  }

  private static String getAttributeValue(StartElement element, QName attributeName) {
    Attribute attribute = element.getAttributeByName(attributeName);
    return attribute != null ? attribute.getValue() : "";
  }

}
