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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.AnalysisException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
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
  private XPath xPath;
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
    xPath = XPathFactory.newInstance().newXPath();
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
    assertThat(context.evaluateOnDocument(context.compile(expression))).hasSize(expectedChildren);
  }

  @Test
  public void can_use_xPath_from_node() throws Exception {
    nodesMatchingXPathExpressionFromNode("assembly-descriptor", "test", 1);
    nodesMatchingXPathExpressionFromNode("//interceptor-binding", "exclude-default-interceptors", 1);
    nodesMatchingXPathExpressionFromNode("assembly-descriptor", "test2/item", 3);
  }

  private void nodesMatchingXPathExpressionFromNode(String expressionOnDocument, String expressionOnNode, int expectedChildren) throws Exception {
    XPathExpression xPathExprOnNode = context.compile(expressionOnNode);
    assertThat(context.evaluate(xPathExprOnNode, firstNode(context, expressionOnDocument))).hasSize(expectedChildren);
  }

  private static Node firstNode(XmlCheckContext context, String expression) throws XPathExpressionException {
    Node result = null;
    for (Node node : context.evaluateOnDocument(context.compile(expression))) {
      result = node;
      break;
    }
    return result;
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_fail_when_trying_to_remove_nodes() throws Exception {
    Iterable<Node> items = context.evaluateOnDocument(context.compile("//test2/item"));
    items.iterator().remove();
  }

  @Test(expected = NoSuchElementException.class)
  public void should_fail_when_trying_to_access_more_items() throws Exception {
    Iterable<Node> items = context.evaluateOnDocument(context.compile("//test2/item"));
    Iterator<Node> iterator = items.iterator();
    for (int i = 0; i < 5; i++) {
      iterator.next();
    }
  }

  @Test(expected = AnalysisException.class)
  public void should_fail_when_compiling_bad_XPathExpression() throws Exception {
    context.compile("");
  }

  @Test(expected = AnalysisException.class)
  public void should_fail_when_unable_to_evaluate_XPathExpression() throws Exception {
    context = new XmlCheckContextImpl(null, new File("."), xPath, sonarComponents);
    context.evaluateOnDocument(context.compile("tag"));
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
    Node node = firstNode(context, "//exclude-default-interceptors");
    int expectedLine = XmlCheckUtils.nodeLine(node);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        reportedMessage = "onNode:" + (String) invocation.getArguments()[3];
        return null;
      }
    }).when(sonarComponents).addIssue(any(File.class), eq(CHECK), eq(expectedLine), anyString(), eq((Integer) null));

    context.reportIssue(CHECK, node, "message");
    assertThat(reportedMessage).isEqualTo("onNode:message");
  }

  @Test
  public void should_report_issue_on_node_with_secondary() throws Exception {
    Node node = firstNode(context, "//test2");
    int nodeLine = XmlCheckUtils.nodeLine(node);
    Node childNode = node.getFirstChild();
    int childNodeLine = XmlCheckUtils.nodeLine(childNode);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        AnalyzerMessage analyzerMessage = (AnalyzerMessage) invocation.getArguments()[0];
        reportedMessage = "onNode:" + analyzerMessage.getMessage() + "(" + analyzerMessage.getLine() + ")";
        for (AnalyzerMessage secondary : analyzerMessage.flows.stream().map(l -> l.get(0)).collect(Collectors.toList())) {
          reportedMessage += ";onChild:" + secondary.getMessage() + "(" + secondary.getLine() + ")";
        }
        return null;
      }
    }).when(sonarComponents).reportIssue(any(AnalyzerMessage.class));

    context.reportIssue(CHECK, node, "message1", Lists.newArrayList(new XmlCheckContext.XmlDocumentLocation("message2", childNode)));

    String expectedMessage = "onNode:message1(" + nodeLine + ");onChild:message2(" + childNodeLine + ")";
    assertThat(reportedMessage).isEqualTo(expectedMessage);
  }

  @Test
  public void should_report_issue_on_node_with_secondary_and_cost() throws Exception {
    Node node = firstNode(context, "//test2");
    int nodeLine = XmlCheckUtils.nodeLine(node);
    Node childNode = node.getFirstChild();
    int childNodeLine = XmlCheckUtils.nodeLine(childNode);
    int cost = 42;

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        AnalyzerMessage analyzerMessage = (AnalyzerMessage) invocation.getArguments()[0];
        reportedMessage = "onNode:" + analyzerMessage.getMessage() + "(" + analyzerMessage.getLine() + ")[" + analyzerMessage.getCost() + "]";
        for (AnalyzerMessage secondary : analyzerMessage.flows.stream().map(l -> l.get(0)).collect(Collectors.toList())) {
          reportedMessage += ";onChild:" + secondary.getMessage() + "(" + secondary.getLine() + ")";
        }
        return null;
      }
    }).when(sonarComponents).reportIssue(any(AnalyzerMessage.class));

    context.reportIssue(CHECK, node, "message1", Lists.newArrayList(new XmlCheckContext.XmlDocumentLocation("message2", childNode)), cost);

    String expectedMessage = "onNode:message1(" + nodeLine + ")[42.0];onChild:message2(" + childNodeLine + ")";
    assertThat(reportedMessage).isEqualTo(expectedMessage);
  }

  @Test
  public void should_not_report_issue_on_unknown_node() throws Exception {
    // manual parsing
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(context.getFile());

    // uses document with recorded lines
    Node node = Iterables.get(context.evaluate(context.compile("//exclude-default-interceptors"), doc), 0);

    context.reportIssue(CHECK, node, "message");
    verify(sonarComponents, never()).addIssue(any(File.class), any(JavaCheck.class), anyInt(), anyString(), anyInt());
  }

  @Test
  public void should_not_report_issue_on_unknown_node_with_secondaries() throws Exception {
    // manual parsing
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(context.getFile());

    // uses document with recorded lines
    Node node = Iterables.get(context.evaluate(context.compile("//exclude-default-interceptors"), doc), 0);

    context.reportIssue(CHECK, node, "message1", Lists.newArrayList(new XmlCheckContext.XmlDocumentLocation("message2", node.getFirstChild())));
    verify(sonarComponents, never()).reportIssue(any(AnalyzerMessage.class));
  }

  @Test
  public void should_not_report_unknown_secondaries() throws Exception {
    // manual parsing
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(context.getFile());
    Node node = firstNode(context, "//test2");
    int nodeLine = XmlCheckUtils.nodeLine(node);

    // uses document with recorded lines
    Node child = Iterables.get(context.evaluate(context.compile("//test2/item"), doc), 0);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        AnalyzerMessage analyzerMessage = (AnalyzerMessage) invocation.getArguments()[0];
        reportedMessage = "onNode:" + analyzerMessage.getMessage() + "(" + analyzerMessage.getLine() + ")";
        for (AnalyzerMessage secondary : analyzerMessage.flows.stream().map(l -> l.get(0)).collect(Collectors.toList())) {
          reportedMessage += ";onChild:" + secondary.getMessage() + "(" + secondary.getLine() + ")";
        }
        return null;
      }
    }).when(sonarComponents).reportIssue(any(AnalyzerMessage.class));

    context.reportIssue(CHECK, node, "message1", Lists.newArrayList(new XmlCheckContext.XmlDocumentLocation("message2", child)));

    assertThat(reportedMessage).isEqualTo("onNode:message1(" + nodeLine + ")");
  }

  @Test
  public void should_not_report_issue_on_node_text_node() throws Exception {
    Node textNode = firstNode(context, "//exclude-default-interceptors").getFirstChild();
    context.reportIssue(CHECK, textNode, "message");
    Mockito.verify(sonarComponents, never()).addIssue(any(File.class), any(JavaCheck.class), anyInt(), anyString(), anyInt());
  }

  private static SonarComponents createSonarComponentsMock() {
    SonarComponents sonarComponents = mock(SonarComponents.class);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        reportedMessage = "onLine:" + invocation.getArguments()[3];
        return null;
      }
    }).when(sonarComponents).addIssue(any(File.class), eq(CHECK), eq(LINE), anyString(), eq(null));

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        reportedMessage = "onFile:" + invocation.getArguments()[3];
        return null;
      }
    }).when(sonarComponents).addIssue(any(File.class), eq(CHECK), eq(-1), anyString(), eq(null));

    return sonarComponents;
  }
}
