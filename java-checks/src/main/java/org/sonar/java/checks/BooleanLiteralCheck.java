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
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S1125")
public class BooleanLiteralCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO, Kind.CONDITIONAL_AND, Kind.CONDITIONAL_OR,
      Kind.LOGICAL_COMPLEMENT, Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree literal;
    if(tree.is(Kind.LOGICAL_COMPLEMENT)) {
      literal = getBooleanLiteral(((UnaryExpressionTree) tree).expression());
    } else if (tree.is(Kind.CONDITIONAL_EXPRESSION)) {
      ConditionalExpressionTree expression = (ConditionalExpressionTree) tree;
      literal = getBooleanLiteral(expression.trueExpression(), expression.falseExpression());
    } else {
      BinaryExpressionTree expression = (BinaryExpressionTree) tree;
      literal = getBooleanLiteral(expression.leftOperand(), expression.rightOperand());
    }
    if(literal != null) {
      reportIssue(literal, "Remove the literal \"" + literal.value() + "\" boolean value.");
    }
  }

  @Nullable
  private static LiteralTree getBooleanLiteral(Tree... trees) {
    return Arrays.stream(trees)
      .filter(tree -> tree.is(Kind.BOOLEAN_LITERAL))
      .map(LiteralTree.class::cast)
      .findFirst().orElse(null);
  }

}
