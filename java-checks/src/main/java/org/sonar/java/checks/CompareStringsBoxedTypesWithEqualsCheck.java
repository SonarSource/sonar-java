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

import java.util.Collections;
import java.util.List;
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
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4973")
public class CompareStringsBoxedTypesWithEqualsCheck extends CompareWithEqualsVisitor {

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
        .withMessage("Strings and Boxed types should be compared using \"equals()\"")
        .withQuickFixes(() -> computeQuickFix(tree))
        .report();
    }
  }

  private static List<JavaQuickFix> computeQuickFix(BinaryExpressionTree tree) {
    String callToEquals = "java.util.Objects.equals(";
    if (tree.is(Tree.Kind.NOT_EQUAL_TO)) {
      callToEquals = "!" + callToEquals;
    }
    AnalyzerMessage.TextSpan interOperandSpace = AnalyzerMessage.textSpanBetween(
      tree.leftOperand().lastToken(), false,
      tree.rightOperand().firstToken(), false
    );
    return Collections.singletonList(
      JavaQuickFix.newQuickFix("Replace with boxed comparison")
        .addTextEdit(JavaTextEdit.insertAfterTree(tree.rightOperand().lastToken(), ")"))
        .addTextEdit(JavaTextEdit.replaceTextSpan(interOperandSpace, ", "))
        .addTextEdit(JavaTextEdit.insertBeforeTree(tree.leftOperand().firstToken(), callToEquals))
        .build()
    );
  }

  private static boolean isCompareWithBooleanConstant(ExpressionTree left, ExpressionTree right) {
    return ExpressionsHelper.getConstantValueAsBoolean(left).value() != null ||
      ExpressionsHelper.getConstantValueAsBoolean(right).value() != null;
  }

}
