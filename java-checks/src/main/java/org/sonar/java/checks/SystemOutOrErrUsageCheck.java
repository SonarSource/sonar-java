/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.MEMBER_SELECT);
  }

  @Override
  public void visitNode(Tree tree) {
    MemberSelectExpressionTree mset = (MemberSelectExpressionTree) tree;
    String name = mset.identifier().name();

    if ("out".equals(name) && isSystem(mset.expression())) {
      reportIssue(tree, "Replace this use of System.out by a logger.");
    } else if ("err".equals(name) && isSystem(mset.expression())) {
      reportIssue(tree, "Replace this use of System.err by a logger.");
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
