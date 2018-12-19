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
package org.sonar.java.checks.xml.ejb;

import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.Node;
import org.sonar.java.checks.xml.AbstractXPathBasedCheck;
import javax.xml.xpath.XPathExpression;

@Rule(key = "S3282")
public class InterceptorExclusionsCheck extends AbstractXPathBasedCheck  {

  private XPathExpression notDefaultInterceptorBindingsExpression = getXPathExpression("ejb-jar/assembly-descriptor/interceptor-binding[ejb-name!=\"*\"]");
  private XPathExpression exclusionsExpression = getXPathExpression("*[self::exclude-default-interceptors[text()=\"true\"] or self::exclude-class-interceptors[text()=\"true\"]]");

  @Override
  protected void scanFile(XmlFile xmlFile) {
    evaluateAsList(notDefaultInterceptorBindingsExpression, xmlFile.getNamespaceUnawareDocument()).forEach(this::checkExclusions);
  }

  private void checkExclusions(Node interceptorBinding) {
    evaluateAsList(exclusionsExpression, interceptorBinding)
      .forEach(node -> reportIssue(node, "Move this exclusion into the class as an annotation."));
  }
}
