/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4248")
public class RegexPatternsNeedlesslyCheck extends AbstractMethodDetection {

  private static final String STRING = "java.lang.String";
  private static final String PATTERN = "java.util.regex.Pattern";
  private static final char[] array;

  static {
    array = ".$|()[{^?*\\".toCharArray();
    Arrays.sort(array);
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition(PATTERN).name("compile").addParameter(STRING),
      MethodMatcher.create().typeDefinition(STRING).name("matches").withAnyParameters(),
      MethodMatcher.create().typeDefinition(STRING).name("split").withAnyParameters(),
      MethodMatcher.create().typeDefinition(STRING).name("replaceAll").withAnyParameters(),
      MethodMatcher.create().typeDefinition(STRING).name("replaceFirst").withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree argument = mit.arguments().get(0);
    if (mit.symbolType().is(PATTERN)) {
      if (argument.is(Tree.Kind.NULL_LITERAL) || (checkArgs(argument) && invokedOnlyOnce(mit))) {
        return;
      }
    } else {
      if (!argument.is(Tree.Kind.STRING_LITERAL) ||
        ("split".equals(((MemberSelectExpressionTree) mit.methodSelect()).identifier().name()) && specialCaseNotReported(argument))) {
        return;
      }
    }
    reportIssue(mit, "Refactor this code to use a \"static final\" Pattern.");
  }

  private static boolean checkArgs(ExpressionTree argument) {
    return argument.is(Tree.Kind.STRING_LITERAL) || (argument.is(Tree.Kind.IDENTIFIER) && (((IdentifierTree) argument).symbol().isFinal()));

  }

  private static boolean invokedOnlyOnce(MethodInvocationTree mit) {
    Tree parentTree = mit.parent();
    while (!parentTree.is(Tree.Kind.METHOD, Tree.Kind.CLASS)) {
      if (parentTree.is(Tree.Kind.VARIABLE)) {
        return ((VariableTree) parentTree).symbol().isFinal();
      }
      parentTree = parentTree.parent();
    }
    return false;
  }

  private static boolean specialCaseNotReported(ExpressionTree argument) {
    String argString = ((LiteralTree) argument).value().replace("\"", "");
    int stringLength = argString.length();
    char arg1 = argString.charAt(0);
    if (stringLength == 1 || (stringLength == 2 && (arg1 == '\\' && argString.charAt(1) == '\\'))) {
      return Arrays.binarySearch(array, arg1) < 0;
    }
    char charToCheck = argString.charAt(1);
    if (charToCheck == '\\') {
      charToCheck = argString.charAt(2);
    }
    return (argString.charAt(0) == '\\' &&
      (!(Character.isLetter(charToCheck) || Character.isDigit(charToCheck))));
  }
}
