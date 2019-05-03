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
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import javax.annotation.CheckForNull;
import java.util.Arrays;
import java.util.List;

@Rule(key = "S2676")
public class AbsOnNegativeCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcherCollection MATH_ABS_METHODS = MethodMatcherCollection.create(
    MethodMatcher.create()
      .typeDefinition("java.lang.Math")
      .name("abs")
      .addParameter("int"),
    MethodMatcher.create()
      .typeDefinition("java.lang.Math")
      .name("abs")
      .addParameter("long")
  );

  private static final MethodMatcherCollection NEGATIVE_METHODS = MethodMatcherCollection.create(
    MethodMatcher.create()
      .name("hashCode")
      .withoutParameter(),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.util.Random"))
      .name("nextInt")
      .withoutParameter(),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.util.Random"))
      .name("nextLong")
      .withoutParameter(),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.lang.Comparable"))
      .name("compareTo")
      .addParameter(TypeCriteria.anyType())
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.UNARY_MINUS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodTree = (MethodInvocationTree) tree;
      if (MATH_ABS_METHODS.anyMatch(methodTree)) {
        ExpressionTree firstArgument = methodTree.arguments().get(0);
        checkForIssue(firstArgument);
      }
    } else {
      ExpressionTree operand = ((UnaryExpressionTree) tree).expression();
      checkForIssue(operand);
    }
  }

  private void checkForIssue(ExpressionTree tree) {
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      Symbol identifierSymbol = ((MemberSelectExpressionTree) tree).identifier().symbol();
      Type ownerType = identifierSymbol.owner().type();
      if ("MIN_VALUE".equals(identifierSymbol.name()) && (ownerType.is("java.lang.Integer") || ownerType.is("java.lang.Long"))) {
        reportIssue(tree, "Use the original value instead.");
      }
    } else {
      MethodInvocationTree nestedTree = extractMethodInvocation(tree);
      if (nestedTree != null && NEGATIVE_METHODS.anyMatch(nestedTree)) {
        reportIssue(nestedTree, "Use the original value instead.");
      }
    }
  }

  @CheckForNull
  private static MethodInvocationTree extractMethodInvocation(ExpressionTree tree) {
    ExpressionTree result = tree;
    while (true) {
      if (result.is(Tree.Kind.TYPE_CAST)) {
        result = ((TypeCastTree) result).expression();
      } else if (result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
        result = ((ParenthesizedTree) result).expression();
      } else if (result.is(Tree.Kind.METHOD_INVOCATION)) {
        return (MethodInvocationTree) result;
      } else {
        return null;
      }
    }
  }

}
