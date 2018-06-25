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
package org.sonar.java.checks.xml.spring;

import com.google.common.collect.Iterables;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import javax.xml.xpath.XPathExpression;
import org.sonar.check.Rule;
import org.sonar.java.xml.XPathXmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

@Rule(key = "S3439")
public class DefaultMessageListenerContainerCheck extends XPathXmlCheck {

  private XPathExpression defaultMessageListenerContainerBeanExpression;
  private XPathExpression acceptMessageWhileStoppingPropertyExpression;
  private XPathExpression sessionTransactedPropertyExpression;
  private XPathExpression valueExpression;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    defaultMessageListenerContainerBeanExpression = context.compile("beans/bean[@class='org.springframework.jms.listener.DefaultMessageListenerContainer']");
    acceptMessageWhileStoppingPropertyExpression = context.compile("property[@name='acceptMessagesWhileStopping']");
    sessionTransactedPropertyExpression = context.compile("property[@name='sessionTransacted']");
    valueExpression = context.compile("value[text()='true']");
  }

  @Override
  public void scanFileWithXPathExpressions(XmlCheckContext context) {
    StreamSupport.stream(context.evaluateOnDocument(defaultMessageListenerContainerBeanExpression).spliterator(), false)
      .filter(bean -> !hasAcceptMessagePropertyEnabled(context, bean) && hasSessionTransactedDisabled(context, bean))
      .forEach(bean -> reportIssue(bean, "Enable \"acceptMessagesWhileStopping\"."));
  }

  private boolean hasAcceptMessagePropertyEnabled(XmlCheckContext context, Node bean) {
    return hasAttributeValue(bean, "acceptMessagesWhileStopping")
      || hasPropertyAsChild(context, bean, acceptMessageWhileStoppingPropertyExpression);
  }

  private boolean hasSessionTransactedDisabled(XmlCheckContext context, Node bean) {
    return !hasAttributeValue(bean, "sessionTransacted")
      && !hasPropertyAsChild(context, bean, sessionTransactedPropertyExpression);
  }

  private static boolean hasAttributeValue(Node bean, String attributeName) {
    NamedNodeMap attributes = bean.getAttributes();
    return IntStream.range(0, attributes.getLength())
      .mapToObj(attributes::item)
      // ignore namespace
      .anyMatch(attribute -> attribute.getNodeName().endsWith(attributeName) && "true".equals(attribute.getNodeValue()));
  }

  private boolean hasPropertyAsChild(XmlCheckContext context, Node bean, XPathExpression expression) {
    return StreamSupport.stream(context.evaluate(expression, bean).spliterator(), false)
      .anyMatch(property -> hasAttributeValue(property, "value") || !Iterables.isEmpty(context.evaluate(valueExpression, property)));
  }
}
