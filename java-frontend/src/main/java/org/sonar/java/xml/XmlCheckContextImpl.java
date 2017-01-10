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

import com.google.common.collect.ImmutableList;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.AnalyzerMessage.TextSpan;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.api.AnalysisException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.sonar.java.xml.XmlCheckUtils.nodeLine;

public class XmlCheckContextImpl implements XmlCheckContext {

  private final Document document;
  private final File file;
  private final SonarComponents sonarComponents;
  private final XPath xPath;

  public XmlCheckContextImpl(Document document, File file, XPath xPath, SonarComponents sonarComponents) {
    this.document = document;
    this.file = file;
    this.xPath = xPath;
    this.sonarComponents = sonarComponents;
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public XPathExpression compile(String expression) {
    try {
      return xPath.compile(expression);
    } catch (XPathExpressionException e) {
      throw new AnalysisException("Unable to compile XPath expression '" + expression + "'", e);
    }
  }

  @Override
  public Iterable<Node> evaluateOnDocument(XPathExpression expression) {
    return evaluate(expression, document);
  }

  @Override
  public Iterable<Node> evaluate(XPathExpression expression, Node node) {
    try {
      NodeList nodeList = (NodeList) expression.evaluate(node, XPathConstants.NODESET);
      if (nodeList.getLength() == 0) {
        return ImmutableList.of();
      }
      return new NodeListIterable(nodeList);
    } catch (XPathExpressionException e) {
      throw new AnalysisException("Unable to evaluate XPath expression", e);
    }
  }

  private static class NodeListIterable implements Iterable<Node> {

    private final NodeList nodeList;

    public NodeListIterable(NodeList nodeList) {
      this.nodeList = nodeList;
    }

    @Override
    public Iterator<Node> iterator() {
      return new NodeListIterator(nodeList);
    }

    private static class NodeListIterator implements Iterator<Node> {
      private final NodeList nodeList;
      private final int length;
      private int count = 0;

      public NodeListIterator(NodeList nodeList) {
        this.nodeList = nodeList;
        this.length = nodeList.getLength();
      }

      @Override
      public boolean hasNext() {
        return count < length;
      }

      @Override
      public Node next() {
        Node node = nodeList.item(count);
        if (node == null) {
          throw new NoSuchElementException();
        }
        count++;
        return node;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }
  }

  @Override
  public void reportIssueOnFile(JavaCheck check, String message) {
    reportIssue(check, -1, message);
  }

  @Override
  public void reportIssue(JavaCheck check, int line, String message) {
    sonarComponents.addIssue(file, check, line, message, null);
  }

  @Override
  public void reportIssue(JavaCheck check, Node node, String message) {
    Integer line = nodeLine(node);
    if (line != null) {
      reportIssue(check, line, message);
    }
  }

  @Override
  public void reportIssue(JavaCheck check, Node node, String message, Iterable<XmlDocumentLocation> secondary) {
    reportIssue(check, node, message, secondary, null);
  }

  @Override
  public void reportIssue(JavaCheck check, Node node, String message, Iterable<XmlDocumentLocation> secondary, @Nullable Integer cost) {
    Integer line = nodeLine(node);
    if (line != null) {
      sonarComponents.reportIssue(buildAnalyzerMessage(check, message, line, secondary, cost, getFile()));
    }
  }

  public static AnalyzerMessage buildAnalyzerMessage(JavaCheck check, String message, Integer line, Iterable<XmlDocumentLocation> secondary, @Nullable Integer cost, File file) {
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(check, file, line, message, cost != null ? cost.intValue() : 0);
    for (XmlDocumentLocation location : secondary) {
      AnalyzerMessage secondaryLocation = getSecondaryAnalyzerMessage(check, file, location);
      if (secondaryLocation != null) {
        analyzerMessage.flows.add(Collections.singletonList(secondaryLocation));
      }
    }
    return analyzerMessage;
  }

  @CheckForNull
  private static AnalyzerMessage getSecondaryAnalyzerMessage(JavaCheck check, File file, XmlDocumentLocation location) {
    Integer startLine = nodeLine(location.node);
    if (startLine == null) {
      return null;
    }
    TextSpan ts = new TextSpan(startLine, 0, startLine, 0);
    return new AnalyzerMessage(check, file, ts, location.msg, 0);
  }

  public SonarComponents getSonarComponents() {
    return sonarComponents;
  }
}
