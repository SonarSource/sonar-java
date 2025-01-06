/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "S1147")
public class SystemExitCalledCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CONSTRUCTOR) || !MethodTreeUtils.isMainMethod((MethodTree) tree)) {
      tree.accept(new InvocationVisitor());
    }
  }

  private class InvocationVisitor extends BaseTreeVisitor {

    private String idName;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (isCallToExitMethod(tree)) {
        reportIssue(tree.methodSelect(), "Remove this call to \"" + idName + "\" or ensure it is really required.");
      }
    }

    private boolean isCallToExitMethod(MethodInvocationTree tree) {
      String selection = concatenate(tree.methodSelect());
      return "System.exit".equals(selection)
        || "Runtime.getRuntime().exit".equals(selection)
        || "Runtime.getRuntime().halt".equals(selection);
    }

    private String concatenate(ExpressionTree tree) {
      Deque<String> pieces = new LinkedList<>();

      ExpressionTree expr = tree;
      while (expr.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
        pieces.push(mse.identifier().name());
        pieces.push(".");
        expr = mse.expression();
      }
      if (expr.is(Tree.Kind.METHOD_INVOCATION)) {
        pieces.push("()");
        pieces.push(concatenate(((MethodInvocationTree) expr).methodSelect()));
      }
      if (expr.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree idt = (IdentifierTree) expr;
        pieces.push(idt.name());
      }

      StringBuilder sb = new StringBuilder();
      idName = pieces.getLast();
      for (String piece : pieces) {
        sb.append(piece);
      }
      return sb.toString();
    }
  }
}
