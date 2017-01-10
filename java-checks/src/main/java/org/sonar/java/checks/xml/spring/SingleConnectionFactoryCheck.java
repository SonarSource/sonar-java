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
package org.sonar.java.checks.xml.spring;

import com.google.common.collect.Iterables;
import org.sonar.check.Rule;
import org.sonar.java.xml.XPathXmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.sonar.java.xml.XmlCheckUtils;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpression;

@Rule(key = "S3438")
public class SingleConnectionFactoryCheck extends XPathXmlCheck {

  private XPathExpression singleConnectionFactoryBeansExpression;
  private XPathExpression reconnectOnExceptionPropertyValueExpression;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    singleConnectionFactoryBeansExpression = context.compile("beans/bean[@class='org.springframework.jms.connection.SingleConnectionFactory']");
    reconnectOnExceptionPropertyValueExpression = context.compile("property[@name='reconnectOnException' and value='true']");
  }

  @Override
  public void scanFileWithXPathExpressions(XmlCheckContext context) {
    for (Node bean : context.evaluateOnDocument(singleConnectionFactoryBeansExpression)) {
      if (!hasPropertyAsAttribute(bean) && !hasPropertyAsChild(bean, context)) {
        reportIssue(bean, "Add a \"reconnectOnException\" property, set to \"true\"");
      }
    }
  }

  private static boolean hasPropertyAsAttribute(Node bean) {
    Node attribute = XmlCheckUtils.nodeAttribute(bean, "p:reconnectOnException");
    return attribute != null && "true".equals(attribute.getNodeValue());
  }

  private boolean hasPropertyAsChild(Node bean, XmlCheckContext context) {
    return !Iterables.isEmpty(context.evaluate(reconnectOnExceptionPropertyValueExpression, bean));
  }

}
