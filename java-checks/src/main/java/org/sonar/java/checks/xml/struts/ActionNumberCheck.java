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
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.xml.AbstractXPathBasedCheck;
import org.sonarsource.analyzer.commons.xml.XmlFile;
import javax.xml.xpath.XPathExpression;
import java.util.ArrayList;
import java.util.List;

@Rule(key = "S3373")
public class ActionNumberCheck extends AbstractXPathBasedCheck {

  private static final int DEFAULT_MAXIMUM_NUMBER_FORWARDS = 4;

  @RuleProperty(
    key = "threshold",
    description = "Maximum allowed number of ``<forward/>`` mappings in an ``<action>``",
    defaultValue = "" + DEFAULT_MAXIMUM_NUMBER_FORWARDS)
  public int maximumForwards = DEFAULT_MAXIMUM_NUMBER_FORWARDS;

  private XPathExpression actionsExpression = getXPathExpression("struts-config/action-mappings/action");
  private XPathExpression forwardsFromActionExpression = getXPathExpression("forward");

  @Override
  protected void scanFile(XmlFile xmlFile) {
    evaluateAsList(actionsExpression, xmlFile.getNamespaceUnawareDocument())
      .forEach(node -> {
        List<Secondary> secondaries = new ArrayList<>();
        evaluateAsList(forwardsFromActionExpression, node).stream().skip(maximumForwards)
          .forEach(forward -> {
            Secondary secondary = new Secondary(forward, "Move this forward to another action.");
            secondaries.add(secondary);
          });
        if (!secondaries.isEmpty()) {
          int cost = secondaries.size();
          int numberForward = maximumForwards + cost;
          String message = "Reduce the number of forwards in this action from " + numberForward + " to at most " + maximumForwards + ".";
          reportIssue(XmlFile.nodeLocation(node), message, secondaries);
        }
      });
  }
}
