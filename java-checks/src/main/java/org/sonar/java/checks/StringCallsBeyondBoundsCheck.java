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

import javax.annotation.CheckForNull;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
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
        issue = checkCodePointAt(invocation);
        break;
      case "codePointBefore":
        issue = checkCodePointBefore(invocation);
        break;
      case "getChars":
        issue = checkGetChars(invocation);
        break;
      case "offsetByCodePoints":
        issue = checkOffsetByCodePoints(invocation);
        break;
      case "codePointCount":
      case "subSequence":
        issue = checkSubsequence(invocation);
        break;
      case "substring":
        issue = checkSubstring(invocation);
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

  private static boolean checkCodePointAt(MethodInvocationTree tree) {
    return check(tree, (str, args) -> {
      Integer index = constant(args.get(0));
      if (index != null && index < 0) {
        return true;
      }
      Integer strlen = length(str);
      if (index != null && strlen != null && index >= strlen) {
        return true;
      }
      return isStringLength(str, args.get(0));
    });
  }

  private static boolean checkCodePointBefore(MethodInvocationTree tree) {
    return check(tree, (str, args) -> {
      Integer index = constant(args.get(0));
      if (index != null && index < 1) {
        return true;
      }
      Integer strlen = length(str);
      return index != null && strlen != null && index > strlen;
    });
  }

  private static boolean checkGetChars(MethodInvocationTree tree) {
    return check(tree, (str, args) -> {
      Integer srcBegin = constant(args.get(0));
      if (srcBegin != null && srcBegin < 0) {
        return true;
      }
      Integer srcEnd = constant(args.get(1));
      if (srcBegin != null && srcEnd != null && srcBegin > srcEnd) {
        return true;
      }
      Integer strlen = length(str);
      if (srcEnd != null && strlen != null && srcEnd > strlen) {
        return true;
      }
      Integer dstBegin = constant(args.get(3));
      return dstBegin != null && dstBegin < 0;
    });
  }

  private static boolean checkOffsetByCodePoints(MethodInvocationTree tree) {
    return check(tree, (str, args) -> {
      Integer index = constant(args.get(0));
      if (index != null && index < 0) {
        return true;
      }
      Integer strlen = length(str);
      return index != null && strlen != null && index > strlen;
    });
  }

  private static boolean checkSubstring(MethodInvocationTree tree) {
    int arity = tree.arguments().size();
    if (arity == 2) {
      return checkSubsequence(tree);
    }
    return check(tree, (str, args) -> {
      Integer index = constant(args.get(0));
      if (index != null && index < 0) {
        return true;
      }
      Integer strlen = length(str);
      return index != null && strlen != null && index > strlen;
    });
  }

  private static boolean checkSubsequence(MethodInvocationTree tree) {
    return check(tree, (str, args) -> {
      Integer beginIndex = constant(args.get(0));
      if (beginIndex != null && beginIndex < 0) {
        return true;
      }
      Integer endIndex = constant(args.get(1));
      if (beginIndex != null && endIndex != null && beginIndex > endIndex) {
        return true;
      }
      Integer strlen = length(str);
      return endIndex != null && strlen != null && endIndex > strlen;
    });
  }

  private static boolean isStringLength(ExpressionTree str, ExpressionTree tree) {
    if (str.is(Kind.IDENTIFIER) && tree.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree invocation = (MethodInvocationTree) tree;
      if (STRING_LENGTH.matches(invocation) && invocation.methodSelect().is(Kind.MEMBER_SELECT)) {
        ExpressionTree expr = ((MemberSelectExpressionTree) invocation.methodSelect()).expression();
        return expr.is(Kind.IDENTIFIER) && ((IdentifierTree) str).symbol().equals(((IdentifierTree) expr).symbol());
      }
    }
    return false;
  }

  @CheckForNull
  private static Integer constant(ExpressionTree tree) {
    return ConstantUtils.resolveAsIntConstant(tree);
  }

  @CheckForNull
  private static String string(ExpressionTree tree) {
    return ConstantUtils.resolveAsStringConstant(tree);
  }

  @CheckForNull
  private static Integer length(ExpressionTree tree) {
    return Optional.ofNullable(string(tree)).map(String::length).orElse(null);
  }
}
