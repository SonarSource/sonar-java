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
package org.sonar.java.xml;

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlParserTest {

  private Document doc;
  private NamedNodeMap attributes;

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void setup() {
    doc = XmlParser.parseXML(new File("src/test/files/xml/parsing.xml"));
  }

  @Test
  public void do_not_perform_any_xml_validation() {
    assertThat(XmlParser.parseXML(new File("src/test/files/xml/noValidation.xml"))).isNotNull();
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  public void empty_xml_file_should_produce_a_nice_warning_log() throws Exception {
    XmlParser.parseXML(new File("src/test/files/xml/empty.xml"));
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
    String filename = FilenameUtils.separatorsToSystem("src/test/files/xml/empty.xml");
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("File " + filename + " is empty and won't be analyzed.");
  }

  @Test
  public void should_return_null_when_encountering_parsing_issue() {
    logTester.setLevel(LoggerLevel.DEBUG);
    assertThat(XmlParser.parseXML(new File("src/test/files/xml/parsing-issue.xml"))).isNull();
    String filename = FilenameUtils.separatorsToSystem("src/test/files/xml/parsing-issue.xml");
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Unable to parse xml file: " + filename);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).hasSize(1).allSatisfy(s -> s.startsWith("XML file parsing failed because of : org.xml.sax.SAXParseException; systemId: file: "));
  }

  @Test
  public void should_not_log_debug_info_if_debug_is_disabled() {
    logTester.setLevel(LoggerLevel.INFO);
    assertThat(XmlParser.parseXML(new File("src/test/files/xml/parsing-issue.xml"))).isNull();
    String filename = FilenameUtils.separatorsToSystem("src/test/files/xml/parsing-issue.xml");
    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("Unable to parse xml file: " + filename);
    assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  @Test
  public void should_not_fail_when_xml_file_uses_external_dtd() {
    assertThat(XmlParser.parseXML(new File("src/test/files/xml/parsing-with-dtd.xml"))).isNotNull();
  }

  @Test
  public void should_have_correct_locations() {
    assertPositionsMatch("assembly-descriptor", 1, 22, 21, 23);
    assertPositionsMatch("interceptor-binding", 2, 24, 12, 25);
    assertPositionsMatch("ejb-name", 6, 15, 6, 41);
    assertPositionsMatch("exclude-default-interceptors", 7, 35, 7, 70);
    assertPositionsMatch("exclude-class-interceptors", 8, 33, 8, 66);
    assertPositionsMatch("method", 9, 13, 11, 14);
    assertPositionsMatch("method-name", 10, 20, 10, 44);
  }

  @Test
  public void should_get_all_nodes() {
    assertNumberChildren("assembly-descriptor", 3);
    assertNumberChildren("interceptor-binding", 4);
    assertNumberChildren("ejb-name", 1);
    assertNumberChildren("test", 1);
    assertNumberChildren("test2", 3);
  }

  @Test
  public void should_get_all_attributes() {
    NodeList elements = doc.getElementsByTagName("test");
    Node attribute = elements.item(0).getAttributes().getNamedItem("my-attribute");
    assertThat(attribute).isNotNull();
    assertThat(attribute.getNodeValue()).isEqualTo("value");
  }

  private void assertNumberChildren(String tagName, int numberChildren) {
    NodeList elements = doc.getElementsByTagName(tagName);
    assertThat(elements.getLength()).isEqualTo(1);
    assertThat(elements.item(0).getChildNodes().getLength()).isEqualTo(numberChildren);
  }

  private void assertPositionsMatch(String tagName, int startLine, int startColumn, int endLine, int endColumn) {
    attributes = doc.getElementsByTagName(tagName).item(0).getAttributes();
    assertAttributeMatch(XmlParser.START_LINE_ATTRIBUTE, startLine);
    assertAttributeMatch(XmlParser.START_COLUMN_ATTRIBUTE, startColumn);
    assertAttributeMatch(XmlParser.END_LINE_ATTRIBUTE, endLine);
    assertAttributeMatch(XmlParser.END_COLUMN_ATTRIBUTE, endColumn);
  }

  private void assertAttributeMatch(String attribute, int value) {
    Node namedItem = attributes.getNamedItem(attribute);
    assertThat(namedItem).isNotNull();

    String actual = namedItem.getNodeValue();
    String expected = String.valueOf(value);
    String message = "'" + attribute + "' : expected '" + expected + "' but got '" + actual + "'";
    assertThat(actual).overridingErrorMessage(message).isEqualTo(expected);
  }
}
