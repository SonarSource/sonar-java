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
package org.sonar.java.xml;

import org.w3c.dom.Node;

public abstract class XPathXmlCheck implements XmlCheck {

  private boolean initialized = false;
  private XmlCheckContext context;

  @Override
  public void scanFile(XmlCheckContext context) {
    this.context = context;
    if (!initialized) {
      precompileXPathExpressions(context);
      initialized = true;
    }
    scanFileWithXPathExpressions(context);
  }

  /**
   * Will be called only once by XmlCheck.
   * Using {@link XmlCheckContext#compile(String)}, should compile all the fixed XPath expressions which will be re-used when analyzing files.
   * @param context
   */
  public abstract void precompileXPathExpressions(XmlCheckContext context);

  /**
   * Will be called for each file.
   * @param context
   */
  public abstract void scanFileWithXPathExpressions(XmlCheckContext context);

  public void reportIssue(Node node, String message) {
    context.reportIssue(this, node, message);
  }

  public void reportIssueOnFile(String message) {
    context.reportIssueOnFile(this, message);
  }
}
