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

import com.google.common.collect.ImmutableMap;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Rule(key = "S1940")
public class BooleanInversionCheck extends IssuableSubscriptionVisitor {

  private static final Map<String, String> OPERATORS = ImmutableMap.<String, String>builder()
    .put("==", "!=")
    .put("!=", "==")
    .put("<", ">=")
    .put(">", "<=")
    .put("<=", ">")
    .put(">=", "<")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.LOGICAL_COMPLEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(((UnaryExpressionTree) tree).expression());
    if (expression.is(
        Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO,
        Tree.Kind.LESS_THAN, Tree.Kind.GREATER_THAN,
        Tree.Kind.LESS_THAN_OR_EQUAL_TO, Tree.Kind.GREATER_THAN_OR_EQUAL_TO)) {
      context.reportIssue(this, tree, "Use the opposite operator (\"" + OPERATORS.get(((BinaryExpressionTree) expression).operatorToken().text()) + "\") instead.");
    }
  }

}
