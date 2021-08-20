/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4973")
public class CompareStringsBoxedTypesWithEqualsCheck extends CompareWithEqualsVisitor {

  private static final String ISSUE_MESSAGE = "Strings and Boxed types should be compared using \"equals()\"";
  private static final String QUICK_FIX_MESSAGE = "Replace with boxed comparison";
  private static final String DOT_EQUALS_AND_OPENING_PARENTHESIS = ".equals(";

  @Override
  protected void checkEqualityExpression(BinaryExpressionTree tree) {
    Type leftOpType = tree.leftOperand().symbolType();
    Type rightOpType = tree.rightOperand().symbolType();
    if (!isNullComparison(leftOpType, rightOpType)
      && !isCompareWithBooleanConstant(tree.leftOperand(), tree.rightOperand())
      && (isStringType(leftOpType, rightOpType) || isBoxedType(leftOpType, rightOpType))) {
      ((InternalJavaIssueBuilder) ((DefaultJavaFileScannerContext) this.context).newIssue())
        .forRule(this)
        .onTree(tree.operatorToken())
        .withMessage(ISSUE_MESSAGE)
        .withQuickFixes(() -> computeQuickFix(tree))
        .report();
    }
  }

  private static boolean isCompareWithBooleanConstant(ExpressionTree left, ExpressionTree right) {
    return ExpressionsHelper.getConstantValueAsBoolean(left).value() != null ||
      ExpressionsHelper.getConstantValueAsBoolean(right).value() != null;
  }

  private static List<JavaQuickFix> computeQuickFix(BinaryExpressionTree tree) {
    Optional<JavaQuickFix> conciseQuickFix = computeConciseQuickFix(tree);
    if (conciseQuickFix.isPresent()) {
      return Collections.singletonList(conciseQuickFix.get());
    }
    return Collections.singletonList(computeDefaultQuickFix(tree));
  }

  private static Optional<JavaQuickFix> computeConciseQuickFix(BinaryExpressionTree tree) {
    if (tree.leftOperand().is(Tree.Kind.STRING_LITERAL)) {
      AnalyzerMessage.TextSpan interOperandSpace = AnalyzerMessage.textSpanBetween(
        tree.leftOperand().lastToken(), false,
        tree.rightOperand().firstToken(), false
      );
      List<JavaTextEdit> edits = new ArrayList<>();
      edits.add(JavaTextEdit.insertAfterTree(tree.rightOperand().lastToken(), ")"));
      edits.add(JavaTextEdit.replaceTextSpan(interOperandSpace, DOT_EQUALS_AND_OPENING_PARENTHESIS));
      if (tree.is(Tree.Kind.NOT_EQUAL_TO)) {
        edits.add(JavaTextEdit.insertBeforeTree(tree.leftOperand().firstToken(), "!"));
      }
      JavaQuickFix.Builder quickFix = JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
        .addTextEdit(JavaTextEdit.insertAfterTree(tree.rightOperand().lastToken(), ")"))
        .addTextEdit(JavaTextEdit.replaceTextSpan(interOperandSpace, DOT_EQUALS_AND_OPENING_PARENTHESIS));
      if (tree.is(Tree.Kind.NOT_EQUAL_TO)) {
        quickFix.addTextEdit(JavaTextEdit.insertBeforeTree(tree.leftOperand().firstToken(), "!"));
      }
      return Optional.of(quickFix.build());
    } else if (tree.rightOperand().is(Tree.Kind.STRING_LITERAL)) {
      String callEqualsOnLiteral = ((LiteralTree) tree.rightOperand()).value() + DOT_EQUALS_AND_OPENING_PARENTHESIS;
      String callToEquals = tree.is(Tree.Kind.NOT_EQUAL_TO) ? ("!" + callEqualsOnLiteral) : callEqualsOnLiteral;
      AnalyzerMessage.TextSpan leftOfOperatorToEndOfComparison = AnalyzerMessage.textSpanBetween(
        tree.leftOperand().lastToken(), false,
        tree.rightOperand().lastToken(), true
      );
      return Optional.of(
        JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
          .addTextEdit(JavaTextEdit.removeTextSpan(leftOfOperatorToEndOfComparison))
          .addTextEdit(JavaTextEdit.insertAfterTree(tree.leftOperand().lastToken(), ")"))
          .addTextEdit(JavaTextEdit.insertBeforeTree(tree.leftOperand().firstToken(), callToEquals))
          .build()
      );
    }
    return Optional.empty();
  }

  private static JavaQuickFix computeDefaultQuickFix(BinaryExpressionTree tree) {
    String callToEquals = tree.is(Tree.Kind.NOT_EQUAL_TO) ? "!java.util.Objects.equals(" : "java.util.Objects.equals(";
    AnalyzerMessage.TextSpan interOperandSpace = AnalyzerMessage.textSpanBetween(
      tree.leftOperand().lastToken(), false,
      tree.rightOperand().firstToken(), false
    );
    return JavaQuickFix.newQuickFix(QUICK_FIX_MESSAGE)
      .addTextEdit(JavaTextEdit.insertAfterTree(tree.rightOperand().lastToken(), ")"))
      .addTextEdit(JavaTextEdit.replaceTextSpan(interOperandSpace, ", "))
      .addTextEdit(JavaTextEdit.insertBeforeTree(tree.leftOperand().firstToken(), callToEquals))
      .build();
  }

}
