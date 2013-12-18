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
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(
  key = CollectionIsEmptyCheck.RULE_KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CollectionIsEmptyCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1155";
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

    if (hasCallToSizeMethod(tree) && isEmptyComparison(tree)) {
      context.addIssue(tree, RULE, "Use isEmpty() to check whether the collection is empty or not.");
    }
  }

  private static boolean hasCallToSizeMethod(BinaryExpressionTree tree) {
    return isCallToSizeMethod(tree.leftOperand()) ||
      isCallToSizeMethod(tree.rightOperand());
  }

  private static boolean isCallToSizeMethod(ExpressionTree tree) {
    if (!tree.is(Kind.METHOD_INVOCATION)) {
      return false;
    }

    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
    return methodInvocationTree.arguments().isEmpty() &&
      methodInvocationTree.methodSelect().is(Kind.MEMBER_SELECT) &&
      "size".equals(((MemberSelectExpressionTree) methodInvocationTree.methodSelect()).identifier().name());
  }

  private static boolean isEmptyComparison(BinaryExpressionTree tree) {
    boolean result;

    if (isEqualityExpression(tree)) {
      result = isZero(tree.leftOperand()) || isZero(tree.rightOperand());
    } else if (tree.is(Kind.GREATER_THAN_OR_EQUAL_TO) || tree.is(Kind.LESS_THAN)) {
      result = isZero(tree.leftOperand()) || isOne(tree.rightOperand());
    } else if (tree.is(Kind.GREATER_THAN) || tree.is(Kind.LESS_THAN_OR_EQUAL_TO)) {
      result = isOne(tree.leftOperand()) || isZero(tree.rightOperand());
    } else {
      result = false;
    }

    return result;
  }

  private static boolean isEqualityExpression(BinaryExpressionTree tree) {
    return tree.is(Kind.EQUAL_TO) ||
      tree.is(Kind.NOT_EQUAL_TO);
  }

  private static boolean isZero(ExpressionTree tree) {
    return tree.is(Kind.INT_LITERAL) &&
      "0".equals(((LiteralTree) tree).value());
  }

  private static boolean isOne(ExpressionTree tree) {
    return tree.is(Kind.INT_LITERAL) &&
      "1".equals(((LiteralTree) tree).value());
  }

}
