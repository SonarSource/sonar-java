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
package org.sonar.java.checks.xml.ejb;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.java.xml.XPathInitializerXmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

@Rule(
  key = "S3281",
  name = "Default EJB interceptors should be declared in \"ejb-jar.xml\"",
  priority = Priority.MAJOR,
  tags = {Tag.BUG})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class DefaultInterceptorsLocationCheck extends XPathInitializerXmlCheck {

  private XPathExpression defaultInterceptorBindingsExpression;
  private XPathExpression interceptorClassesExpression;

  @Override
  public void initXPathExpressions(XmlCheckContext context) throws XPathExpressionException {
    defaultInterceptorBindingsExpression = context.compile("ejb-jar/assembly-descriptor/interceptor-binding[ejb-name=\"*\"]");
    interceptorClassesExpression = context.compile("interceptor-class");
  }

  @Override
  public void scanFileWithExpressions(XmlCheckContext context) throws XPathExpressionException {
    if (!"ejb-jar.xml".equalsIgnoreCase(context.getFile().getName())) {
      for (Node interceptorBinding : context.evaluateOnFile(defaultInterceptorBindingsExpression)) {
        for (Node interceptorClass : context.evaluate(interceptorClassesExpression, interceptorBinding)) {
          context.reportIssue(this, interceptorClass, "Move this default interceptor to \"ejb-jar.xml\"");
        }
      }
    }
  }
}
