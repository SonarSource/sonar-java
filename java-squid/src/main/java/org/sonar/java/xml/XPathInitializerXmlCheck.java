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
package org.sonar.java.xml;

import org.sonar.squidbridge.api.AnalysisException;

import javax.xml.xpath.XPathExpressionException;

public abstract class XPathInitializerXmlCheck implements XmlCheck {

  private boolean initialized = false;

  @Override
  public void scanFile(XmlCheckContext context) {
    try {
      if (!initialized) {
        initXPathExpressions(context);
        initialized = true;
      }
      scanFileWithExpressions(context);
    } catch (XPathExpressionException e) {
      throw new AnalysisException("Unable perform analysis on file " + context.getFile().getAbsolutePath(), e);
    }
  }

  /**
   * Will be called only once by XmlCheck.
   * Using {@link XmlCheckContext#compile(String)}, should compile all the XPath expressions which will be re-used for all the analyzed files.
   * @param context
   * @throws XPathExpressionException
   */
  public abstract void initXPathExpressions(XmlCheckContext context) throws XPathExpressionException;

  /**
   * Will be called for each file.
   * @param context
   * @throws XPathExpressionException
   */
  public abstract void scanFileWithExpressions(XmlCheckContext context) throws XPathExpressionException;

}
