/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

@Rule(
  key = "S2676",
  name = "Neither \"Math.abs\" nor negation should be used on numbers that could be \"MIN_VALUE\"",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class AbsOnNegativeCheck extends SubscriptionBaseVisitor implements JavaFileScanner {

  private static final MethodInvocationMatcherCollection MATH_ABS_METHODS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create()
      .typeDefinition("java.lang.Math")
      .name("abs")
      .addParameter("int"),
    MethodMatcher.create()
      .typeDefinition("java.lang.Math")
      .name("abs")
      .addParameter("long")
    );

  private static final MethodInvocationMatcherCollection NEGATIVE_METHODS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create()
      .name("hashCode"),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.util.Random"))
      .name("nextInt"),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.util.Random"))
      .name("nextLong"),
    MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.lang.Comparable"))
      .name("compareTo")
      .addParameter(TypeCriteria.anyType())
    );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.UNARY_MINUS);
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
        addIssue(tree, "Use the original value instead.");
      }
    } else {
      MethodInvocationTree nestedTree = extractMethodInvocation(tree);
      if (nestedTree != null && NEGATIVE_METHODS.anyMatch(nestedTree)) {
        addIssue(nestedTree, "Use the original value instead.");
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
