/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
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
  protected MethodMatchers getMethodInvocationMatchers() {
    if (StringUtils.isEmpty(methodName)) {
      return MethodMatchers.none();
    }
    MethodMatchers.TypeBuilder typeBuilder = MethodMatchers.create();
    MethodMatchers.NameBuilder nameBuilder;
    if (StringUtils.isNotEmpty(className)) {
      nameBuilder = typeBuilder.ofTypes(className);
    } else {
      nameBuilder = typeBuilder.ofAnyType();
    }
    MethodMatchers.ParametersBuilder parametersBuilder = nameBuilder.names(methodName);

    if (allOverloads) {
      return parametersBuilder.withAnyParameters().build();
    } else {
      String[] args = StringUtils.split(argumentTypes, ",");
      if (args.length == 0) {
        return parametersBuilder.addWithoutParametersMatcher().build();
      } else {
        String[] trimmedArgs = new String[args.length];
        for (int i = 0; i < trimmedArgs.length; i++) {
          trimmedArgs[i] = StringUtils.trim(args[i]);
        }
        return parametersBuilder.addParametersMatcher(trimmedArgs).build();
      }
    }
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
