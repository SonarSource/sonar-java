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

@Rule(key = "S3281")
public class DefaultInterceptorsLocationCheck extends XPathXmlCheck {

  private XPathExpression defaultInterceptorClassesExpression;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    defaultInterceptorClassesExpression = context.compile("ejb-jar/assembly-descriptor/interceptor-binding[ejb-name=\"*\"]/interceptor-class");
  }

  @Override
  public void scanFileWithXPathExpressions(XmlCheckContext context) {
    if (!"ejb-jar.xml".equalsIgnoreCase(context.getFile().getName())) {
      for (Node interceptorClass : context.evaluateOnDocument(defaultInterceptorClassesExpression)) {
        reportIssue(interceptorClass, "Move this default interceptor to \"ejb-jar.xml\"");
      }
    }
  }
}
