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

import static java.lang.String.format;
import java.util.Arrays;
import java.util.List;

import javax.annotation.CheckForNull;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2121")
public class SillyStringOperationsCheck extends AbstractMethodDetection {

  private static final String CHAR_SEQUENCE = "java.lang.CharSequence";
  private static final String STRING = "java.lang.String";
  private static final MethodMatcher STRING_LENGTH = MethodMatcher.create().typeDefinition(STRING).name("length").withoutParameter();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
        MethodMatcher.create().typeDefinition(STRING).name("contains")
          .addParameter(TypeCriteria.subtypeOf(CHAR_SEQUENCE)),
        MethodMatcher.create().typeDefinition(STRING).name("compareTo")
          .addParameter(TypeCriteria.is(STRING)),
        MethodMatcher.create().typeDefinition(STRING).name("compareToIgnoreCase")
          .addParameter(TypeCriteria.is(STRING)),
        MethodMatcher.create().typeDefinition(STRING).name("endsWith")
          .addParameter(TypeCriteria.is(STRING)),
        MethodMatcher.create().typeDefinition(STRING).name("indexOf")
          .addParameter(TypeCriteria.is(STRING)),
        MethodMatcher.create().typeDefinition(STRING).name("indexOf")
          .addParameter(TypeCriteria.is(STRING))
          .addParameter("int"),
        MethodMatcher.create().typeDefinition(STRING).name("lastIndexOf")
          .addParameter(TypeCriteria.is(STRING)),
        MethodMatcher.create().typeDefinition(STRING).name("lastIndexOf")
          .addParameter(TypeCriteria.is(STRING))
          .addParameter("int"),
        MethodMatcher.create().typeDefinition(STRING).name("matches")
          .addParameter(TypeCriteria.is(STRING)),
        MethodMatcher.create().typeDefinition(STRING).name("replaceFirst")
          .addParameter(TypeCriteria.is(STRING))
          .addParameter(TypeCriteria.is(STRING)),
        MethodMatcher.create().typeDefinition(STRING).name("split")
          .addParameter(TypeCriteria.is(STRING)),
        MethodMatcher.create().typeDefinition(STRING).name("split")
          .addParameter(TypeCriteria.is(STRING))
          .addParameter("int"),
        MethodMatcher.create().typeDefinition(STRING).name("startsWith")
          .addParameter(TypeCriteria.is(STRING)),
        MethodMatcher.create().typeDefinition(STRING).name("startsWith")
          .addParameter(TypeCriteria.is(STRING))
          .addParameter("int"),
        MethodMatcher.create().typeDefinition(STRING).name("substring")
          .addParameter("int"),
        MethodMatcher.create().typeDefinition(STRING).name("substring")
          .addParameter("int")
          .addParameter("int"));
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    if (tree.methodSelect().is(Kind.MEMBER_SELECT)) {
      boolean issue;
      ExpressionTree str = ((MemberSelectExpressionTree) tree.methodSelect()).expression();
      Arguments args = tree.arguments();
      String method = tree.symbol().name();
      switch (method) {
        case "contains":
        case "compareTo":
        case "compareToIgnoreCase":
        case "endsWith":
        case "indexOf":
        case "lastIndexOf":
        case "matches":
        case "split":
        case "startsWith":
          issue = checkStartsWith(str, args);
          break;
        case "replaceFirst":
          issue = checkReplaceFirst(str, args);
          break;
        case "substring":
          issue = checkSubstring(str, args);
          break;
        default:
          issue = false;
      }
      if (issue) {
        reportIssue(tree, format("Remove this \"%s\" call; it has predictable results.", method));
      }
    }
  }

  private static boolean checkStartsWith(ExpressionTree str, Arguments args) {
    return isSameString(str, args.get(0));
  }

  private static boolean checkReplaceFirst(ExpressionTree str, Arguments args) {
    return isSameString(str, args.get(0)) || isSameString(args.get(0), args.get(1));
  }

  private static boolean checkSubstring(ExpressionTree str, Arguments args) {
    if (args.size() == 1) {
      return isZero(args.get(0)) || isStringLength(str, args.get(0));
    } else {
      return isStringLength(str, args.get(0)) || (isZero(args.get(0)) && isStringLength(str, args.get(1)));
    }
  }

  private static boolean isSameString(ExpressionTree str, ExpressionTree tree) {
    return isSameSymbol(str, tree) || isSameStringLiteral(str, tree);
  }

  private static boolean isSameSymbol(ExpressionTree tree, ExpressionTree other) {
    Symbol s = symbol(tree);
    return s != null && s.equals(symbol(other));
  }

  private static boolean isSameStringLiteral(ExpressionTree str, ExpressionTree tree) {
    String s = string(str);
    return s != null && s.equals(string(tree));
  }

  private static boolean isZero(ExpressionTree tree) {
    Integer n = ConstantUtils.resolveAsIntConstant(tree);
    return n != null && n == 0;
  }

  private static boolean isStringLength(ExpressionTree str, ExpressionTree tree) {
    if (tree.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree invocation = (MethodInvocationTree) tree;
      if (STRING_LENGTH.matches(invocation) && invocation.methodSelect().is(Kind.MEMBER_SELECT)) {
        return isSameSymbol(str, ((MemberSelectExpressionTree) invocation.methodSelect()).expression());
      }
    }
    return false;
  }

  @CheckForNull
  private static Symbol symbol(ExpressionTree tree) {
    if (tree.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).symbol();
    }
    return null;
  }

  @CheckForNull
  private static String string(ExpressionTree tree) {
    return ConstantUtils.resolveAsStringConstant(tree);
  }
}
