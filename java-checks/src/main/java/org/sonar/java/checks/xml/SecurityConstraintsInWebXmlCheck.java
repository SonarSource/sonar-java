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
package org.sonar.java.checks.xml;

import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.java.xml.XPathXmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.xml.xpath.XPathExpression;

import java.io.File;

@Rule(
  key = "S3369",
  name = "Security constraints should be defined",
  priority = Priority.CRITICAL,
  tags = {Tag.CWE, Tag.JEE, Tag.OWASP_A7, Tag.SECURITY, Tag.WEBSPHERE})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SECURITY_FEATURES)
@SqaleConstantRemediation("3h")
public class SecurityConstraintsInWebXmlCheck extends XPathXmlCheck {

  private XPathExpression securityConstraintExpression;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    this.securityConstraintExpression = context.compile("web-app/security-constraint");
  }

  @Override
  public void scanFileWithXPathExpressions(XmlCheckContext context) {
    if (isWebXmlFile(context.getFile()) && hasNoSecurityConstraint(context)) {
      reportIssueOnFile("Add \"security-constraint\" elements to this descriptor.");
    }
  }

  private boolean hasNoSecurityConstraint(XmlCheckContext context) {
    return Iterables.isEmpty(context.evaluateOnDocument(securityConstraintExpression));
  }

  private static boolean isWebXmlFile(File file) {
    return "web.xml".equalsIgnoreCase(file.getName());
  }
}
