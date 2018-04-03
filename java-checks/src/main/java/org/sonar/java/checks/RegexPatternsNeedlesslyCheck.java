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
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
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
    ExpressionTree argument = mit.arguments().get(0);
    if (mit.symbol().name().equals("split") && argument.is(Tree.Kind.STRING_LITERAL)) {
      String argValue = LiteralUtils.trimQuotes(((LiteralTree) argument).value());
      int strLength = argValue.length();
      if ((strLength == 1 || (strLength == 2 && argValue.charAt(0) == '\\')) &&
        !SPLIT_EXCLUSION.matcher(argValue).matches()) {
        return;
      }
    }
    if (!storedInStaticFinal(mit) && (argument.is(Tree.Kind.STRING_LITERAL) || isConstant(argument))) {
      reportIssue(mit, "Refactor this code to use a \"static final\" Pattern.");
    }
  }

  private static boolean storedInStaticFinal(MethodInvocationTree mit) {
    Tree tree = mit.parent();
    while (!tree.is(Tree.Kind.VARIABLE, Tree.Kind.CLASS, Tree.Kind.ASSIGNMENT)) {
      tree = tree.parent();
    }
    if (tree.is(Tree.Kind.CLASS)) {
      return false;
    } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
      return isConstant(((AssignmentExpressionTree) tree).variable());
    }
    VariableTree variableTree = (VariableTree) tree;
    return variableTree.symbol().isStatic() && variableTree.symbol().isFinal();
  }

  private static boolean isConstant(ExpressionTree expr) {
    IdentifierTree identifierTree = null;
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      identifierTree = (IdentifierTree) expr;
    } else if (expr.is(Tree.Kind.MEMBER_SELECT)) {
      identifierTree = ((MemberSelectExpressionTree) expr).identifier();
    }
    return identifierTree != null && identifierTree.symbol().isFinal() && identifierTree.symbol().isStatic();
  }
}
