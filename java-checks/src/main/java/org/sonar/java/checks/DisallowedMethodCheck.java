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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2253")
public class DisallowedMethodCheck extends AbstractMethodDetection {

  @RuleProperty(key = "className", description = "Name of the class whose method is forbidden")
  private String className = "";

  @RuleProperty(key = "methodName", description = "Name of the forbidden method")
  private String methodName = "";

  @RuleProperty(key = "argumentTypes", description = "Comma-delimited list of argument types, E.G. java.lang.String, int[], int")
  private String argumentTypes = "";

  @RuleProperty(key = "allOverloads", description = "Set to true to flag all overloads regardless of parameter type", defaultValue = "false")
  private boolean allOverloads = false;

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    if (StringUtils.isEmpty(methodName)) {
      return Collections.emptyList();
    }
    MethodMatcher invocationMatcher = MethodMatcher.create().name(methodName);
    if (StringUtils.isNotEmpty(className)) {
      invocationMatcher.typeDefinition(className);
    }
    if (allOverloads) {
      invocationMatcher.withAnyParameters();
    } else {
      String[] args = StringUtils.split(argumentTypes, ",");
      if (args.length == 0) {
        invocationMatcher.withoutParameter();
      } else {
        for (String arg : args) {
          invocationMatcher.addParameter(StringUtils.trim(arg));
        }
      }
    }
    return Collections.singletonList(invocationMatcher);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(ExpressionUtils.methodName(mit), "Remove this forbidden call");
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

  public void setAllOverloads(boolean allOverloads) {
    this.allOverloads = allOverloads;
  }
}
