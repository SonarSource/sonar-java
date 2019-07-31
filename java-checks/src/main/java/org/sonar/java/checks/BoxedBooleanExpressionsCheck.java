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
package org.sonar.java.checks;

import javax.annotation.CheckForNull;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type.Primitives;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S5411")
public class BoxedBooleanExpressionsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final MethodMatcher OPTIONAL_ORELSE = MethodMatcher.create().typeDefinition("java.util.Optional").name("orElse").addParameter(TypeCriteria.anyType());
  private static final String BOOLEAN = "java.lang.Boolean";
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    if (tree.condition() != null && !isSafeBooleanExpression(tree.condition())) {
      scan(tree.initializer());
      scan(tree.update());
      scan(tree.statement());
    } else {
      super.visitForStatement(tree);
    }
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    if (!isSafeBooleanExpression(tree.condition())) {
      scan(tree.statement());
    } else {
      super.visitWhileStatement(tree);
    }
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    if (!isSafeBooleanExpression(tree.condition())) {
      scan(tree.statement());
    } else {
      super.visitDoWhileStatement(tree);
    }
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    if (!isSafeBooleanExpression(tree.condition())) {
      scan(tree.thenStatement());
      scan(tree.elseStatement());
    } else {
      super.visitIfStatement(tree);
    }
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    if (!isSafeBooleanExpression(tree.condition())) {
      scan(tree.trueExpression());
      scan(tree.falseExpression());
    } else {
      super.visitConditionalExpression(tree);
    }
  }

  private boolean isSafeBooleanExpression(ExpressionTree tree) {
    ExpressionTree boxedBoolean = findBoxedBoolean(tree);
    if (boxedBoolean != null) {
      context.reportIssue(this, boxedBoolean, "Use the primitive boolean expression here.");
      return false;
    }
    return true;
  }

  @CheckForNull
  private static ExpressionTree findBoxedBoolean(ExpressionTree tree) {
    if (tree.symbolType().is(BOOLEAN) && !isOptionalInvocation(tree)) {
      return tree;
    }
    if (tree.is(Kind.LOGICAL_COMPLEMENT)) {
      return findBoxedBoolean(((UnaryExpressionTree) tree).expression());
    }
    if (tree instanceof BinaryExpressionTree) {
      BinaryExpressionTree expr = (BinaryExpressionTree) tree;
      if (findBoxedBoolean(expr.leftOperand()) != null && expr.rightOperand().symbolType().isPrimitive(Primitives.BOOLEAN)) {
        return expr.leftOperand();
      }
      if (findBoxedBoolean(expr.rightOperand()) != null && expr.leftOperand().symbolType().isPrimitive(Primitives.BOOLEAN) && !isNullCheck(expr.leftOperand())) {
        return expr.rightOperand();
      }
    }
    return null;
  }

  private static boolean isNullCheck(ExpressionTree tree) {
    if (tree.is(Kind.NOT_EQUAL_TO, Kind.EQUAL_TO)) {
      BinaryExpressionTree expr = (BinaryExpressionTree) tree;
      return expr.leftOperand().is(Kind.NULL_LITERAL) || expr.rightOperand().is(Kind.NULL_LITERAL);
    }
    return false;
  }

  private static boolean isOptionalInvocation(ExpressionTree tree) {
    if (tree.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      return OPTIONAL_ORELSE.matches(mit) && !mit.arguments().get(0).is(Kind.NULL_LITERAL);
    }
    return false;
  }
}
