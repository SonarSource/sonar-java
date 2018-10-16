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
package org.sonar.java.checks.xml.hibernate;

import java.util.stream.StreamSupport;
import javax.xml.xpath.XPathExpression;
import org.sonar.check.Rule;
import org.sonar.java.xml.XPathXmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Rule(key = "S3822")
public class DatabaseSchemaUpdateCheck extends XPathXmlCheck {

  private XPathExpression hibernateHbm2ddlAutoProperty;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    hibernateHbm2ddlAutoProperty = context.compile("//property[@name='hibernate.hbm2ddl.auto']");
  }

  @Override
  public void scanFileWithXPathExpressions(XmlCheckContext context) {
    StreamSupport.stream(context.evaluateOnDocument(hibernateHbm2ddlAutoProperty).spliterator(), false)
      .forEach(property -> checkProperty(property, context));
  }

  private void checkProperty(Node property, XmlCheckContext context) {
    NodeList children = property.getChildNodes();
    if (children.getLength() == 1) {
      String value = children.item(0).getNodeValue().trim();
      if (!"none".equals(value) && !"validate".equals(value)) {
        context.reportIssue(this, property, "Use \"validate\" or remove this property.");
      }
    }
  }
}
