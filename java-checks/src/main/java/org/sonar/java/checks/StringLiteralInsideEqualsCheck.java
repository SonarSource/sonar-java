/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonarsource.analyzer.commons.quickfixes.QuickFix;

@Rule(key = "S1132")
public class StringLiteralInsideEqualsCheck extends IssuableSubscriptionVisitor {

  private static final int MESSAGE_ARG_MAX_LENGTH = 10;

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    check((MethodInvocationTree) tree);
  }

  private void check(MethodInvocationTree tree) {
    if (tree.methodSelect() instanceof MemberSelectExpressionTree memberSelectExpressionTree && isEquals(tree)) {
      LiteralTree stringLiteral = (LiteralTree) tree.arguments().get(0);
      ExpressionTree leftSideMember = memberSelectExpressionTree.expression();
      if (!leftSideMember.is(Kind.STRING_LITERAL)) {
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(stringLiteral)
          .withMessage("Move the " + stringLiteral.value() + " string literal on the left side of this string comparison.")
          .withQuickFix(() -> computeQuickFix(stringLiteral, leftSideMember))
          .report();
      }
    }
  }

  private static boolean isEquals(MethodInvocationTree tree) {
    IdentifierTree identifier = ((MemberSelectExpressionTree) tree.methodSelect()).identifier();
    return isNamedEquals(identifier) && tree.arguments().size() == 1 && tree.arguments().get(0).is(Kind.STRING_LITERAL);
  }

  private static boolean isNamedEquals(IdentifierTree tree) {
    return "equals".equals(tree.name()) ||
      "equalsIgnoreCase".equals(tree.name());
  }

  private QuickFix computeQuickFix(LiteralTree equalsArgument, ExpressionTree leftSideMember) {
    String equalsParameterValue = QuickFixHelper.contentForTree(equalsArgument, context);
    String quickFixMessage = String.format("Move %s on the left side of .equals", cutTooLongString(equalsParameterValue));
    return QuickFix.newQuickFix(quickFixMessage)
      .addTextEdit(AnalyzerMessage.replaceTree(equalsArgument, QuickFixHelper.contentForTree(leftSideMember, context)))
      .addTextEdit(AnalyzerMessage.replaceTree(leftSideMember, equalsParameterValue))
      .build();
  }

  private static String cutTooLongString(String s) {
    return s.length() > MESSAGE_ARG_MAX_LENGTH ? (s.substring(0, MESSAGE_ARG_MAX_LENGTH) + "\"...") : s;
  }

}
