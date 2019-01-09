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
package org.sonar.java.checks.xml.maven;

import java.util.Collections;
import java.util.Optional;
import javax.xml.xpath.XPathExpression;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.checks.SimpleXPathBasedCheck;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Rule(key = "S3422")
public class DependencyWithSystemScopeCheck extends SimpleXPathBasedCheck {

  private XPathExpression dependencyExpression = getXPathExpression("//dependencies/dependency");

  @Override
  public void scanFile(XmlFile xmlFile) {
    if (!"pom.xml".equalsIgnoreCase(xmlFile.getInputFile().filename())) {
      return;
    }

    evaluateAsList(dependencyExpression, xmlFile.getNamespaceUnawareDocument())
      .forEach(dependency -> checkDependency((Element) dependency));
  }

  private void checkDependency(Element dependency) {
    Optional<Node> scope = getElementByName("scope", dependency);
    if (!scope.isPresent() || !"system".equalsIgnoreCase(scope.get().getTextContent())) {
      return;
    }

    Optional<Node> systemPathOptional = getElementByName("systemPath", dependency);
    if (systemPathOptional.isPresent()) {
      reportIssue(
        XmlFile.nodeLocation(scope.get()),
        "Update this scope and remove the \"systemPath\".",
        Collections.singletonList(new Secondary(systemPathOptional.get(), "Remove this")));
    } else {
      reportIssue(scope.get(), "Update this scope.");
    }
  }

  private static Optional<Node> getElementByName(String name, Element nestingElement) {
    NodeList nodeList = nestingElement.getElementsByTagName(name);
    if (nodeList.getLength() > 0) {
      return Optional.of(nodeList.item(0));
    }

    return Optional.empty();
  }
}
