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
package org.sonar.java.se.checks;

import org.sonar.java.model.SEExpressionUtils;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class SyntaxTreeNameFinder extends BaseTreeVisitor {

  private String name;

  public static String getName(Tree syntaxNode) {
    SyntaxTreeNameFinder finder = new SyntaxTreeNameFinder();
    syntaxNode.accept(finder);
    return finder.getName();
  }

  private String getName() {
    return name;
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    name = tree.name();
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    ExpressionTree expression = SEExpressionUtils.skipParentheses(tree.expression());
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      String identifierName = ((IdentifierTree) expression).name();
      if ("this".equals(identifierName) || "super".equals(identifierName)) {
        tree.identifier().accept(this);
        return;
      }
    }
    expression.accept(this);
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    tree.expression().accept(this);
  }

  @Override
  public void visitParenthesized(ParenthesizedTree tree) {
    tree.expression().accept(this);
  }

  @Override
  public void visitTypeCast(TypeCastTree tree) {
    tree.expression().accept(this);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    ExpressionTree methodSelect = tree.methodSelect();
    if(methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      name = ((MemberSelectExpressionTree) methodSelect).identifier().name();
    } else {
      methodSelect.accept(this);
    }
  }

  @Override
  public void visitVariable(VariableTree tree) {
    if (tree.initializer() == null) {
      name = tree.simpleName().name();
    } else {
      super.visitVariable(tree);
    }
  }
}
