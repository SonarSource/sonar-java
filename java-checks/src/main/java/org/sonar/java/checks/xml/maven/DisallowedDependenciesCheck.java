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

import javax.xml.xpath.XPathExpression;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.xml.maven.helpers.MavenDependencyMatcher;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.checks.SimpleXPathBasedCheck;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Rule(key = DisallowedDependenciesCheck.KEY)
public class DisallowedDependenciesCheck extends SimpleXPathBasedCheck {

  public static final String KEY = "S3417";

  private XPathExpression dependencyExpression = getXPathExpression("//dependencies/dependency");

  @RuleProperty(
    key = "dependencyName",
    description = "Pattern describing forbidden dependencies group and artifact ids. E.G. '``*:.*log4j``' or '``x.y:*``'")
  public String dependencyName = "";

  @RuleProperty(
    key = "version",
    description = "Dependency version pattern or dash-delimited range. Leave blank for all versions. E.G. '``1.3.*``', '``1.0-3.1``', '``1.0-*``' or '``*-3.1``'")
  public String version = "";

  private MavenDependencyMatcher matcher = null;

  @Override
  public void scanFile(XmlFile xmlFile) {
    if (!"pom.xml".equalsIgnoreCase(xmlFile.getInputFile().filename())) {
      return;
    }

    evaluateAsList(dependencyExpression, xmlFile.getNamespaceUnawareDocument()).forEach(dependency -> {
      String groupId = getChildElementText("groupId", dependency);
      String artifactId = getChildElementText("artifactId", dependency);
      String dependencyVersion = getChildElementText("version", dependency);
      if (getMatcher().matches(groupId, artifactId, dependencyVersion)) {
        reportIssue(dependency, "Remove this forbidden dependency.");
      }
    });
  }

  private static String getChildElementText(String childElementName, Node parent) {
    for (Node node : XmlFile.children(parent)) {
      if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals(childElementName)) {
        return node.getTextContent();
      }
    }

    return "";
  }

  private MavenDependencyMatcher getMatcher() {
    if (matcher == null) {
      try {
        matcher = new MavenDependencyMatcher(dependencyName, version);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("[" + KEY + "] Unable to build matchers from provided dependency name: " + dependencyName, e);
      }
    }
    return matcher;
  }
}
