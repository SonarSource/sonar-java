/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S2761")
public class DoublePrefixOperatorCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.LOGICAL_COMPLEMENT, Tree.Kind.BITWISE_COMPLEMENT, Tree.Kind.UNARY_PLUS, Tree.Kind.UNARY_MINUS);
  }

  static HashSet<ExpressionTree> prefixSet = new HashSet<>();

  @Override
  public void visitNode(Tree tree) {
    UnaryExpressionTree temp = (UnaryExpressionTree) tree;
    if (usedForDoublePrefix(temp)) {
      return;
    }
    ExpressionTree expr = ExpressionUtils.skipParentheses(temp.expression());
    if (temp.is(expr.kind())) {
      prefixSet.add(expr);
      reportIssue(temp.operatorToken(), ((UnaryExpressionTree) expr).operatorToken(), "Remove multiple operator prefixes.");
    }
  }

  private static boolean usedForDoublePrefix(UnaryExpressionTree tree) {
    if (prefixSet.contains(tree)) {
      return true;
    }
    Tree parent = tree;
    while (parent != null) {
      if (!parent.is(Tree.Kind.PARENTHESIZED_EXPRESSION, Tree.Kind.BITWISE_COMPLEMENT, Tree.Kind.LOGICAL_COMPLEMENT, Tree.Kind.UNARY_PLUS, Tree.Kind.UNARY_MINUS)) {
        return false;
      }
      parent = parent.parent();
      if (prefixSet.contains(parent)) {
        return true;
      }
    }
    return false;
  }
}
