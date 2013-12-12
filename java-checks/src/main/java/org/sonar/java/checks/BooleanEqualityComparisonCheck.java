/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(
  key = BooleanEqualityComparisonCheck.RULE_KEY,
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class BooleanEqualityComparisonCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1125";
  private static final RuleKey RULE = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    scan(context.getTree());
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);

    String operator = operator(tree);
    if (operator != null && hasBooleanLiteralOperands(tree)) {
      addIssue(tree, operator);
    }
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    super.visitUnaryExpression(tree);

    String operator = operator(tree);
    if (operator != null && isBooleanLiteral(tree.expression())) {
      addIssue(tree, operator);
    }
  }

  private void addIssue(Tree tree, String operator) {
    context.addIssue(tree, RULE, "Remove the useless \"" + operator + "\" operator.");
  }

  private static boolean hasBooleanLiteralOperands(BinaryExpressionTree tree) {
    return isBooleanLiteral(tree.leftOperand()) ||
      isBooleanLiteral(tree.rightOperand());
  }

  private static boolean isBooleanLiteral(Tree tree) {
    return tree.is(Kind.BOOLEAN_LITERAL);
  }

  private static String operator(Tree tree) {
    String result;

    if (tree.is(Kind.EQUAL_TO)) {
      result = "==";
    } else if (tree.is(Kind.NOT_EQUAL_TO)) {
      result = "!=";
    } else if (tree.is(Kind.CONDITIONAL_AND)) {
      result = "&&";
    } else if (tree.is(Kind.CONDITIONAL_OR)) {
      result = "||";
    } else if (tree.is(Kind.LOGICAL_COMPLEMENT)) {
      result = "!";
    } else {
      result = null;
    }

    return result;
  }

}
