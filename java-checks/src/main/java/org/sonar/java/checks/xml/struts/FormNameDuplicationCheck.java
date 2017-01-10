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
package org.sonar.java.checks.xml.struts;

import com.google.common.collect.Lists;
import org.sonar.check.Rule;
import org.sonar.java.xml.XPathXmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.sonar.java.xml.XmlCheckUtils;
import org.w3c.dom.Node;

import javax.annotation.CheckForNull;
import javax.xml.xpath.XPathExpression;
import java.util.HashMap;
import java.util.Map;

@Rule(key = "S3374")
public class FormNameDuplicationCheck extends XPathXmlCheck {

  private XPathExpression formsetsExpression;
  private XPathExpression formsExpression;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    this.formsetsExpression = context.compile("form-validation/formset");
    this.formsExpression = context.compile("form");
  }

  @Override
  public void scanFileWithXPathExpressions(XmlCheckContext context) {
    for (Node formset : context.evaluateOnDocument(formsetsExpression)) {
      Map<String, Node> formsByName = new HashMap<>();
      for (Node form : context.evaluate(formsExpression, formset)) {
        String name = getNameAttribute(form);
        Node original = formsByName.get(name);
        if (original == null) {
          formsByName.put(name, form);
        } else {
          reportIssue(context, form, original);
        }
      }
    }
  }

  private void reportIssue(XmlCheckContext context, Node form, Node original) {
    String msg = "Rename this form; line " + XmlCheckUtils.nodeLine(original) + " holds another form declaration with the same name.";
    XmlCheckContext.XmlDocumentLocation secondary = new XmlCheckContext.XmlDocumentLocation("original", original);
    context.reportIssue(this, form, msg, Lists.newArrayList(secondary));
  }

  @CheckForNull
  private static String getNameAttribute(Node form) {
    Node name = XmlCheckUtils.nodeAttribute(form, "name");
    if (name != null) {
      return name.getNodeValue();
    }
    // node has no "name" attribute
    return null;
  }
}
