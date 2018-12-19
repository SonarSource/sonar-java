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

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.sonar.check.Rule;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.sonar.java.checks.xml.AbstractXPathBasedCheck;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonar.java.AnalysisException;

@Rule(key = "S3822")
public class DatabaseSchemaUpdateCheck extends AbstractXPathBasedCheck  {

  private XPathExpression hibernateHbm2ddlAutoProperty = getXPathExpression("//property[@name='hibernate.hbm2ddl.auto']");

  @Override
  protected void scanFile(XmlFile file) {
    try {
      NodeList nodeList = (NodeList) hibernateHbm2ddlAutoProperty.evaluate(file.getNamespaceUnawareDocument(), XPathConstants.NODESET);
      for (int i = 0; i < nodeList.getLength(); i++) {
        checkProperty(nodeList.item(i));
      }
    } catch (XPathExpressionException e) {
      throw new AnalysisException("Unable to evaluate XPath expression", e);
    }
  }

  private void checkProperty(Node property) {
    NodeList children = property.getChildNodes();
    if (children.getLength() == 1) {
      String value = children.item(0).getNodeValue().trim();
      if (!"none".equals(value) && !"validate".equals(value)) {
        reportIssue(property, "Use \"validate\" or remove this property.");
      }
    }
  }
}
