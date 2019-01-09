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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S888")
public class ForLoopTerminationConditionCheck extends AbstractForLoopRule {

  @Override
  public void visitForStatement(ForStatementTree forStatement) {
    ExpressionTree condition = forStatement.condition();
    if (condition == null || !condition.is(Tree.Kind.NOT_EQUAL_TO)) {
      return;
    }
    BinaryExpressionTree inequalityCondition = (BinaryExpressionTree) condition;
    IntInequality loopVarAndTerminalValue = IntInequality.of(inequalityCondition);
    if (loopVarAndTerminalValue != null) {
      IdentifierTree loopIdentifier = loopVarAndTerminalValue.identifier;
      int terminationValue = loopVarAndTerminalValue.literalValue;
      Integer initialValue = initialValue(loopIdentifier, forStatement);
      if (initialValue != null && initialValue != terminationValue) {
        checkIncrement(forStatement, loopIdentifier, initialValue < terminationValue);
      }
    }
  }

  private void checkIncrement(ForStatementTree forStatement, IdentifierTree loopIdentifier, boolean positiveIncrement) {
    if (forStatement.update().size() <= 1) {
      ForLoopIncrement loopIncrement = ForLoopIncrement.findInUpdates(forStatement);
      if (loopIncrement == null || !loopIncrement.hasSameIdentifier(loopIdentifier)) {
        addIssue(forStatement);
      } else if (loopIncrement.hasValue()) {
        int requiredIncrement = positiveIncrement ? 1 : -1;
        if (loopIncrement.value() != requiredIncrement || forBodyUpdatesLoopIdentifier(forStatement, loopIdentifier)) {
          addIssue(forStatement);
        }
      }
    }
  }

  private void addIssue(ForStatementTree tree) {
    reportIssue(tree.condition(), "Replace '!=' operator with one of '<=', '>=', '<', or '>' comparison operators.");
  }

  private static boolean forBodyUpdatesLoopIdentifier(ForStatementTree forStatement, IdentifierTree loopIdentifier) {
    LoopVariableAssignmentVisitor visitor = new LoopVariableAssignmentVisitor(loopIdentifier);
    forStatement.statement().accept(visitor);
    return visitor.foundAssignment;
  }

  private static class LoopVariableAssignmentVisitor extends BaseTreeVisitor {

    private final IdentifierTree loopIdentifier;
    private boolean foundAssignment = false;

    public LoopVariableAssignmentVisitor(IdentifierTree loopIdentifier) {
      this.loopIdentifier = loopIdentifier;
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree unaryExp) {
      if (isSameIdentifier(loopIdentifier, unaryExp.expression())
        && unaryExp.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.PREFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT)) {
        foundAssignment = true;
      }
      super.visitUnaryExpression(unaryExp);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree assignmentExpression) {
      if (isSameIdentifier(loopIdentifier, assignmentExpression.variable())) {
        foundAssignment = true;
      }
      super.visitAssignmentExpression(assignmentExpression);
    }
  }

  private static Integer initialValue(IdentifierTree loopIdentifier, ForStatementTree forStatement) {
    Integer value = null;
    for (ForLoopInitializer initializer : ForLoopInitializer.list(forStatement)) {
      if (initializer.hasSameIdentifier(loopIdentifier) && initializer.value() != null) {
        value = initializer.value();
      }
    }
    return value;
  }
  
  private static class IntInequality {
    
    private final IdentifierTree identifier;
    private final int literalValue;
    
    private IntInequality(IdentifierTree identifier, int value) {
      this.identifier = identifier;
      this.literalValue = value;
    }
    
    public static IntInequality of(BinaryExpressionTree binaryExp) {
      Integer value = null;
      IdentifierTree identifier = null;
      for (ExpressionTree expressionTree : ImmutableList.of(binaryExp.leftOperand(), binaryExp.rightOperand())) {
        if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
          identifier = (IdentifierTree) expressionTree;
        } else {
          value = LiteralUtils.intLiteralValue(expressionTree);
        }
      }
      if (identifier != null && value != null) {
        return new IntInequality(identifier, value);
      }
      return null;
    }
  }

}
