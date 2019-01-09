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
package org.sonar.java.checks.xml.web;

import java.util.Collections;
import javax.xml.xpath.XPathExpression;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.xml.XmlFile;

@Rule(key = "S3369")
public class SecurityConstraintsInWebXmlCheck extends AbstractWebXmlXPathBasedCheck {

  private XPathExpression securityConstraintExpression = getXPathExpression(WEB_XML_ROOT + "/security-constraint");

  private boolean hasNoSecurityConstraint(XmlFile file) {
    return evaluateAsList(securityConstraintExpression, file.getNamespaceUnawareDocument()).isEmpty();
  }

  @Override
  public void scanWebXml(XmlFile file) {
    if (hasNoSecurityConstraint(file)) {
      reportIssueOnFile("Add \"security-constraint\" elements to this descriptor.", Collections.emptyList());
    }
  }
}
