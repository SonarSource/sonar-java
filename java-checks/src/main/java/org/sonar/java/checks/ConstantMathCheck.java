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

import org.sonar.squidbridge.annotations.ActivatedByDefault;

import org.sonar.plugins.java.api.tree.LiteralTree;
import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

@Rule(
  key = "S2185",
  name = "Silly math should not be performed",
  tags = {"clumsy"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.CPU_EFFICIENCY)
@SqaleConstantRemediation("15min")
public class ConstantMathCheck extends SubscriptionBaseVisitor implements JavaFileScanner {

  private static final MethodInvocationMatcherCollection TRUNCATION_METHODS = MethodInvocationMatcherCollection.create(
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("ceil").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("ceil").addParameter("float"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("floor").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("floor").addParameter("float"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("round").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("round").addParameter("float")
    );

  private static final MethodInvocationMatcherCollection CONSTANT_WITH_ZERO_METHODS = MethodInvocationMatcherCollection.create(
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("atan2").addParameter("double").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("cos").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("cosh").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("expm1").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("sin").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("sinh").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("tan").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("tanh").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("toRadians").addParameter("double")
    );

  private static final MethodInvocationMatcherCollection CONSTANT_WITH_ZERO_OR_ONE_METHODS = MethodInvocationMatcherCollection.create(
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("acos").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("asin").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("atan").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("cbrt").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("exp").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("log").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("log10").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("sqrt").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("toDegrees").addParameter("double"),
    MethodInvocationMatcher.create().typeDefinition("java.lang.Math").name("exp").addParameter("double")
    );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.REMAINDER, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.REMAINDER)) {
        BinaryExpressionTree remainderTree = (BinaryExpressionTree) tree;
        if (isIntegralOne(remainderTree.rightOperand())) {
          addIssue(tree, "Remove this computation of % 1, which always evaluates to zero.");
        }
      } else {
        MethodInvocationTree methodTree = (MethodInvocationTree) tree;
        if (isTruncation(methodTree) || isConstantWithZero(methodTree) || isConstantWithZeroOrOne(methodTree)) {
          addIssue(tree, String.format("Remove this silly call to \"Math.%s\"", methodTree.symbol().name()));
        }
      }
    }
  }

  private boolean isTruncation(MethodInvocationTree methodTree) {
    return TRUNCATION_METHODS.anyMatch(methodTree) && isCastFromIntegralToFloating(removeParenthesis(methodTree.arguments().get(0)));
  }

  private boolean isConstantWithZero(MethodInvocationTree methodTree) {
    return CONSTANT_WITH_ZERO_METHODS.anyMatch(methodTree) && isFloatingZero(methodTree.arguments().get(0));
  }

  private boolean isConstantWithZeroOrOne(MethodInvocationTree methodTree) {
    return CONSTANT_WITH_ZERO_OR_ONE_METHODS.anyMatch(methodTree) && isFloatingZeroOrOne(methodTree.arguments().get(0));
  }

  private boolean isCastFromIntegralToFloating(ExpressionTree tree) {
    Type resultType = tree.symbolType();
    // explicit cast
    if (tree.is(Tree.Kind.TYPE_CAST) && isIntegral(getInnerType(((TypeCastTree) tree).expression())) && (resultType.is("double") || resultType.is("float"))) {
      return true;
    }
    // implicit cast
    return isIntegral(resultType);
  }

  private boolean isIntegral(Type type) {
    return type.isPrimitive() && !type.is("double") && !type.is("float");
  }

  private boolean isIntegralOne(ExpressionTree tree) {
    Long value = LiteralUtils.longLiteralValue(tree);
    return value != null && value == 1;
  }

  private Type getInnerType(ExpressionTree tree) {
    ExpressionTree result = tree;
    while (true) {
      if (result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
        result = ((ParenthesizedTree) result).expression();
      } else if (result.is(Tree.Kind.TYPE_CAST)) {
        result = ((TypeCastTree) result).expression();
      } else {
        return result.symbolType();
      }
    }
  }

  private ExpressionTree removeParenthesis(ExpressionTree tree) {
    ExpressionTree result = tree;
    while (result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      result = ((ParenthesizedTree) result).expression();
    }
    return result;
  }

  private boolean isFloatingZero(ExpressionTree tree) {
    Double value = parseDouble(removeParenthesis(tree));
    return value != null && value == 0.0d;
  }

  private boolean isFloatingZeroOrOne(ExpressionTree tree) {
    // current implementation of parseDouble returns null for all value but 0 and 1, so there is no need to check the actual returned value.
    return parseDouble(removeParenthesis(tree)) != null;
  }

  @CheckForNull
  private Double parseDouble(ExpressionTree tree) {
    if (tree.is(Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL)) {
      String value = ((LiteralTree) tree).value();
      if (value.equals("0.0") || value.equals("0.0d") || value.equals("0.0f")) {
        return 0.0d;
      } else if (value.equals("1.0") || value.equals("1.0d") || value.equals("1.0f")) {
        return 1.0d;
      }
    }
    return null;
  }

}
