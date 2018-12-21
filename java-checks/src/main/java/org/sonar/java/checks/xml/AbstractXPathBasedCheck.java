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
package org.sonar.java.checks.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.sonar.java.AnalysisException;
import org.sonarsource.analyzer.commons.xml.checks.SonarXmlCheck;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class AbstractXPathBasedCheck extends SonarXmlCheck {

  private final XPath xpath = XPathFactory.newInstance().newXPath();

  protected XPathExpression getXPathExpression(String expression){
    try {
      return xpath.compile(expression);
    } catch (XPathExpressionException e) {
      throw new IllegalStateException("Failed to compile XPath expression " + expression, e);
    }
  }

  protected static NodeList evaluate(XPathExpression expression, Node node) {
    try {
      return (NodeList) expression.evaluate(node, XPathConstants.NODESET);
    } catch (XPathExpressionException e) {
      throw new AnalysisException("Unable to evaluate XPath expression", e);
    }
  }

  protected static List<Node> evaluateAsList(XPathExpression expression, Node node) {
    return asList(evaluate(expression, node));
  }

  protected static List<Node> asList(NodeList nodeList) {
    int numberResults = nodeList.getLength();
    if (numberResults == 0) {
      return Collections.emptyList();
    }
    List<Node> result = new ArrayList<>(numberResults);
    for (int i = 0; i < numberResults; i++) {
      result.add(nodeList.item(i));
    }
    return result;
  }

  @CheckForNull
  protected static Node nodeAttribute(Node node, String attribute) {
    NamedNodeMap attributes = node.getAttributes();
    if (attributes == null) {
      return null;
    }
    return attributes.getNamedItem(attribute);
  }
}
