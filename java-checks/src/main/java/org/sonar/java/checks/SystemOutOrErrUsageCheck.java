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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S106")
public class SystemOutOrErrUsageCheck extends IssuableSubscriptionVisitor {

  private boolean isCompactSourceFile = false;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(
      Tree.Kind.COMPILATION_UNIT,
      Tree.Kind.IMPLICIT_CLASS,
      Tree.Kind.MEMBER_SELECT
    );
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.COMPILATION_UNIT)) {
      isCompactSourceFile = false;
    } else if (tree.is(Tree.Kind.IMPLICIT_CLASS)) {
      // System.out or System.err is allowed in compact source files.
      isCompactSourceFile = true;
    } else if (!isCompactSourceFile && tree instanceof MemberSelectExpressionTree mset) {
      visitMemberSelectExpression(mset);
    }
  }

  private void visitMemberSelectExpression(MemberSelectExpressionTree mset) {
    String name = mset.identifier().name();

    if ("out".equals(name) && isSystem(mset.expression())) {
      reportIssue(mset, "Replace this use of System.out by a logger.");
    } else if ("err".equals(name) && isSystem(mset.expression())) {
      reportIssue(mset, "Replace this use of System.err by a logger.");
    }
  }

  private static boolean isSystem(ExpressionTree expression) {
    IdentifierTree identifierTree = null;
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      identifierTree = (IdentifierTree) expression;
    } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      identifierTree = ((MemberSelectExpressionTree) expression).identifier();
    }
    return identifierTree != null && "System".equals(identifierTree.name());
  }
}
