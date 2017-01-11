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
package org.sonar.java.checks.xml.ejb;

import org.sonar.check.Rule;
import org.sonar.java.xml.XPathXmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpression;

@Rule(key = "S3282")
public class InterceptorExclusionsCheck extends XPathXmlCheck {

  private XPathExpression notDefaultInterceptorBindingsExpression;
  private XPathExpression exclusionsExpression;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    notDefaultInterceptorBindingsExpression = context.compile("ejb-jar/assembly-descriptor/interceptor-binding[ejb-name!=\"*\"]");
    exclusionsExpression = context.compile("*[self::exclude-default-interceptors[text()=\"true\"] or self::exclude-class-interceptors[text()=\"true\"]]");
  }

  @Override
  public void scanFileWithXPathExpressions(XmlCheckContext context) {
    for (Node interceptorBinding : context.evaluateOnDocument(notDefaultInterceptorBindingsExpression)) {
      checkExclusions(context, interceptorBinding);
    }
  }

  private void checkExclusions(XmlCheckContext context, Node interceptorBinding) {
    for (Node exclusion : context.evaluate(exclusionsExpression, interceptorBinding)) {
      reportIssue(exclusion, "Move this exclusion into the class as an annotation.");
    }
  }
}
