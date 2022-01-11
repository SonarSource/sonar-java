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
package org.sonar.java.checks.spring;


import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5876")
public class SpringSessionFixationCheck extends AbstractMethodDetection {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer$SessionFixationConfigurer")
      .names("none")
      .addWithoutParametersMatcher()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodInvocation) {
    reportIssue(ExpressionUtils.methodName(methodInvocation), "Create a new session during user authentication to prevent session fixation attacks.");
  }

}
