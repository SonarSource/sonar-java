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
package org.sonar.java.checks.xml.maven;

import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.java.xml.XPathXmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpression;

@Rule(key = "S3421")
public class DeprecatedPomPropertiesCheck extends XPathXmlCheck {
  private static final String POM_PROPERTY_PREFIX = "${pom.";
  private static final String POM_PROPERTY_SUFFIX = "}";
  private XPathExpression textsExpression;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    textsExpression = context.compile("//*[text()]");
  }

  @Override
  public void scanFileWithXPathExpressions(XmlCheckContext context) {
    if ("pom.xml".equals(context.getFile().getName())) {
      for (Node textNode : context.evaluateOnDocument(textsExpression)) {
        String text = textNode.getFirstChild().getNodeValue();
        while (StringUtils.contains(text, POM_PROPERTY_PREFIX)) {
          String property = extractPropertyName(text);
          reportIssue(textNode, "Replace \"pom." + property + "\" with \"project." + property + "\".");
          text = skipFirstProperty(text);
        }
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
