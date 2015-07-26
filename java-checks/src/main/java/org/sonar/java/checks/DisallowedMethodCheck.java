/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.squidbridge.annotations.NoSqale;
import org.sonar.squidbridge.annotations.RuleTemplate;
import org.sonar.squidbridge.api.CheckMessage;

import java.util.List;

@Rule(
  key = "S2253",
  name = "Disallowed methods should not be used",
  priority = Priority.MAJOR)
@RuleTemplate
@NoSqale
public class DisallowedMethodCheck extends AbstractMethodDetection {

  @RuleProperty(key = "className", description = "Name of the class whose method is forbidden")
  private String className = "";

  @RuleProperty(key = "methodName", description = "Name of the forbidden method")
  private String methodName = "";

  @RuleProperty(key = "argumentTypes", description = "Comma-delimited list of argument types, E.G. java.lang.String, int[], int")
  private String argumentTypes = "";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    if(StringUtils.isEmpty(methodName)) {
      return ImmutableList.of();
    }
    MethodMatcher invocationMatcher = MethodMatcher.create().name(methodName);
    if(StringUtils.isNotEmpty(className)) {
      invocationMatcher.typeDefinition(className);
    }
    String[] args = StringUtils.split(argumentTypes, ",");
    for (String arg : args) {
      invocationMatcher.addParameter(StringUtils.trim(arg));
    }
    return ImmutableList.of(invocationMatcher);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    context.addIssue(mit, new CheckMessage(this, "Remove this forbidden call"));
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public void setArgumentTypes(String argumentTypes) {
    this.argumentTypes = argumentTypes;
  }
}
