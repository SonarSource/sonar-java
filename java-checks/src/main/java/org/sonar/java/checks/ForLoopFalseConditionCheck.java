/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S2252",
  priority = Priority.CRITICAL,
  tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class ForLoopFalseConditionCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ForStatementTree forStatement = (ForStatementTree) tree;
    ExpressionTree condition = forStatement.condition();
    if (condition != null && (isAlwaysFalseCondition(condition) || isConditionFalseAtInitialization(forStatement))) {
      addIssue(condition, "This loop will never execute.");
    }
  }

  private boolean isAlwaysFalseCondition(ExpressionTree expression) {
    if (expression.is(Tree.Kind.BOOLEAN_LITERAL)) {
      return BooleanUtils.isFalse(booleanLiteralValue(expression));
    }
    if (expression.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      ExpressionTree subExpression = ((UnaryExpressionTree) expression).expression();
      return BooleanUtils.isTrue(booleanLiteralValue(subExpression));
    }
    return false;
  }

  @CheckForNull
  private Boolean booleanLiteralValue(ExpressionTree expression) {
    if (expression.is(Tree.Kind.BOOLEAN_LITERAL)) {
      return Boolean.valueOf(((LiteralTree) expression).value());
    }
    return null;
  }

  private boolean isConditionFalseAtInitialization(ForStatementTree forStatement) {
    Iterable<ForLoopInitializer> initializers = ForLoopInitializer.list(forStatement);
    ExpressionTree condition = forStatement.condition();
    if (!condition.is(Tree.Kind.GREATER_THAN, Tree.Kind.GREATER_THAN_OR_EQUAL_TO, Tree.Kind.LESS_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO)) {
      return false;
    }
    BinaryExpressionTree binaryCondition = (BinaryExpressionTree) condition;
    Integer leftOperand = eval(binaryCondition.leftOperand(), initializers);
    Integer rightOperand = eval(binaryCondition.rightOperand(), initializers);
    if (leftOperand != null && rightOperand != null) {
      return !evaluateCondition(condition, leftOperand, rightOperand);
    }
    return false;
  }

  private boolean evaluateCondition(ExpressionTree condition, int leftOperand, int rightOperand) {
    boolean conditionValue = true;
    if (condition.is(Tree.Kind.GREATER_THAN)) {
      conditionValue = leftOperand > rightOperand;
    } else if (condition.is(Tree.Kind.GREATER_THAN_OR_EQUAL_TO)) {
      conditionValue = leftOperand >= rightOperand;
    } else if (condition.is(Tree.Kind.LESS_THAN)) {
      conditionValue = leftOperand < rightOperand;
    } else if (condition.is(Tree.Kind.LESS_THAN_OR_EQUAL_TO)) {
      conditionValue = leftOperand <= rightOperand;
    }
    return conditionValue;
  }

  private Integer eval(ExpressionTree expression, Iterable<ForLoopInitializer> initializers) {
    Integer intLiteralValue = intLiteralValue(expression);
    if (intLiteralValue == null) {
      for (ForLoopInitializer initializer : initializers) {
        if (initializer.isSameIdentifier(expression)) {
          intLiteralValue = initializer.value;
        }
      }
    }
    return intLiteralValue;
  }

  private static class ForLoopInitializer {

    private final IdentifierTree identifier;
    private final Integer value;

    public ForLoopInitializer(IdentifierTree identifier, Integer value) {
      this.identifier = identifier;
      this.value = value;
    }

    public boolean isSameIdentifier(ExpressionTree expression) {
      return isSameIdentifier(identifier, expression);
    }

    private static boolean isSameIdentifier(IdentifierTree identifier, ExpressionTree expression) {
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree other = (IdentifierTree) expression;
        return other.name().equals(identifier.name());
      }
      return false;
    }

    public static Iterable<ForLoopInitializer> list(ForStatementTree forStatement) {
      List<ForLoopInitializer> list = Lists.newArrayList();
      for (StatementTree statement : forStatement.initializer()) {
        if (statement.is(Tree.Kind.VARIABLE)) {
          VariableTree variable = (VariableTree) statement;
          ExpressionTree initializer = variable.initializer();
          Integer value = initializer == null ? null : intLiteralValue(initializer);
          list.add(new ForLoopInitializer(variable.simpleName(), value));
        }
        if (statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
          ExpressionTree expression = ((ExpressionStatementTree) statement).expression();
          ForLoopInitializer initializer = assignmentInitializer(expression);
          if (initializer != null) {
            list.add(initializer);
          }
        }
      }
      return list;
    }

    private static ForLoopInitializer assignmentInitializer(ExpressionTree expression) {
      if (expression.is(Tree.Kind.ASSIGNMENT)) {
        AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
        ExpressionTree variable = assignment.variable();
        if (variable.is(Tree.Kind.IDENTIFIER)) {
          return new ForLoopInitializer((IdentifierTree) variable, intLiteralValue(assignment.expression()));
        }
      }
      return null;
    }

  }

  @CheckForNull
  private static Integer intLiteralValue(ExpressionTree expression) {
    if (expression.is(Tree.Kind.INT_LITERAL)) {
      return intLiteralValue((LiteralTree) expression);
    }
    if (expression.is(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
      UnaryExpressionTree unaryExp = (UnaryExpressionTree) expression;
      Integer subExpressionIntValue = intLiteralValue(unaryExp.expression());
      return expression.is(Tree.Kind.UNARY_MINUS) ? minus(subExpressionIntValue) : subExpressionIntValue;
    }
    return null;
  }

  @CheckForNull
  private static Integer intLiteralValue(LiteralTree literal) {
    String literalValue = literal.value();
    if (literalValue.startsWith("0x") || literalValue.startsWith("0b")) {
      return null;
    }
    return Integer.valueOf(literalValue);
  }

  @CheckForNull
  private static Integer minus(@Nullable Integer nullableInteger) {
    return nullableInteger == null ? null : -nullableInteger;
  }

}
