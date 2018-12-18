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
package org.sonar.java.checks.xml.ejb;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.sonar.check.Rule;
import org.sonar.java.AnalysisException;
import org.sonar.java.checks.xml.AbstractXPathBasedCheck;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.w3c.dom.NodeList;

@Rule(key = "S3281")
public class DefaultInterceptorsLocationCheck extends AbstractXPathBasedCheck {

  private XPathExpression defaultInterceptorClassesExpression = getXPathExpression("ejb-jar/assembly-descriptor/interceptor-binding[ejb-name=\"*\"]/interceptor-class");

  @Override
  protected void scanFile(XmlFile file) {

    if (!"ejb-jar.xml".equalsIgnoreCase(file.getInputFile().filename())) {

      try {
        NodeList nodeList = (NodeList) defaultInterceptorClassesExpression.evaluate(file.getNamespaceUnawareDocument(), XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
          reportIssue(nodeList.item(i), "Move this default interceptor to \"ejb-jar.xml\"");
        }

      } catch (XPathExpressionException e) {
        throw new AnalysisException("Unable to evaluate XPath expression", e);
      }
    }
  }
}
