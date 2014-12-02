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
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Cardinality;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import java.util.List;

@Rule(key = "S2253",
    cardinality = Cardinality.MULTIPLE,
    priority = Priority.MAJOR)
public class DisallowedMethodCheck extends AbstractMethodDetection {

  @RuleProperty(key = "className")
  private String className = "";

  @RuleProperty(key = "methodName")
  private String methodName = "";

  @RuleProperty(key = "argumentTypes")
  private String argumentTypes = "";

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    if(StringUtils.isEmpty(methodName)) {
      return ImmutableList.of();
    }
    MethodInvocationMatcher invocationMatcher = MethodInvocationMatcher.create().name(methodName);
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
  protected void onMethodFound(MethodInvocationTree mit) {
    addIssue(mit, "Remove this forbidden call");
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
