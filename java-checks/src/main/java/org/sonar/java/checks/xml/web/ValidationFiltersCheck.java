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
package org.sonar.java.checks.xml.web;

import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.java.xml.XmlCheckContext;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.xml.xpath.XPathExpression;

@Rule(
  key = "S3355",
  name = "Web applications should use validation filters",
  priority = Priority.CRITICAL,
  tags = {Tag.INJECTION, Tag.OWASP_A1, Tag.SECURITY})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SECURITY_FEATURES)
@SqaleConstantRemediation("30min")
@ActivatedByDefault
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
    if (hasMissingFilter(context)) {
      reportIssueOnFile("Add a validation filter to this \"web.xml\".");
    }
  }

  private boolean hasMissingFilter(XmlCheckContext context) {
    return Iterables.isEmpty(context.evaluateOnDocument(filterNamesFromFilterExpression))
      || Iterables.isEmpty(context.evaluateOnDocument(filterNamesFromFilterMappingExpression));
  }

}
