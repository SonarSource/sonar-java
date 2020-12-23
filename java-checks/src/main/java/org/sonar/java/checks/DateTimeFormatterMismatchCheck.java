/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5917")
public class DateTimeFormatterMismatchCheck extends IssuableSubscriptionVisitor {
  private static final MethodMatchers OF_PATTERN_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.format.DateTimeFormatter")
    .names("ofPattern")
    .addParametersMatcher("java.lang.String")
    .addParametersMatcher("java.lang.String", "java.util.Locale")
    .build();

  private static final MethodMatchers APPEND_VALUE_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.format.DateTimeFormatterBuilder")
    .names("appendValue")
    .addParametersMatcher("java.time.temporal.TemporalField", "int")
    .build();

  private static final MethodMatchers DATE_TIME_FORMATTER_BUILDER = MethodMatchers.create()
    .ofTypes("java.time.format.DateTimeFormatterBuilder")
    .names("toFormatter")
    .addParametersMatcher()
    .build();

  private static final Pattern WEEK_PATTERN = Pattern.compile(".*ww{1,2}.*");
  private static final Pattern YEAR_OF_ERA_PATTERN = Pattern.compile(".*[uy]+.*");

  private static final String CHANGE_WEEK_FORMAT_MESSAGE = "Change this week format to use the week of week-based year instead.";
  private static final String CHANGE_YEAR_FORMAT_MESSAGE = "Change this year format to use the week-based year instead.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (OF_PATTERN_MATCHER.matches(invocation)) {
      visitPattern(invocation);
    } else if (DATE_TIME_FORMATTER_BUILDER.matches(invocation)) {
      visitBuildChain(invocation);
    }
  }

  private void visitPattern(MethodInvocationTree invocation) {
    Arguments arguments = invocation.arguments();
    ExpressionTree argument = arguments.get(0);
    if (argument.is(Tree.Kind.STRING_LITERAL)) {
      String pattern = ((LiteralTree) argument).value();
      if (WEEK_PATTERN.matcher(pattern).matches() && YEAR_OF_ERA_PATTERN.matcher(pattern).matches()) {
        reportIssue(invocation, CHANGE_YEAR_FORMAT_MESSAGE);
      }
    }
  }

  /**
   * Walking back through an invocation chain from a call to DateTimeFormatterBuilder.toFormatter looking for calls to appendValue.
   * If conflicting week and year settings are detected, an issue is reported
   * @param invocation A call to DateTimeFormatterBuilder.toFormatter
   */
  private void visitBuildChain(MethodInvocationTree invocation) {
    boolean usesWeekBasedYear = false;
    boolean usesWeekOfWeekBasedYear = false;
    Tree wanderer = invocation.methodSelect();
    while (wanderer != null && wanderer.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) wanderer).expression();
      if (!expression.is(Tree.Kind.METHOD_INVOCATION)) {
        break;
      }
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      if (APPEND_VALUE_MATCHER.matches(mit)) {
        usesWeekBasedYear |= isWeekBasedYearUsed(mit);
        usesWeekOfWeekBasedYear |= isWeekOfWeekBasedYearUsed(mit);
      }
      wanderer = mit.methodSelect();
    }
    if (wanderer == null) {
      return;
    }
    ExpressionTree lastExpression = ((MemberSelectExpressionTree) wanderer).expression();
    if (!lastExpression.is(Tree.Kind.NEW_CLASS)) {
      return;
    }
    if (usesWeekBasedYear && !usesWeekOfWeekBasedYear) {
      reportIssue(invocation, CHANGE_WEEK_FORMAT_MESSAGE);
    } else if (!usesWeekBasedYear && usesWeekOfWeekBasedYear) {
      reportIssue(invocation, CHANGE_YEAR_FORMAT_MESSAGE);
    }
  }

  private static boolean isWeekBasedYearUsed(MethodInvocationTree invocation) {
    Arguments arguments = invocation.arguments();
    ExpressionTree argument = arguments.get(0);
    if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree call = (MethodInvocationTree) argument;
      Symbol symbol = call.symbol();
      return symbol.name().equals("weekBasedYear");
    }
    return false;
  }

  private static boolean isWeekOfWeekBasedYearUsed(MethodInvocationTree invocation) {
    Arguments arguments = invocation.arguments();
    ExpressionTree argument = arguments.get(0);
    if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree call = (MethodInvocationTree) argument;
      Symbol symbol = call.symbol();
      return symbol.name().equals("weekOfWeekBasedYear");
    }
    return false;
  }
}
