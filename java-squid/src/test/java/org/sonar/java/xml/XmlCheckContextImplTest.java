/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.JavaCheck;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class XmlCheckContextImplTest {

  private static String reportedMessage;
  private SonarComponents sonarComponents;
  private XmlCheckContext context;
  private static final File XML_FILE = new File("src/test/files/xml/parsing.xml");
  private static final XmlCheck CHECK = new XmlCheck() {
    @Override
    public void scanFile(XmlCheckContext context) {
    }
  };
  private static final int LINE = 42;

  @Before
  public void setup() {
    reportedMessage = null;
    sonarComponents = createSonarComponentsMock();
    XPath xPath = XPathFactory.newInstance().newXPath();
    context = new XmlCheckContextImpl(XmlParser.parseXML(XML_FILE), XML_FILE, xPath, sonarComponents);
  }

  @Test
  public void can_retrieve_file_from_context() {
    assertThat(context.getFile()).isEqualTo(XML_FILE);
  }

  @Test
  public void can_retrieve_sonar_component_from_context() {
    assertThat(((XmlCheckContextImpl) context).getSonarComponents()).isEqualTo(sonarComponents);
  }

  @Test
  public void can_use_xPath() throws Exception {
    nodesMatchingXPathExpression("assembly-descriptor", 1);
    nodesMatchingXPathExpression("//interceptor-binding", 1);
    nodesMatchingXPathExpression("//test2/item", 3);
    nodesMatchingXPathExpression("//unknownNode", 0);
  }

  private void nodesMatchingXPathExpression(String expression, int expectedChildren) throws Exception {
    assertThat(context.evaluateOnFile(context.compile(expression))).hasSize(expectedChildren);
  }

  @Test
  public void can_use_xPath_from_node() throws Exception {
    nodesMatchingXPathExpressionFromNode("assembly-descriptor", "test", 1);
    nodesMatchingXPathExpressionFromNode("//interceptor-binding", "exclude-default-interceptors", 1);
    nodesMatchingXPathExpressionFromNode("assembly-descriptor", "test2/item", 3);
  }

  private void nodesMatchingXPathExpressionFromNode(String expressionOnFile, String expressionOnNode, int expectedChildren) throws Exception {
    XPathExpression xPathExprOnNode = context.compile(expressionOnNode);
    XPathExpression xPathExprOnFile = context.compile(expressionOnFile);
    assertThat(context.evaluate(xPathExprOnNode, context.evaluateOnFile(xPathExprOnFile).get(0))).hasSize(expectedChildren);
  }

  @Test
  public void should_report_issue_on_line() {
    context.reportIssue(CHECK, LINE, "message");
    assertThat(reportedMessage).isEqualTo("onLine:message");
  }

  @Test
  public void should_report_issue_on_file() {
    context.reportIssueOnFile(CHECK, "message");
    assertThat(reportedMessage).isEqualTo("onFile:message");
  }

  @Test
  public void should_report_issue_on_node() throws Exception {
    Node node = context.evaluateOnFile(context.compile("//exclude-default-interceptors")).get(0);
    int expectedLine = Integer.valueOf(node.getAttributes().getNamedItem(XmlParser.START_LINE_ATTRIBUTE).getNodeValue());

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        reportedMessage = "onNode:" + (String) invocation.getArguments()[3];
        return null;
      }
    }).when(sonarComponents).addIssue(any(File.class), eq(CHECK), eq(expectedLine), anyString(), eq((Double) null));

    context.reportIssue(CHECK, node, "message");
    assertThat(reportedMessage).isEqualTo("onNode:message");
  }

  @Test
  public void should_not_report_issue_on_unknown_node() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(context.getFile());

    Node node = ((NodeList) XPathFactory.newInstance().newXPath().compile("//exclude-default-interceptors").evaluate(doc, XPathConstants.NODESET)).item(0);

    context.reportIssue(CHECK, node, "message");
    verify(sonarComponents, never()).addIssue(any(File.class), any(JavaCheck.class), anyInt(), anyString(), anyDouble());
  }

  @Test
  public void should_not_report_issue_on_node_text_node() throws Exception {
    Node textNode = context.evaluateOnFile(context.compile("//exclude-default-interceptors")).get(0).getFirstChild();
    context.reportIssue(CHECK, textNode, "message");
    Mockito.verify(sonarComponents, never()).addIssue(any(File.class), any(JavaCheck.class), anyInt(), anyString(), anyDouble());
  }

  private static SonarComponents createSonarComponentsMock() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        reportedMessage = "onLine:" + (String) invocation.getArguments()[3];
        return null;
      }
    }).when(sonarComponents).addIssue(any(File.class), eq(CHECK), eq(LINE), anyString(), eq((Double) null));

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        reportedMessage = "onFile:" + (String) invocation.getArguments()[3];
        return null;
      }
    }).when(sonarComponents).addIssue(any(File.class), eq(CHECK), eq(-1), anyString(), eq((Double) null));

    return sonarComponents;
  }
}
