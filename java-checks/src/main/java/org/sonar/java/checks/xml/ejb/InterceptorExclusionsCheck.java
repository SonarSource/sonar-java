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
import org.sonar.java.xml.XPathInitializedXmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

@Rule(
  key = "S3282",
  name = "EJB interceptor exclusions should be declared as annotations",
  priority = Priority.MAJOR,
  tags = {Tag.PITFALL})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("15min")
public class InterceptorExclusionsCheck extends XPathInitializedXmlCheck {

  private XPathExpression notDefaultInterceptorBindingsExpression;
  private XPathExpression exclusionsExpression;

  @Override
  public void initXPathExpressions(XmlCheckContext context) throws XPathExpressionException {
    notDefaultInterceptorBindingsExpression = context.compile("ejb-jar/assembly-descriptor/interceptor-binding[ejb-name!=\"*\"]");
    exclusionsExpression = context.compile("*[self::exclude-default-interceptors[text()=\"true\"] or self::exclude-class-interceptors[text()=\"true\"]]");
  }

  @Override
  public void scanFileWithXPathExpressions(XmlCheckContext context) throws XPathExpressionException {
    for (Node interceptorBinding : context.evaluateOnFile(notDefaultInterceptorBindingsExpression)) {
      checkExclusions(context, interceptorBinding);
    }
  }

  private void checkExclusions(XmlCheckContext context, Node interceptorBinding) throws XPathExpressionException {
    for (Node exclusion : context.evaluate(exclusionsExpression, interceptorBinding)) {
      reportIssue(exclusion, "Move this exclusion into the class as an annotation.");
    }
  }
}
