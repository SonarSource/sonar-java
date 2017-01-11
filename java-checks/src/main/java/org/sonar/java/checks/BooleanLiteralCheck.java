/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.List;

@Rule(key = "S1125")
public class BooleanLiteralCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO, Kind.CONDITIONAL_AND, Kind.CONDITIONAL_OR, Kind.LOGICAL_COMPLEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree literal;
    if(tree.is(Kind.LOGICAL_COMPLEMENT)) {
      literal = getBooleanLiteral(((UnaryExpressionTree)tree).expression());
    } else {
      literal = getBooleanLiteralOperands((BinaryExpressionTree)tree);
    }
    if(literal != null) {
      reportIssue(literal, "Remove the literal \"" + literal.value() + "\" boolean value.");
    }
  }

  private static LiteralTree getBooleanLiteral(Tree tree) {
    LiteralTree result = null;
    if (tree.is(Kind.BOOLEAN_LITERAL)) {
      result = (LiteralTree) tree;
    }
    return result;
  }

  private static LiteralTree getBooleanLiteralOperands(BinaryExpressionTree tree) {
    LiteralTree result = getBooleanLiteral(tree.leftOperand());
    if (result == null) {
      result = getBooleanLiteral(tree.rightOperand());
    }
    return result;
  }
}
