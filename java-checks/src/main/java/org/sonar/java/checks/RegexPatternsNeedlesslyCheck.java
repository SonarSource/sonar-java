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
import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4248")
public class RegexPatternsNeedlesslyCheck extends AbstractMethodDetection {

  private static final String STRING = "java.lang.String";
  private static final String PATTERN = "java.util.regex.Pattern";
  private static final MethodMatcher SPLIT_MATCHER = MethodMatcher.create().typeDefinition(STRING).name("split").withAnyParameters();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(PATTERN).name("compile").addParameter(STRING),
      MethodMatcher.create().typeDefinition(STRING).name("matches").withAnyParameters(),
      SPLIT_MATCHER,
      MethodMatcher.create().typeDefinition(STRING).name("replaceAll").withAnyParameters(),
      MethodMatcher.create().typeDefinition(STRING).name("replaceFirst").withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArgument = ExpressionUtils.skipParentheses(mit.arguments().get(0));
    if (SPLIT_MATCHER.matches(mit) && firstArgument.is(Tree.Kind.STRING_LITERAL)) {
      String argValue = LiteralUtils.trimQuotes(((LiteralTree) firstArgument).value());
      if (exceptionSplitMethod(argValue)) {
        return;
      }
    }
    if (!storedInStaticFinal(mit) && (firstArgument.is(Tree.Kind.STRING_LITERAL) || isConstant(firstArgument))) {
      reportIssue(ExpressionUtils.methodName(mit), mit.arguments(), "Refactor this code to use a \"static final\" Pattern.");
    }
  }

  private static boolean storedInStaticFinal(MethodInvocationTree mit) {
    Tree tree = mit.parent();
    while (!tree.is(Kind.VARIABLE, Kind.ASSIGNMENT, Kind.COMPILATION_UNIT)) {
      tree = tree.parent();
    }
    return isConstant(tree);
  }

  private static boolean isConstant(Tree tree) {
    Symbol symbol = null;
    switch (tree.kind()) {
      case IDENTIFIER:
        symbol = ((IdentifierTree) tree).symbol();
        break;
      case MEMBER_SELECT:
        symbol = (((MemberSelectExpressionTree) tree).identifier()).symbol();
        break;
      case VARIABLE:
        symbol = ((VariableTree) tree).symbol();
        break;
      case ASSIGNMENT:
        return isConstant(((AssignmentExpressionTree) tree).variable());
      default:
        break;
    }
    return symbol != null && symbol.isFinal() && symbol.isStatic();
  }

  /**
   * Following code is copy of actual {@link java.lang.String#split(String, int)} condition for fastpath
   * Condition is checking for one of the following cases:
   *
   * (1) one-char String and this character is not one of the RegEx's meta characters ".$|()[{^?*+\\", or
   * (2) two-char String and the first char is the backslash and the second is not the ascii digit or ascii letter.
   *
   */
  private static boolean exceptionSplitMethod(String argValue) {
    String regex = StringEscapeUtils.unescapeJava(argValue);
    char ch;
    return ((regex.length() == 1 && ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1) ||
      (regex.length() == 2 &&
        regex.charAt(0) == '\\' &&
        (((ch = regex.charAt(1)) - '0') | ('9' - ch)) < 0 &&
        ((ch - 'a') | ('z' - ch)) < 0 &&
        ((ch - 'A') | ('Z' - ch)) < 0)) &&
      (ch < Character.MIN_HIGH_SURROGATE || ch > Character.MAX_LOW_SURROGATE);
  }

}
