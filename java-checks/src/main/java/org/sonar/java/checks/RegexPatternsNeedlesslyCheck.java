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
import java.util.List;
import java.util.regex.Pattern;
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
  private static final Pattern SPLIT_EXCLUSION = Pattern.compile("[\\$\\.\\|\\(\\)\\[\\{\\^\\?\\*\\+\\\\]|\\\\\\w");

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
    ExpressionTree firstArgument = ExpressionUtils.skipParentheses(mit.arguments().get(0));
    if (isSplitMethod(mit) && firstArgument.is(Tree.Kind.STRING_LITERAL)) {
      String argValue = LiteralUtils.trimQuotes(((LiteralTree) firstArgument).value());
      if ((exceptionSplitMethod(argValue) &&
        !SPLIT_EXCLUSION.matcher(argValue).matches()) || metaCharactersInSplit(argValue)) {
        return;
      }
    }
    if (!storedInStaticFinal(mit) && (firstArgument.is(Tree.Kind.STRING_LITERAL) || isConstant(firstArgument))) {
      reportIssue(ExpressionUtils.methodName(mit), mit.arguments(), "Refactor this code to use a \"static final\" Pattern.");
    }
  }

  private static boolean storedInStaticFinal(MethodInvocationTree mit) {
    Tree tree = mit.parent();
    while (!tree.is(Kind.VARIABLE, Kind.CLASS, Kind.ASSIGNMENT, Kind.METHOD, Kind.ENUM)) {
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

  private static boolean metaCharactersInSplit(String argValue) {
    int strLength = argValue.length();
    return ((strLength == 3 && argValue.charAt(1) == '\\' && argValue.charAt(0) == '\\'
      && SPLIT_EXCLUSION.matcher(Character.toString(argValue.charAt(2))).matches()) ||
      (strLength == 4 && argValue.charAt(0) == '\\' && argValue.charAt(3) == '\\'));
  }

  private static boolean exceptionSplitMethod(String argValue) {
    int strLength = argValue.length();
    return strLength == 1 || (strLength == 2 && argValue.charAt(0) == '\\');
  }

  private static boolean isSplitMethod(MethodInvocationTree mit) {
    return mit.symbol().name().equals("split");
  }
}
