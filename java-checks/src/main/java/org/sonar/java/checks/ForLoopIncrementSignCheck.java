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
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2251")
public class ForLoopIncrementSignCheck extends AbstractForLoopRule {

  @Override
  public void visitForStatement(ForStatementTree forStatement) {
    ExpressionTree condition = forStatement.condition();
    ForLoopIncrement loopIncrement = ForLoopIncrement.findInUpdates(forStatement);
    if (condition == null || loopIncrement == null || !loopIncrement.hasValue()) {
      return;
    }
    checkIncrementSign(condition, loopIncrement);
  }

  private void checkIncrementSign(ExpressionTree condition, ForLoopIncrement loopIncrement) {
    if (condition.is(Tree.Kind.GREATER_THAN, Tree.Kind.GREATER_THAN_OR_EQUAL_TO)) {
      BinaryExpressionTree binaryExp = (BinaryExpressionTree) condition;
      if (loopIncrement.hasSameIdentifier(binaryExp.leftOperand())) {
        checkNegativeIncrement(condition, loopIncrement);
      } else if (loopIncrement.hasSameIdentifier(binaryExp.rightOperand())) {
        checkPositiveIncrement(condition, loopIncrement);
      }
    } else if (condition.is(Tree.Kind.LESS_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO)) {
      BinaryExpressionTree binaryExp = (BinaryExpressionTree) condition;
      if (loopIncrement.hasSameIdentifier(binaryExp.leftOperand())) {
        checkPositiveIncrement(condition, loopIncrement);
      } else if (loopIncrement.hasSameIdentifier(binaryExp.rightOperand())) {
        checkNegativeIncrement(condition, loopIncrement);
      }
    }
  }

  private void checkPositiveIncrement(Tree tree, ForLoopIncrement loopIncrement) {
    if (loopIncrement.value() < 0) {
      reportIssue(tree, String.format("\"%s\" is decremented and will never reach \"stop condition\".", loopIncrement.identifier().name()));
    }
  }

  private void checkNegativeIncrement(Tree tree, ForLoopIncrement loopIncrement) {
    if (loopIncrement.value() > 0) {
      reportIssue(tree, String.format("\"%s\" is incremented and will never reach \"stop condition\".", loopIncrement.identifier().name()));
    }
  }


}
