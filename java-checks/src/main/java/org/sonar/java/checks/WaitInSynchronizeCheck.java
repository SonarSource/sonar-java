/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2273")
public class WaitInSynchronizeCheck extends AbstractInSynchronizeChecker {

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!isInSyncBlock()) {
      IdentifierTree methodName = ExpressionUtils.methodName(mit);
      ExpressionTree methodSelect = mit.methodSelect();
      String lockName;
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        lockName = ((MemberSelectExpressionTree) methodSelect).expression().symbolType().name();
      } else {
        lockName = "this";
      }
      reportIssue(methodName, "Move this call to \"" + methodName + "()\" into a synchronized block to be sure the monitor on \"" + lockName + "\" is held.");
    }
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofAnyType().names("wait")
        .addWithoutParametersMatcher()
        .addParametersMatcher("long")
        .addParametersMatcher("long", "int")
        .build(),
      MethodMatchers.create()
        .ofAnyType()
        .names("notify", "notifyAll")
        .addWithoutParametersMatcher()
        .build());
  }
}
