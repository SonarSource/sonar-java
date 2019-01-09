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

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "S4142")
public class DuplicateArgumentCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    Arguments arguments = mit.arguments();
    int arity = arguments.size();
    if (arity <= 1) {
      return;
    }
    Set<ExpressionTree> reported = new HashSet<>();
    for (int i = 0; i < arity; i++) {
      ExpressionTree arg = ExpressionUtils.skipParentheses(arguments.get(i));
      if (isLiteral(arg) || arg.is(Tree.Kind.IDENTIFIER) || arg.is(Tree.Kind.NEW_CLASS)) {
        continue;
      }
      for (int j = i + 1; j < arity; j++) {
        ExpressionTree otherArg = ExpressionUtils.skipParentheses(arguments.get(j));
        if (!reported.contains(otherArg) && SyntacticEquivalence.areEquivalent(arg, otherArg)) {
          reportIssue(
            otherArg,
            String.format("Verify that this is the intended value; it is the same as the %s argument.", argumentNumber(i + 1)),
            Collections.singletonList(new JavaFileScannerContext.Location("", arg)),
            null);
          reported.add(otherArg);
        }
      }
    }
  }

  private static boolean isLiteral(ExpressionTree arg) {
    if (arg.is(Tree.Kind.TYPE_CAST)) {
      return isLiteral(((TypeCastTree) arg).expression());
    }
    if (arg instanceof UnaryExpressionTree) {
      return isLiteral(((UnaryExpressionTree) arg).expression());
    }
    return arg.is(
      Tree.Kind.BOOLEAN_LITERAL,
      Tree.Kind.CHAR_LITERAL,
      Tree.Kind.DOUBLE_LITERAL,
      Tree.Kind.FLOAT_LITERAL,
      Tree.Kind.INT_LITERAL,
      Tree.Kind.LONG_LITERAL,
      Tree.Kind.NULL_LITERAL,
      Tree.Kind.STRING_LITERAL);
  }

  private static String argumentNumber(int index) {
    switch (index) {
      case 1:
        return "1st";
      case 2:
        return "2nd";
      case 3:
        return "3rd";
      default:
        return index + "th";
    }
  }
}
