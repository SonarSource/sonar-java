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

import com.google.common.base.Preconditions;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.sonar.java.model.LiteralUtils.intLiteralValue;

public abstract class AbstractForLoopRule extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ForStatementTree forStatement = (ForStatementTree) tree;
    visitForStatement(forStatement);
  }

  public abstract void visitForStatement(ForStatementTree forStatement);

  protected static boolean isSameIdentifier(IdentifierTree identifier, ExpressionTree expression) {
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree other = (IdentifierTree) expression;
      return other.name().equals(identifier.name());
    }
    return false;
  }

  protected static class IntVariable {

    private final IdentifierTree identifier;

    public IntVariable(IdentifierTree identifier) {
      this.identifier = identifier;
    }

    public boolean hasSameIdentifier(ExpressionTree expression) {
      return isSameIdentifier(identifier, expression);
    }

    public IdentifierTree identifier() {
      return identifier;
    }
  }

  protected static class ForLoopInitializer extends IntVariable {

    private final Integer value;

    public ForLoopInitializer(IdentifierTree identifier, @Nullable Integer value) {
      super(identifier);
      this.value = value;
    }

    @CheckForNull
    public Integer value() {
      return value;
    }

    public static Iterable<ForLoopInitializer> list(ForStatementTree forStatement) {
      List<ForLoopInitializer> list = new ArrayList<>();
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

  protected static class ForLoopIncrement extends IntVariable {

    private final Integer value;

    public ForLoopIncrement(IdentifierTree identifier, @Nullable Integer value) {
      super(identifier);
      this.value = value;
    }

    public boolean hasValue() {
      return value != null;
    }

    public int value() {
      Preconditions.checkState(value != null, "This ForLoopIncrement has no value");
      return value;
    }

    @CheckForNull
    public static ForLoopIncrement findInUpdates(ForStatementTree forStatement) {
      ForLoopIncrement result = null;
      List<StatementTree> updates = forStatement.update();
      if (updates.size() == 1) {
        ExpressionStatementTree statement = (ExpressionStatementTree) updates.get(0);
        ExpressionTree expression = statement.expression();
        if (expression.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_INCREMENT)) {
          UnaryExpressionTree unaryExp = (UnaryExpressionTree) expression;
          result = increment(unaryExp.expression(), 1);
        } else if (expression.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.PREFIX_DECREMENT)) {
          UnaryExpressionTree unaryExp = (UnaryExpressionTree) expression;
          result = increment(unaryExp.expression(), -1);
        } else if (expression.is(Tree.Kind.PLUS_ASSIGNMENT)) {
          AssignmentExpressionTree assignmentExp = (AssignmentExpressionTree) expression;
          result = increment(assignmentExp.variable(), intLiteralValue(assignmentExp.expression()));
        } else if (expression.is(Tree.Kind.MINUS_ASSIGNMENT)) {
          AssignmentExpressionTree assignmentExp = (AssignmentExpressionTree) expression;
          result = increment(assignmentExp.variable(), minus(intLiteralValue(assignmentExp.expression())));
        } else if (expression.is(Tree.Kind.ASSIGNMENT)) {
          AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
          result = assignmentIncrement(assignment);
        }
      }
      return result;
    }

    @CheckForNull
    private static ForLoopIncrement increment(ExpressionTree expression, Integer value) {
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        return new ForLoopIncrement((IdentifierTree) expression, value);
      }
      return null;
    }

    @CheckForNull
    private static Integer minus(@Nullable Integer nullableInteger) {
      return nullableInteger == null ? null : -nullableInteger;
    }

    private static ForLoopIncrement assignmentIncrement(AssignmentExpressionTree assignmentExpression) {
      ExpressionTree expression = assignmentExpression.expression();
      ExpressionTree variable = assignmentExpression.variable();
      if (variable.is(Tree.Kind.IDENTIFIER) && expression.is(Tree.Kind.PLUS, Tree.Kind.MINUS)) {
        BinaryExpressionTree binaryExp = (BinaryExpressionTree) expression;
        Integer increment = intLiteralValue(binaryExp.rightOperand());
        if (increment != null && isSameIdentifier((IdentifierTree) variable, binaryExp.leftOperand())) {
          increment = expression.is(Tree.Kind.MINUS) ? minus(increment) : increment;
          return increment(variable, increment);
        }
        return new ForLoopIncrement((IdentifierTree) variable, null);
      }
      return null;
    }

  }
}
