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
import org.sonar.java.xml.XmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;

@Rule(
  key = "S3281",
  name = "Default EJB interceptors should be declared in \"ejb-jar.xml\"",
  priority = Priority.MAJOR,
  tags = {Tag.BUG})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class DefaultInterceptorsLocationCheck implements XmlCheck {

  @Override
  public void scanFile(XmlCheckContext context) {
    if (!"ejb-jar.xml".equalsIgnoreCase(context.getFile().getName())) {
      try {
        NodeList interceptorBindings = context.evaluateXPathExpression("ejb-jar/assembly-descriptor/interceptor-binding[ejb-name=\"*\"]");
        for (int i = 0; i < interceptorBindings.getLength(); i++) {
          reportOnClasses(context, interceptorBindings.item(i));
        }
      } catch (XPathExpressionException e) {
        throw new IllegalArgumentException("[S3281] Unable evaluate xpath expression for file " + context.getFile().getAbsolutePath(), e);
      }
    }
  }

  private void reportOnClasses(XmlCheckContext context, Node interceptorBinding) throws XPathExpressionException {
    NodeList classes = context.evaluateXPathExpressionFromNode(interceptorBinding, "interceptor-class");
    for (int i = 0; i < classes.getLength(); i++) {
      context.reportIssue(this, classes.item(i), "Move this default interceptor to \"ejb-jar.xml\"");
    }
  }
}
