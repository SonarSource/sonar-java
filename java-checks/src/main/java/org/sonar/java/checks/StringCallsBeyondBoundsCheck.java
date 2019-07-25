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
import java.util.Optional;
import java.util.function.BiPredicate;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S3039")
public class StringCallsBeyondBoundsCheck extends AbstractMethodDetection {

  private static final String STRING = "java.lang.String";
  private static final MethodMatcher STRING_LENGTH =
    MethodMatcher.create().typeDefinition(STRING).name("length").withoutParameter();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(STRING).name("charAt").addParameter("int"),
      MethodMatcher.create().typeDefinition(STRING).name("codePointAt").addParameter("int"),
      MethodMatcher.create().typeDefinition(STRING).name("codePointBefore").addParameter("int"),
      MethodMatcher.create().typeDefinition(STRING).name("codePointCount").addParameter("int").addParameter("int"),
      MethodMatcher.create().typeDefinition(STRING).name("getChars").addParameter("int").addParameter("int").addParameter("char[]").addParameter("int"),
      MethodMatcher.create().typeDefinition(STRING).name("offsetByCodePoints").addParameter("int").addParameter("int"),
      MethodMatcher.create().typeDefinition(STRING).name("substring").addParameter("int"),
      MethodMatcher.create().typeDefinition(STRING).name("substring").addParameter("int").addParameter("int"),
      MethodMatcher.create().typeDefinition(STRING).name("subSequence").addParameter("int").addParameter("int")
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree invocation) {
    boolean issue;
    String method = invocation.symbol().name();
    switch (method) {
      case "charAt":
      case "codePointAt":
        issue = check(invocation, (str, args) ->
          isInvalidStringIndex(str, args.get(0)) ||
          isStringLength(str, args.get(0)));
        break;
      case "codePointBefore":
        issue = check(invocation, (str, args) ->
          isInvalidInclusiveStringIndex(str, args.get(0), 1));
        break;
      case "getChars":
        issue = check(invocation, (str, args) ->
          isInvalidInclusiveStringIndex(str, args.get(0))   ||
          isInvalidInclusiveStringIndex(str, args.get(1))   ||
          isInvalidIndex(args.get(3), 0, Integer.MAX_VALUE) ||
          isInverted(args.get(0), args.get(1)));
        break;
      case "offsetByCodePoints":
        issue = check(invocation, (str, args) ->
          isInvalidInclusiveStringIndex(str, args.get(0)));
        break;
      case "codePointCount":
      case "subSequence":
        issue = check(invocation, (str, args) ->
          isInvalidInclusiveStringIndex(str, args.get(0)) ||
          isInvalidInclusiveStringIndex(str, args.get(1)) ||
          isInverted(args.get(0), args.get(1)));
        break;
      case "substring":
        int arity = invocation.arguments().size();
        if (arity == 1) {
          issue = check(invocation, (str, args) ->
            isInvalidInclusiveStringIndex(str, args.get(0)));
        } else {
          issue = check(invocation, (str, args) ->
            isInvalidInclusiveStringIndex(str, args.get(0)) ||
            isInvalidInclusiveStringIndex(str, args.get(1)) ||
            isInverted(args.get(0), args.get(1)));
        }
        break;
      default:
        issue = false;
    }
    if (issue) {
      reportIssue(invocation, String.format("Refactor this \"%s\" call; it will result in an \"StringIndexOutOfBounds\" exception at runtime.", invocation.symbol().name()));
    }
  }

  private static boolean check(MethodInvocationTree invocation, BiPredicate<ExpressionTree, Arguments> condition) {
    if (invocation.methodSelect().is(Kind.MEMBER_SELECT)) {
      ExpressionTree string = ((MemberSelectExpressionTree) invocation.methodSelect()).expression();
      Arguments arguments = invocation.arguments();
      return condition.test(string, arguments);
    }
    return false;
  }

  private static boolean isInvalidIndex(ExpressionTree indexParam, int lowerBound, int upperBound) {
    return val(indexParam).map(idx -> idx < lowerBound || idx >= upperBound).orElse(false);
  }

  private static boolean isInvalidStringIndex(ExpressionTree str, ExpressionTree indexParam) {
    return len(str).map(strlen -> isInvalidIndex(indexParam, 0, strlen)).orElse(false);
  }

  private static boolean isInvalidInclusiveStringIndex(ExpressionTree str, ExpressionTree indexParam) {
    return isInvalidInclusiveStringIndex(str, indexParam, 0);
  }

  private static boolean isInvalidInclusiveStringIndex(ExpressionTree str, ExpressionTree indexParam, int lowerBound) {
    return len(str).map(strlen -> isInvalidIndex(indexParam, lowerBound, strlen + 1)).orElse(false);
  }

  private static boolean isInverted(ExpressionTree beginIndex, ExpressionTree endIndex) {
    return val(beginIndex).flatMap(x -> val(endIndex).map(y -> x > y)).orElse(false);
  }

  private static boolean isStringLength(ExpressionTree str, ExpressionTree tree) {
    if (str.is(Kind.IDENTIFIER) && tree.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree invocation = (MethodInvocationTree) tree;
      if (STRING_LENGTH.matches(invocation) && invocation.methodSelect().is(Kind.MEMBER_SELECT)) {
        ExpressionTree expr = ((MemberSelectExpressionTree) invocation.methodSelect()).expression();
        if (expr.is(Kind.IDENTIFIER)) {
          return ((IdentifierTree) str).symbol().equals(((IdentifierTree) expr).symbol());
        }
      }
    }
    return false;
  }

  private static Optional<Integer> val(ExpressionTree tree) {
    Optional<Integer> constant = cst(tree);
    if (constant.isPresent()) {
      return constant;
    } else {
      return len(tree);
    }
  }

  private static Optional<Integer> cst(ExpressionTree tree) {
    return Optional.ofNullable(ExpressionsHelper.getConstantValueAsInteger(tree).value());
  }

  private static Optional<String> str(ExpressionTree tree) {
    return Optional.ofNullable(ExpressionsHelper.getConstantValueAsString(tree).value());
  }

  private static Optional<Integer> len(ExpressionTree tree) {
    if (tree.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree invocation = (MethodInvocationTree) tree;
      if (STRING_LENGTH.matches(invocation)) {
        if (!invocation.methodSelect().is(Kind.MEMBER_SELECT)) {
          return Optional.empty();
        }
        tree = ((MemberSelectExpressionTree) invocation.methodSelect()).expression();
      }
    }
    return str(tree).map(String::length);
  }
}
