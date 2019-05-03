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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S2761")
public class DoublePrefixOperatorCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.LOGICAL_COMPLEMENT, Tree.Kind.BITWISE_COMPLEMENT, Tree.Kind.UNARY_PLUS, Tree.Kind.UNARY_MINUS);
  }

  private Set<ExpressionTree> prefixSet = new HashSet<>();

  @Override
  public void setContext(JavaFileScannerContext context) {
    prefixSet.clear();
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    UnaryExpressionTree exprTree = (UnaryExpressionTree) tree;
    if (alreadyReported(exprTree)) {
      return;
    }
    ExpressionTree expr = ExpressionUtils.skipParentheses(exprTree.expression());
    if (exprTree.is(expr.kind())) {
      UnaryExpressionTree child = (UnaryExpressionTree) expr;
      if (child.is(Tree.Kind.BITWISE_COMPLEMENT) && !ExpressionUtils.skipParentheses(child.expression()).is(Tree.Kind.BITWISE_COMPLEMENT)) {
        return;
      }
      prefixSet.add(child);
      reportIssue(exprTree.operatorToken(), child.operatorToken(), "Remove multiple operator prefixes.");
    }
  }

  private boolean alreadyReported(UnaryExpressionTree tree) {
    if (prefixSet.contains(tree)) {
      return true;
    }
    Tree parent = tree;
    while (parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION, Tree.Kind.BITWISE_COMPLEMENT, Tree.Kind.LOGICAL_COMPLEMENT, Tree.Kind.UNARY_PLUS, Tree.Kind.UNARY_MINUS)) {
      parent = parent.parent();
      if (prefixSet.contains(parent)) {
        return true;
      }
    }
    return false;
  }

}
