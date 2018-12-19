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
package org.sonar.java.checks.xml.struts;

import org.sonar.check.Rule;
import org.sonar.java.checks.xml.AbstractXPathBasedCheck;
import org.sonar.java.xml.XmlCheckUtils;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.XmlTextRange;
import org.w3c.dom.Node;
import javax.annotation.CheckForNull;
import javax.xml.xpath.XPathExpression;
import java.util.*;
import java.util.List;

@Rule(key = "S3374")
public class FormNameDuplicationCheck extends AbstractXPathBasedCheck {

  private XPathExpression formsetsExpression = getXPathExpression("form-validation/formset");
  private XPathExpression formsExpression = getXPathExpression("form");

  @Override
  protected void scanFile(XmlFile xmlFile) {
    evaluateAsList(formsetsExpression, xmlFile.getNamespaceUnawareDocument())
      .forEach(this::checkIfDuplicate);
  }

  private void checkIfDuplicate(Node formSet) {
    Map<String, Node> formsByName = new HashMap<>();
    evaluateAsList(formsExpression, formSet)
      .forEach(form -> {
        String name = getNameAttribute(form);
        Node original = formsByName.get(name);
        if (original == null) {
          formsByName.put(name, form);
        } else {
          reportIssue(XmlFile.nodeLocation(form), original);
        }
      });
  }

  private void reportIssue(XmlTextRange range, Node original) {
    String msg = "Rename this form; line " + XmlFile.nodeLocation(original).getStartLine() + " holds another form declaration with the same name.";
    List<Secondary> secondaries = Collections.singletonList(new Secondary(original, "original"));
    reportIssue(range, msg, secondaries);
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
