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
package org.sonar.java.checks.xml.web;

import com.google.common.collect.Iterables;
import org.sonar.check.Rule;
import org.sonar.java.xml.XmlCheckContext;

import javax.xml.xpath.XPathExpression;

@Rule(key = "S3369")
public class SecurityConstraintsInWebXmlCheck extends WebXmlCheckTemplate {

  private XPathExpression securityConstraintExpression;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    this.securityConstraintExpression = context.compile(WEB_XML_ROOT + "/security-constraint");
  }

  @Override
  public void scanWebXml(XmlCheckContext context) {
    if (hasNoSecurityConstraint(context)) {
      reportIssueOnFile("Add \"security-constraint\" elements to this descriptor.");
    }
  }

  private boolean hasNoSecurityConstraint(XmlCheckContext context) {
    return Iterables.isEmpty(context.evaluateOnDocument(securityConstraintExpression));
  }
}
