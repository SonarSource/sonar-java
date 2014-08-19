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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(
  key = ConcatenationWithStringValueOfCheck.RULE_KEY,
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class ConcatenationWithStringValueOfCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1153";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (!tree.is(Kind.PLUS)) {
      super.visitBinaryExpression(tree);
      return;
    }

    // TODO This code exploits the associativity bug SONARJAVA-610
    boolean seenStringLiteral = false;
    ExpressionTree current = tree;
    while (current.is(Kind.PLUS)) {
      BinaryExpressionTree binOp = (BinaryExpressionTree) current;
      scan(binOp.leftOperand());

      if (!seenStringLiteral) {
        if (binOp.leftOperand().is(Kind.STRING_LITERAL)) {
          seenStringLiteral = true;
          check(binOp.rightOperand());
        }
      } else if (isStringValueOf(binOp.leftOperand())) {
        check(binOp.leftOperand());
      }

      current = ((BinaryExpressionTree) current).rightOperand();
    }

    scan(current);
  }

  private void check(ExpressionTree tree) {
    if (isStringValueOf(tree)) {
      context.addIssue(tree, ruleKey, "Directly append the argument of String.valueOf().");
    }
  }

  private static boolean isStringValueOf(ExpressionTree tree) {
    if (!tree.is(Kind.METHOD_INVOCATION)) {
      return false;
    }

    return isStringValueOf((MethodInvocationTree) tree);
  }

  private static boolean isStringValueOf(MethodInvocationTree tree) {
    if (!tree.methodSelect().is(Kind.MEMBER_SELECT)) {
      return false;
    }

    return tree.arguments().size() == 1 &&
      isStringValueOf((MemberSelectExpressionTree) tree.methodSelect());
  }

  private static boolean isStringValueOf(MemberSelectExpressionTree tree) {
    if (!tree.expression().is(Kind.IDENTIFIER)) {
      return false;
    }

    return "valueOf".equals(tree.identifier().name()) &&
      "String".equals(((IdentifierTree) tree.expression()).name());
  }

}
