/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2718",
  name = "\"DateUtils.truncate\" from Apache Commons Lang library should not be used",
  tags = {"java8", "performance"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.CPU_EFFICIENCY)
@SqaleConstantRemediation("10min")
public class DateUtilsTruncateCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      truncateMethodMatcher("java.util.Date"),
      truncateMethodMatcher("java.util.Calendar"),
      truncateMethodMatcher("java.lang.Object"));
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    addIssue(mit, "Use \"Instant.truncatedTo\" instead.");
  }

  private MethodInvocationMatcher truncateMethodMatcher(String firstParameterType) {
    return MethodInvocationMatcher.create()
      .typeDefinition("org.apache.commons.lang.time.DateUtils").name("truncate").addParameter(firstParameterType).addParameter("int");
  }
}
