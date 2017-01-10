/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlCheckUtilsTest {

  private static final String XML_DOCUMENT = "src/test/files/xml/XmlCheckUtils.xml";
  private static Document doc;
  private static XPathExpression expression;

  @BeforeClass
  public static void setup() throws Exception {
    doc = XmlParser.parseXML(new File(XML_DOCUMENT));
    expression = XPathFactory.newInstance().newXPath().compile("my-xml/test");
  }

  @Test
  public void private_constructor() throws Exception {
    assertThat(Modifier.isFinal(XmlCheckUtils.class.getModifiers())).isTrue();
    Constructor constructor = XmlCheckUtils.class.getDeclaredConstructor();
    assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void unknown_attribute_can_not_be_found() throws Exception {
    Node node = evaluate(doc);
    // the node has no attribute
    assertThat(XmlCheckUtils.nodeAttribute(node, "unknown")).isNull();
    // the child of node is a text node, without attribute
    assertThat(XmlCheckUtils.nodeAttribute(node.getFirstChild(), "unknown")).isNull();
  }

  @Test
  public void existing_attribute_can_be_retrieved() throws Exception {
    Node node = evaluate(doc);
    Node nodeAttribute = XmlCheckUtils.nodeAttribute(node, "my-attribute");
    assertThat(nodeAttribute).isNotNull();
    assertThat(nodeAttribute.getNodeValue()).isEqualTo("value");
  }

  @Test
  public void can_get_node_line_if_parsed_correctly() throws Exception {
    Node node = evaluate(doc);
    assertThat(XmlCheckUtils.nodeLine(node)).isEqualTo(2);
  }

  @Test
  public void fail_to_retrieve_line_on_document_parsed_differently() throws Exception {
    // own parsing
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(XML_DOCUMENT));
    Node node = evaluate(doc);
    assertThat(XmlCheckUtils.nodeLine(node)).isNull();
  }

  private static Node evaluate(Document doc) throws XPathExpressionException {
    return (Node) expression.evaluate(doc, XPathConstants.NODE);
  }

}
