/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.se.checkers;

import org.sonar.java.model.JavaTree;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.SymbolicValue;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;

public class NullDereferenceCheck implements SEChecker {

  @Override
  public void checkPreStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.MEMBER_SELECT, Tree.Kind.METHOD_INVOCATION)) {
      MemberSelectExpressionTree mse = null;
      if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
        ExpressionTree expressionTree = ((MethodInvocationTree) syntaxNode).methodSelect();
        if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
          mse = (MemberSelectExpressionTree) expressionTree;
        }
      } else {
        mse = ((MemberSelectExpressionTree) syntaxNode);
      }

      if (mse != null) {
        IdentifierTree identifierTree = getFirstIdentifierOfMemberSelect(mse);
        if (identifierTree != null) {
          checkNullDeref(context, identifierTree);
          return;
        }
      }
    }
    context.addTransition(context.getState());
  }

  @CheckForNull
  private IdentifierTree getFirstIdentifierOfMemberSelect(MemberSelectExpressionTree memberSelectExpressionTree) {
    ExpressionTree mse = memberSelectExpressionTree;
    while (mse.is(Tree.Kind.MEMBER_SELECT)) {
      mse = ((MemberSelectExpressionTree) mse).expression();
    }
    if (mse.is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) mse;
    }
    return null;
  }

  private void checkNullDeref(CheckerContext context, IdentifierTree dereferenced) {
    SymbolicValue val = context.getVal(dereferenced);
    if (val != null && val.isNull()) {
      System.out.println("Null pointer dereference at line " + ((JavaTree) dereferenced).getLine());
      context.createSink();
      return;
    }
    // TODO : improve next state with assumption on not null value as we can safely assume that if we get passed this, value is not null.
    context.addTransition(context.getState());
  }
}
