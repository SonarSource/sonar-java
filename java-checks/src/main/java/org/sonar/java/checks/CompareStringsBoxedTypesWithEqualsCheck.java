/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4973")
public class CompareStringsBoxedTypesWithEqualsCheck extends CompareWithEqualsVisitor {

  private static final String ISSUE_MESSAGE = "Strings and Boxed types should be compared using \"equals()\".";
  private static final String QUICK_FIX_MESSAGE = "Replace with boxed comparison";
  private static final String DOT_EQUALS_AND_OPENING_PARENTHESIS = ".equals(";

  private QuickFixHelper.ImportSupplier importSupplier;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    importSupplier = null;
    super.scanFile(context);
  }

  @Override
  protected void checkEqualityExpression(BinaryExpressionTree tree) {
    Type leftOpType = tree.leftOperand().symbolType();
    Type rightOpType = tree.rightOperand().symbolType();
    if (!isNullComparison(leftOpType, rightOpType)
      && !isCompareWithBooleanConstant(tree.leftOperand(), tree.rightOperand())
      && (isStringType(leftOpType, rightOpType) || isBoxedType(leftOpType, rightOpType))) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(tree.operatorToken())
        .withMessage(ISSUE_MESSAGE)
        .withQuickFix(() -> computeConciseQuickFix(tree).orElseGet(() -> computeDefaultQuickFix(tree)))
        .report();
    }
  }

  private static boolean isCompareWithBooleanConstant(ExpressionTree left, ExpressionTree right) {
    return ExpressionsHelper.getConstantValueAsBoolean(left).value() != null ||
      ExpressionsHelper.getConstantValueAsBoolean(right).value() != null;
  }

  private static Optional<JavaQuickFix> computeConciseQuickFix(BinaryExpressionTree tree) {
    ExpressionTree leftOperand = tree.leftOperand();
    ExpressionTree rightOperand = tree.rightOperand();
    if (leftOperand.is(Tree.Kind.STRING_LITERAL)) {
      AnalyzerMessage.TextSpan interOperandSpace = AnalyzerMessage.textSpanBetween(
        leftOperand, false,
        rightOperand, false
      );
      JavaQuickFix.Builder quickFix = JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
        .addTextEdit(JavaTextEdit.insertAfterTree(rightOperand, ")"))
        .addTextEdit(JavaTextEdit.replaceTextSpan(interOperandSpace, DOT_EQUALS_AND_OPENING_PARENTHESIS));
      if (tree.is(Tree.Kind.NOT_EQUAL_TO)) {
        quickFix.addTextEdit(JavaTextEdit.insertBeforeTree(leftOperand, "!"));
      }
      return Optional.of(quickFix.build());
    } else if (rightOperand.is(Tree.Kind.STRING_LITERAL)) {
      String callEqualsOnLiteral = ((LiteralTree) rightOperand).value() + DOT_EQUALS_AND_OPENING_PARENTHESIS;
      String callToEquals = tree.is(Tree.Kind.NOT_EQUAL_TO) ? ("!" + callEqualsOnLiteral) : callEqualsOnLiteral;
      AnalyzerMessage.TextSpan leftOfOperatorToEndOfComparison = AnalyzerMessage.textSpanBetween(
        leftOperand, false,
        rightOperand, true
      );
      return Optional.of(
        JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
          .addTextEdit(JavaTextEdit.replaceTextSpan(leftOfOperatorToEndOfComparison, ")"))
          .addTextEdit(JavaTextEdit.insertBeforeTree(leftOperand, callToEquals))
          .build()
      );
    }
    return Optional.empty();
  }

  private JavaQuickFix computeDefaultQuickFix(BinaryExpressionTree tree) {
    String callToEquals = tree.is(Tree.Kind.NOT_EQUAL_TO) ? "!Objects.equals(" : "Objects.equals(";
    AnalyzerMessage.TextSpan interOperandSpace = AnalyzerMessage.textSpanBetween(
      tree.leftOperand(), false,
      tree.rightOperand(), false
    );
    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
      .addTextEdit(JavaTextEdit.insertAfterTree(tree.rightOperand(), ")"))
      .addTextEdit(JavaTextEdit.replaceTextSpan(interOperandSpace, ", "))
      .addTextEdit(JavaTextEdit.insertBeforeTree(tree.leftOperand(), callToEquals));

    if (importSupplier == null) {
      importSupplier = QuickFixHelper.newImportSupplier(context);
    }

    importSupplier.newImportEdit("java.util.Objects")
      .ifPresent(builder::addTextEdit);

    return builder.build();
  }

}
