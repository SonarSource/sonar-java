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
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(
  key = BooleanLiteralCheck.RULE_KEY,
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class BooleanLiteralCheck extends BaseTreeVisitor implements JavaFileScanner {

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
    String literal = getBooleanLiteralOperands(tree);
    if (hasCorrectOperator(tree) && literal != null) {
      addIssue(tree, literal);
    }
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    super.visitUnaryExpression(tree);
    String literal = getBooleanLiteral(tree.expression());
    if ( hasCorrectOperator(tree) && literal != null) {
      addIssue(tree, literal);
    }
  }

  private void addIssue(Tree tree, String literal) {
    context.addIssue(tree, RULE, "Remove the literal \"" + literal + "\" boolean value.");
  }

  private static String getBooleanLiteralOperands(BinaryExpressionTree tree) {
    String result = getBooleanLiteral(tree.leftOperand());
    if (result == null) {
      result = getBooleanLiteral(tree.rightOperand());
    }
    return result;
  }

  private static String getBooleanLiteral(Tree tree) {
    String result = null;
    if (tree.is(Kind.BOOLEAN_LITERAL)) {
      result = ((LiteralTree) tree).value();
    }
    return result;
  }

  private static boolean hasCorrectOperator(Tree tree) {
    return tree.is(Kind.EQUAL_TO) ||
      tree.is(Kind.NOT_EQUAL_TO) ||
      tree.is(Kind.CONDITIONAL_AND) ||
      tree.is(Kind.CONDITIONAL_OR) ||
      tree.is(Kind.LOGICAL_COMPLEMENT);
  }

}
