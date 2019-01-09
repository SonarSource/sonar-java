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

import com.google.common.collect.ImmutableSet;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Rule(key = "S2757")
public class WrongAssignmentOperatorCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> SUSPICIOUS_TOKEN_VALUES = ImmutableSet.of("!", "+", "-");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    AssignmentExpressionTree aeTree = (AssignmentExpressionTree) tree;
    SyntaxToken operatorToken = aeTree.operatorToken();
    SyntaxToken expressionFirstToken = aeTree.expression().firstToken();
    SyntaxToken variableLastToken = aeTree.variable().lastToken();
    if (isSuspiciousToken(expressionFirstToken)
      && noSpacingBetween(operatorToken, expressionFirstToken)
      && !noSpacingBetween(variableLastToken, operatorToken)) {
      reportIssue(operatorToken, expressionFirstToken, getMessage(expressionFirstToken, aeTree));
    }
  }

  private static String getMessage(SyntaxToken expressionFirstToken, AssignmentExpressionTree aeTree) {
    if (isSingleNegationAssignment(expressionFirstToken, aeTree)) {
      return "Add a space between \"=\" and \"!\" to avoid confusion.";
    }
    return "Was \"" + expressionFirstToken.text() + "=\" meant instead?";
  }

  private static boolean isSingleNegationAssignment(SyntaxToken firstToken, AssignmentExpressionTree aeTree) {
    return "!".equals(firstToken.text()) && (aeTree.parent() == null || !aeTree.parent().is(Tree.Kind.ASSIGNMENT));
  }

  private static boolean noSpacingBetween(SyntaxToken firstToken, SyntaxToken secondToken) {
    return firstToken.line() == secondToken.line()
      && firstToken.column() + firstToken.text().length() == secondToken.column();
  }

  private static boolean isSuspiciousToken(SyntaxToken firstToken) {
    return SUSPICIOUS_TOKEN_VALUES.contains(firstToken.text());
  }
}
