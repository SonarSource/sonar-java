/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S8432")
public class UnusedScopedValueWhereResultCheck extends IssuableSubscriptionVisitor {
  private final List<String> immediateUsageMethods = List.of("run", "call");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof MethodInvocationTree methodInvocationTree) {
      visitMethodInvocationTree(methodInvocationTree);
    }
  }


  private void visitMethodInvocationTree(MethodInvocationTree tree) {
    if (tree.methodSelect() instanceof MemberSelectExpressionTree memberSelectExpressionTree &&
      "where".equals(memberSelectExpressionTree.identifier().name())) {
      boolean usedImmediately = tree.parent() instanceof MemberSelectExpressionTree parentMemberSelect &&
        immediateUsageMethods.contains(parentMemberSelect.identifier().name());
      if (usedImmediately) {
        return;
      }
      if (tree.parent() instanceof VariableTree variableTree) {
        return;
      }
      reportIssue(tree, "Hello");
    }
  }
}


