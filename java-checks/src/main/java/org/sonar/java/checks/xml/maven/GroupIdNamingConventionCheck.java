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

import java.util.regex.Pattern;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.AnalysisException;
import org.sonar.java.checks.xml.AbstractXPathBasedCheck;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.Node;

@Rule(key = GroupIdNamingConventionCheck.KEY)
public class GroupIdNamingConventionCheck extends AbstractXPathBasedCheck {

  public static final String KEY = "S3419";

  private static final String DEFAULT_REGEX = "(com|org)(\\.[a-z][a-z-0-9]*)+";

  @RuleProperty(
    key = "regex",
    description = "The regular expression the \"groupId\" should match",
    defaultValue = "" + DEFAULT_REGEX)
  public String regex = DEFAULT_REGEX;

  private XPathExpression groupIdExpression = getXPathExpression("project/groupId");
  private Pattern pattern = null;

  @Override
  protected void scanFile(XmlFile file) {
    if (!"pom.xml".equals(file.getInputFile().filename())) {
      return;
    }
    try {
      Node groupId = (Node) groupIdExpression.evaluate(file.getNamespaceUnawareDocument(), XPathConstants.NODE);
      String content = groupId.getTextContent();
      if (!getPattern().matcher(content).matches()) {
        reportIssue(groupId.getFirstChild(), "Update this \"groupId\" to match the provided regular expression: '" + regex + "'");
      }
    } catch (NullPointerException e) {
      // there is no 'groupId' in the pom, do nothing
    } catch (XPathExpressionException e) {
      throw new AnalysisException("Unable to evaluate XPath expression", e);
    }
  }

  private Pattern getPattern() {
    if (pattern == null) {
      try {
        pattern = Pattern.compile(regex, Pattern.DOTALL);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("[" + KEY + "] Unable to compile the regular expression: " + regex, e);
      }
    }
    return pattern;
  }

}
