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
package org.sonar.java.checks.xml.web;

import com.google.common.collect.Iterables;
import org.sonar.check.Rule;
import org.sonar.java.xml.XmlCheckContext;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpression;
import java.util.HashSet;
import java.util.Set;

@Rule(key = "S3355")
public class ValidationFiltersCheck extends WebXmlCheckTemplate {
  private XPathExpression filterNamesFromFilterExpression;
  private XPathExpression filterNamesFromFilterMappingExpression;

  @Override
  public void precompileXPathExpressions(XmlCheckContext context) {
    filterNamesFromFilterExpression = context.compile(WEB_XML_ROOT + "/filter/filter-name");
    filterNamesFromFilterMappingExpression = context.compile(WEB_XML_ROOT + "/filter-mapping/filter-name");
  }

  @Override
  public void scanWebXml(XmlCheckContext context) {
    Iterable<Node> filtersDefinedInFilters = context.evaluateOnDocument(filterNamesFromFilterExpression);
    Iterable<Node> filtersUsedInMapping = context.evaluateOnDocument(filterNamesFromFilterMappingExpression);
    if (Iterables.isEmpty(filtersDefinedInFilters) || Iterables.isEmpty(filtersUsedInMapping)) {
      reportIssueOnFile("Add a validation filter to this \"web.xml\".");
    } else {
      Set<String> filterNamesFromFilters = getFilterNames(filtersDefinedInFilters);
      for (Node node : filtersUsedInMapping) {
        String filterName = getStringValue(node);
        if (!filterNamesFromFilters.contains(filterName)) {
          reportIssue(node, "\"" + filterName + "\" is not defined in this file.");
        }
      }
    }
  }

  private static Set<String> getFilterNames(Iterable<Node> nodes) {
    Set<String> nodeByFilterName = new HashSet<>();
    for (Node node : nodes) {
      nodeByFilterName.add(getStringValue(node));
    }
    return nodeByFilterName;
  }

  private static String getStringValue(Node node) {
    return node.getFirstChild().getNodeValue();
  }

}
