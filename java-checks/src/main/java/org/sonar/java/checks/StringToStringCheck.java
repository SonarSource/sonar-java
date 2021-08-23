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

import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@Rule(key = "S1858")
public class StringToStringCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.lang.String")
      .names("toString")
      .addWithoutParametersMatcher()
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    ExpressionTree expressionTree = extractBaseExpression(((MemberSelectExpressionTree) tree.methodSelect()).expression());
    Tree reportTree = null;
    String message = null;
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      reportTree = expressionTree;
      message = String.format("\"%s\" is already a string, there's no need to call \"toString()\" on it.",
        ((IdentifierTree) expressionTree).identifierToken().text());
    } else if (expressionTree.is(Tree.Kind.STRING_LITERAL)) {
      reportTree = expressionTree;
      message = "there's no need to call \"toString()\" on a string literal.";
    } else if (expressionTree.is(Tree.Kind.TEXT_BLOCK)) {
      reportTree = expressionTree;
      message =  "there's no need to call \"toString()\" on a text block.";
    } else if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      IdentifierTree methodName = ExpressionUtils.methodName((MethodInvocationTree) expressionTree);
      reportTree = methodName;
      message = "\"" + methodName + "\" returns a string, there's no need to call \"toString()\".";
    } else if (expressionTree.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
      ArrayAccessExpressionTree arrayAccess = (ArrayAccessExpressionTree) expressionTree;
      IdentifierTree name = extractName(arrayAccess.expression());
      if (name == null) {
        reportTree = arrayAccess.expression();
        message = "There's no need to call \"toString()\" on an array of String.";
      } else {
        reportTree = name;
        message = String.format("\"%s\" is an array of strings, there's no need to call \"toString()\".", name.identifierToken().text());
      }
    }
    reportIssue(reportTree, message, tree, expressionTree);
  }

  private void reportIssue(@Nullable Tree reportTree, @Nullable String message, MethodInvocationTree toStringInvocation, ExpressionTree baseExpression) {
    if (reportTree != null) {
      Objects.requireNonNull(message, "Message should always be set with a report tree.");
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(reportTree)
        .withMessage(message)
        .withQuickFix(() -> JavaQuickFix.newQuickFix("Remove \"toString()\"")
          .addTextEdit(JavaTextEdit.removeTextSpan(textSpanBetween(baseExpression, false, toStringInvocation, true)))
          .build())
        .report();
    }
  }

  private static ExpressionTree extractBaseExpression(ExpressionTree tree) {
    ExpressionTree expressionTree = tree;
    while (true) {
      if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
        expressionTree = ((MemberSelectExpressionTree) expressionTree).identifier();
      } else if (expressionTree.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
        expressionTree = ((ParenthesizedTree) expressionTree).expression();
      } else if (expressionTree.is(Tree.Kind.TYPE_CAST)) {
        expressionTree = ((TypeCastTree) expressionTree).expression();
      } else {
        return expressionTree;
      }
    }
  }

  private static IdentifierTree extractName(ExpressionTree tree) {
    ExpressionTree expressionTree = extractBaseExpression(tree);
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) expressionTree;
    }
    return null;
  }

}
