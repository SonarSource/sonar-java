/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.List;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathExpression;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import org.sonarsource.analyzer.commons.xml.checks.SimpleXPathBasedCheck;
import org.w3c.dom.Node;

@Rule(key = "S3373")
public class ActionNumberCheck extends SimpleXPathBasedCheck {

  private static final int DEFAULT_MAXIMUM_NUMBER_FORWARDS = 4;

  @RuleProperty(
    key = "threshold",
    description = "Maximum allowed number of ``<forward/>`` mappings in an ``<action>``",
    defaultValue = "" + DEFAULT_MAXIMUM_NUMBER_FORWARDS)
  public int maximumForwards = DEFAULT_MAXIMUM_NUMBER_FORWARDS;

  private XPathExpression actionsExpression = getXPathExpression("struts-config/action-mappings/action");
  private XPathExpression forwardsFromActionExpression = getXPathExpression("forward");

  @Override
  public void scanFile(XmlFile xmlFile) {
    evaluateAsList(actionsExpression, xmlFile.getNamespaceUnawareDocument())
      .forEach(this::checkAction);
  }

  private void checkAction(Node node) {
    List<Secondary> secondaries = evaluateAsList(forwardsFromActionExpression, node).stream()
      .skip(maximumForwards)
      .map(forward -> new Secondary(forward, "Move this forward to another action."))
      .collect(Collectors.toList());
    if (!secondaries.isEmpty()) {
      int numberForward = maximumForwards + secondaries.size();
      String message = "Reduce the number of forwards in this action from " + numberForward + " to at most " + maximumForwards + ".";
      reportIssue(XmlFile.nodeLocation(node), message, secondaries);
    }
  }

}
