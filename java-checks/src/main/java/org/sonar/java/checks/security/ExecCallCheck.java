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
package org.sonar.java.checks.security;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S4721")
public class ExecCallCheck extends AbstractMethodDetection {


  private static final String MESSAGE = "Make sure that executing this OS command is safe here.";
  private static final String PROCESS_BUILDER = "java.lang.ProcessBuilder";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition("java.lang.Runtime").name("exec").withAnyParameters(),
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("org.apache.commons.exec.Executor")).name("execute").withAnyParameters(),
      MethodMatcher.create().typeDefinition(PROCESS_BUILDER).name("command").withAnyParameters(),
      // constructor
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(PROCESS_BUILDER)).name("<init>").withAnyParameters()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!(mit.symbol().name().equals("command") && mit.arguments().isEmpty())) {
      reportIssue(mit, MESSAGE);
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (!newClassTree.arguments().isEmpty()) {
      reportIssue(newClassTree, MESSAGE);
    }
  }
}
