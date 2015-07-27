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
package org.sonar.java.checks;


import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S1697",
  name = "Short-circuit logic should be used to prevent null pointer dereferences in conditionals",
  tags = {"bug"},
  priority = Priority.BLOCKER)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("2min")
public class NullDereferenceInConditionalCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (isAndWithNullComparison(tree) || isOrWithNullExclusion(tree)) {
      ExpressionTree nonNullOperand = getNonNullOperand(tree.leftOperand());
      IdentifierTree identifierTree = getIdentifier(nonNullOperand);
      if (identifierTree != null) {
        IdentifierVisitor visitor = new IdentifierVisitor(identifierTree);
        tree.rightOperand().accept(visitor);
        if (visitor.raiseIssue) {
          context.addIssue(tree, this, "Either reverse the equality operator in the \"" +
              identifierTree.name() + "\" null test, or reverse the logical operator that follows it.");
        }
      }
    }
    super.visitBinaryExpression(tree);
  }

  private static IdentifierTree getIdentifier(ExpressionTree tree) {
    ExpressionTree nonNullOperand = ExpressionsHelper.skipParentheses(tree);
    if (nonNullOperand.is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) nonNullOperand;
    }
    return null;
  }

  private static boolean isAndWithNullComparison(BinaryExpressionTree tree) {
    return tree.is(Tree.Kind.CONDITIONAL_AND) && isEqualNullComparison(tree.leftOperand());
  }

  private static boolean isOrWithNullExclusion(BinaryExpressionTree tree) {
    return tree.is(Tree.Kind.CONDITIONAL_OR) && isNotEqualNullComparison(tree.leftOperand());
  }

  private static class IdentifierVisitor extends BaseTreeVisitor {
    private IdentifierTree identifierTree;
    boolean raiseIssue = false;

    IdentifierVisitor(IdentifierTree identifierTree) {
      this.identifierTree = identifierTree;
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      if (tree.expression().is(Tree.Kind.IDENTIFIER) || tree.expression().is(Tree.Kind.MEMBER_SELECT)) {
        //Check only first identifier of a member select expression : in a.b.c we are only interested in a.
        scan(tree.expression());
      }
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      //Ignore assignement to the identifier
      if (!isIdentifierWithSameName(tree.variable())) {
        scan(tree.variable());
      }
      scan(tree.expression());
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      boolean scanLeft = true;
      boolean scanRight = true;
      if (tree.is(Tree.Kind.EQUAL_TO) || tree.is(Tree.Kind.NOT_EQUAL_TO)) {
        scanLeft = !isIdentifierWithSameName(tree.leftOperand());
        scanRight = !isIdentifierWithSameName(tree.rightOperand());
      }
      if (scanLeft) {
        scan(tree.leftOperand());
      }
      if (scanRight) {
        scan(tree.rightOperand());
      }
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      raiseIssue |= equalsIdentName(tree);
    }

    private boolean equalsIdentName(IdentifierTree tree) {
      return identifierTree.name().equals(tree.name());
    }

    private boolean isIdentifierWithSameName(ExpressionTree tree) {
      return tree.is(Tree.Kind.IDENTIFIER) && equalsIdentName((IdentifierTree) tree);
    }
  }

  private static boolean isEqualNullComparison(ExpressionTree tree) {
    return isNullComparison(tree, Tree.Kind.EQUAL_TO);
  }

  private static boolean isNotEqualNullComparison(ExpressionTree tree) {
    return isNullComparison(tree, Tree.Kind.NOT_EQUAL_TO);
  }

  private static boolean isNullComparison(ExpressionTree expressionTree, Tree.Kind comparatorKind) {
    ExpressionTree tree = ExpressionsHelper.skipParentheses(expressionTree);
    boolean result = false;
    if (tree.is(comparatorKind)) {
      BinaryExpressionTree binary = (BinaryExpressionTree) tree;
      result = binary.leftOperand().is(Tree.Kind.NULL_LITERAL) || binary.rightOperand().is(Tree.Kind.NULL_LITERAL);
    }
    return result;
  }

  private static ExpressionTree getNonNullOperand(ExpressionTree expressionTree) {
    BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) ExpressionsHelper.skipParentheses(expressionTree);
    if (binaryExpressionTree.leftOperand().is(Tree.Kind.NULL_LITERAL)) {
      return binaryExpressionTree.rightOperand();
    }
    return binaryExpressionTree.leftOperand();
  }

}
