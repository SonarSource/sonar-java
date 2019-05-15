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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;

@Rule(key = "S5304")
public class EnvVariablesHotspotCheck extends AbstractMethodDetection {

  private static final MethodMatcher RUNTIME_EXEC =
    MethodMatcher.create().typeDefinition("java.lang.Runtime").name("exec").withAnyParameters();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition("java.lang.System").name("getenv").withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.lang.ProcessBuilder").name("environment").withoutParameter(),
      RUNTIME_EXEC
      );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!isRuntimeExecWithoutSettingEnv(mit)) {
      reportIssue(mit, "Make sure that environment variables are used safely here.");
    }
  }

  private static boolean isRuntimeExecWithoutSettingEnv(MethodInvocationTree mit) {
    return RUNTIME_EXEC.matches(mit) &&
      (mit.arguments().size() < 2 || mit.arguments().get(1).is(NULL_LITERAL));
  }
}
