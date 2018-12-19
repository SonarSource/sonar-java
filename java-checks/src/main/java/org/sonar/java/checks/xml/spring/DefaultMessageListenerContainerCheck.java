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

import java.util.stream.IntStream;
import javax.xml.xpath.XPathExpression;
import org.sonar.check.Rule;
import org.sonar.java.checks.xml.AbstractXPathBasedCheck;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Rule(key = "S3439")
public class DefaultMessageListenerContainerCheck extends AbstractXPathBasedCheck {

  private XPathExpression defaultMessageListenerContainerBeanExpression =
    getXPathExpression("beans/bean[@class='org.springframework.jms.listener.DefaultMessageListenerContainer']");
  private XPathExpression acceptMessageWhileStoppingPropertyExpression = getXPathExpression("property[@name='acceptMessagesWhileStopping']");
  private XPathExpression sessionTransactedPropertyExpression = getXPathExpression("property[@name='sessionTransacted']");
  private XPathExpression valueExpression = getXPathExpression("value[text()='true']");


  @Override
  protected void scanFile(XmlFile xmlFile) {
    NodeList beanNodes = evaluate(defaultMessageListenerContainerBeanExpression, xmlFile.getNamespaceUnawareDocument());
    for (int i = 0; i < beanNodes.getLength(); i++) {
      Node bean = beanNodes.item(i);
      if (!hasAcceptMessagePropertyEnabled(bean) && hasSessionTransactedDisabled(bean)) {
        reportIssue(bean, "Enable \"acceptMessagesWhileStopping\".");
      }
    }
  }

  private boolean hasAcceptMessagePropertyEnabled(Node bean) {
    return hasAttributeValue(bean, "acceptMessagesWhileStopping")
      || hasPropertyAsChild(bean, acceptMessageWhileStoppingPropertyExpression);
  }

  private boolean hasSessionTransactedDisabled(Node bean) {
    return !hasAttributeValue(bean, "sessionTransacted")
      && !hasPropertyAsChild(bean, sessionTransactedPropertyExpression);
  }

  private static boolean hasAttributeValue(Node bean, String attributeName) {
    NamedNodeMap attributes = bean.getAttributes();
    return IntStream.range(0, attributes.getLength())
      .mapToObj(attributes::item)
      // ignore namespace
      .anyMatch(attribute -> attribute.getNodeName().endsWith(attributeName) && "true".equals(attribute.getNodeValue()));
  }

  private boolean hasPropertyAsChild(Node bean, XPathExpression expression) {
    NodeList propertyList = evaluate(expression, bean);
    for (int i = 0; i < propertyList.getLength(); i++) {
      Node property = propertyList.item(i);
      if (hasAttributeValue(property, "value") || evaluate(valueExpression, property).getLength() > 0) {
        return true;
      }
    }
    return false;
  }
}
