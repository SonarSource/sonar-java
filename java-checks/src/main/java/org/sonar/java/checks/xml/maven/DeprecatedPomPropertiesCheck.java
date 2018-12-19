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
package org.sonar.java.checks.xml.maven;

import javax.xml.xpath.XPathExpression;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.xml.AbstractXPathBasedCheck;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Rule(key = "S3421")
public class DeprecatedPomPropertiesCheck extends AbstractXPathBasedCheck {
  private static final String POM_PROPERTY_PREFIX = "${pom.";
  private static final String POM_PROPERTY_SUFFIX = "}";
  private XPathExpression textsExpression = getXPathExpression("//*[text()]");

  @Override
  protected void scanFile(XmlFile file) {
    if (!"pom.xml".equalsIgnoreCase(file.getInputFile().filename())) {
      return;
    }
    NodeList textNodes = evaluate(textsExpression, file.getDocument());
    for (int i = 0; i < textNodes.getLength(); i++) {
      checkText(textNodes.item(i));
    }
  }

  private void checkText(Node textNode) {
    NodeList childNodes = textNode.getChildNodes();
    for (int j = 0; j < childNodes.getLength(); j++) {
      Node childNode = childNodes.item(j);
      if (childNode.getNodeType() != Node.TEXT_NODE) {
        continue;
      }
      String text = childNode.getNodeValue();
      while (StringUtils.contains(text, POM_PROPERTY_PREFIX)) {
        String property = extractPropertyName(text);
        reportIssue(childNode, "Replace \"pom." + property + "\" with \"project." + property + "\".");
        text = skipFirstProperty(text);
      }
    }
  }

  private static String skipFirstProperty(String text) {
    return text.substring(text.indexOf(POM_PROPERTY_SUFFIX, text.indexOf(POM_PROPERTY_PREFIX)));
  }

  private static String extractPropertyName(String text) {
    String property = text.substring(text.indexOf(POM_PROPERTY_PREFIX) + POM_PROPERTY_PREFIX.length());
    return property.substring(0, property.indexOf(POM_PROPERTY_SUFFIX));
  }

}
