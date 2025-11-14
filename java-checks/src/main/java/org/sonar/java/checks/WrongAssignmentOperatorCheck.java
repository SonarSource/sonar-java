/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2757")
public class WrongAssignmentOperatorCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> SUSPICIOUS_TOKEN_VALUES = SetUtils.immutableSetOf("!", "+", "-");

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
    return Position.endOf(firstToken).equals(Position.startOf(secondToken));
  }

  private static boolean isSuspiciousToken(SyntaxToken firstToken) {
    return SUSPICIOUS_TOKEN_VALUES.contains(firstToken.text());
  }
}
