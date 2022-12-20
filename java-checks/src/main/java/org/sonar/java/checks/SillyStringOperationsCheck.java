/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static java.lang.String.format;

@Rule(key = "S2121")
public class SillyStringOperationsCheck extends AbstractMethodDetection {

  private static final String CHAR_SEQUENCE = "java.lang.CharSequence";
  private static final String STRING = "java.lang.String";
  private static final MethodMatchers STRING_LENGTH = MethodMatchers.create()
    .ofTypes(STRING).names("length").addWithoutParametersMatcher().build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofTypes(STRING)
        .names("contains")
        .addParametersMatcher(params -> params.size() == 1 && params.get(0).isSubtypeOf(CHAR_SEQUENCE))
        .build(),
      MethodMatchers.create().ofTypes(STRING)
        .names("compareTo", "compareToIgnoreCase", "endsWith", "indexOf", "lastIndexOf", "matches", "split", "startsWith")
        .addParametersMatcher(STRING)
        .build(),
      MethodMatchers.create().ofTypes(STRING)
        .names("replaceFirst")
        .addParametersMatcher(STRING, STRING)
        .build(),
      MethodMatchers.create().ofTypes(STRING)
        .names("indexOf", "lastIndexOf", "split", "startsWith")
        .addParametersMatcher(STRING, "int")
        .build(),
      MethodMatchers.create()
        .ofTypes(STRING)
        .names("substring")
        .addParametersMatcher("int")
        .addParametersMatcher("int", "int")
        .build());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    if (tree.methodSelect().is(Kind.MEMBER_SELECT)) {
      boolean issue;
      ExpressionTree str = ((MemberSelectExpressionTree) tree.methodSelect()).expression();
      Arguments args = tree.arguments();
      String method = tree.methodSymbol().name();
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
    return tree.asConstant(Integer.class).filter(n -> n == 0).isPresent();
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
    return tree.asConstant(String.class).orElse(null);
  }
}
