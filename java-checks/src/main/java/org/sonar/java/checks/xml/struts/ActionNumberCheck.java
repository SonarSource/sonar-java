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

import com.google.common.collect.Iterables;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.xml.XPathXmlCheck;
import org.sonar.java.xml.XmlCheckContext;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpression;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "S3373")
public class ActionNumberCheck extends XPathXmlCheck {

  private static final int DEFAULT_MAXIMUM_NUMBER_FORWARDS = 4;

  @RuleProperty(
    key = "threshold",
    description = "Maximum allowed number of ``<forward/>`` mappings in an ``<action>``",
    defaultValue = "" + DEFAULT_MAXIMUM_NUMBER_FORWARDS)
  public int maximumForwards = DEFAULT_MAXIMUM_NUMBER_FORWARDS;

  private XPathExpression actionsExpression;
  private XPathExpression forwardsFromActionExpression;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    actionsExpression = context.compile("struts-config/action-mappings/action");
    forwardsFromActionExpression = context.compile("forward");
  }

  @Override
  public void scanFileWithXPathExpressions(XmlCheckContext context) {
    for (Node action : context.evaluateOnDocument(actionsExpression)) {
      Iterable<Node> extraForwards = Iterables.skip(context.evaluate(forwardsFromActionExpression, action), maximumForwards);
      if (!Iterables.isEmpty(extraForwards)) {
        List<XmlCheckContext.XmlDocumentLocation> secondaries = new LinkedList<>();
        for (Node forward : extraForwards) {
          secondaries.add(new XmlCheckContext.XmlDocumentLocation("Move this forward to another action.", forward));
        }
        int cost = secondaries.size();
        int numberForward = maximumForwards + cost;
        String message = "Reduce the number of forwards in this action from " + numberForward + " to at most " + maximumForwards + ".";
        context.reportIssue(this, action, message, secondaries, cost);
      }
    }
  }

}
