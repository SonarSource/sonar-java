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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

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

  private static final MethodMatchers WEEK_BASED_YEAR_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.temporal.WeekFields")
    .names("weekBasedYear")
    .addParametersMatcher()
    .build();

  private static final MethodMatchers WEEK_OF_WEEK_BASED_YEAR_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.temporal.WeekFields")
    .names("weekOfWeekBasedYear")
    .addParametersMatcher()
    .build();

  private static final Pattern WEEK_PATTERN = Pattern.compile(".*ww{1,2}.*");
  private static final Pattern YEAR_OF_ERA_PATTERN = Pattern.compile(".*[uy]+.*");

  private static final String CHANGE_YEAR_FORMAT_WEEK_BASED_MESSAGE = "Change this year format to use the week-based year instead.";
  private static final String CHANGE_YEAR_FORMAT_TO_CHRONOFIELD_MESSAGE = "Change this year format to use ChronoField.YEAR instead.";

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

  /**
   * We currently only check formatters initialized with a String literal or the value of a final variable.
   */
  private void visitPattern(MethodInvocationTree invocation) {
    Arguments arguments = invocation.arguments();
    ExpressionTree argument = arguments.get(0);
    if (argument == null) {
      return;
    }
    if (argument.is(Tree.Kind.STRING_LITERAL)) {
      String pattern = ((LiteralTree) argument).value();
      if (isInfringingPattern(pattern)) {
        reportIssue(invocation, CHANGE_YEAR_FORMAT_WEEK_BASED_MESSAGE);
      }
    } else if (argument.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) argument;
      Symbol symbol = identifier.symbol();
      if (!symbol.isFinal()) {
        return;
      }
      Tree declaration = symbol.declaration();
      if (declaration == null || !declaration.is(Tree.Kind.VARIABLE)) {
        return;
      }
      VariableTree variable = (VariableTree) declaration;
      ExpressionTree initializer = variable.initializer();
      if (initializer == null || !initializer.is(Tree.Kind.STRING_LITERAL)) {
        return;
      }
      String pattern = ((LiteralTree) initializer).value();
      if (isInfringingPattern(pattern)) {
        reportIssue(invocation, CHANGE_YEAR_FORMAT_WEEK_BASED_MESSAGE);
      }
    }
  }

  private static boolean isInfringingPattern(String pattern) {
    return WEEK_PATTERN.matcher(pattern).matches() && YEAR_OF_ERA_PATTERN.matcher(pattern).matches();
  }

  /**
   * Walking back through an invocation chain from a call to DateTimeFormatterBuilder.toFormatter looking for calls to appendValue.
   * If conflicting week and year settings are detected, an issue is reported
   * @param invocation A call to DateTimeFormatterBuilder.toFormatter
   */
  private void visitBuildChain(MethodInvocationTree invocation) {
    boolean usesWeek = false;
    boolean usesWeekOfWeekBasedYear = false;
    boolean usesYear = false;
    boolean usesWeekBasedYear = false;
    List<JavaFileScannerContext.Location> locations = new ArrayList<>();
    ExpressionTree wanderer = invocation.methodSelect();
    ExpressionTree primary = null;
    while (wanderer.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) wanderer).expression();
      if (!expression.is(Tree.Kind.METHOD_INVOCATION)) {
        break;
      }
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      if (APPEND_VALUE_MATCHER.matches(mit)) {
        ExpressionTree argument = mit.arguments().get(0);
        if (isWeekBasedYearUsed(argument)) {
          usesYear = true;
          usesWeekBasedYear = true;
          primary = argument;
        } else if (isYearArgument(argument)) {
          usesYear = true;
          primary = argument;
        }
        if (isWeekOfWeekBasedYearUsed(argument)) {
          usesWeek = true;
          usesWeekOfWeekBasedYear = true;
          locations.add(new JavaFileScannerContext.Location("POP", argument));
        } else if (isWeekArgument(argument)) {
          usesWeek = true;
          locations.add(new JavaFileScannerContext.Location("POP", argument));
        }
      }
      wanderer = mit.methodSelect();
    }
    ExpressionTree lastExpression = ((MemberSelectExpressionTree) wanderer).expression();
    if (!lastExpression.is(Tree.Kind.NEW_CLASS)) {
      return;
    }
    if (usesWeek && usesYear) {
      if (usesWeekBasedYear && !usesWeekOfWeekBasedYear) {
        reportIssue(primary, CHANGE_YEAR_FORMAT_TO_CHRONOFIELD_MESSAGE, locations, null);
      } else if (!usesWeekBasedYear && usesWeekOfWeekBasedYear) {
        reportIssue(primary, CHANGE_YEAR_FORMAT_WEEK_BASED_MESSAGE, locations, null);
      }
    }
  }

  public static boolean isWeekArgument(ExpressionTree argument) {
    return isWeekOfWeekBasedYearUsed(argument) || isChronoFieldWeek(argument);
  }

  private static boolean isWeekOfWeekBasedYearUsed(ExpressionTree argument) {
    if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree call = (MethodInvocationTree) argument;
      return WEEK_OF_WEEK_BASED_YEAR_MATCHER.matches(call);
    }
    return false;
  }

  public static boolean isChronoFieldWeek(ExpressionTree argument) {
    if (argument.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree select = (MemberSelectExpressionTree) argument;
      IdentifierTree identifier = select.identifier();
      return identifier.name().equals("ALIGNED_WEEK_OF_YEAR");
    }
    return false;
  }

  public static boolean isYearArgument(ExpressionTree argument) {
    return isWeekBasedYearUsed(argument) || isChronoFieldYear(argument);
  }

  private static boolean isWeekBasedYearUsed(ExpressionTree argument) {
    if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree call = (MethodInvocationTree) argument;
      return WEEK_BASED_YEAR_MATCHER.matches(call);
    }
    return false;
  }

  public static boolean isChronoFieldYear(ExpressionTree argument) {
    if (argument.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree select = (MemberSelectExpressionTree) argument;
      IdentifierTree identifier = select.identifier();
      return identifier.name().equals("YEAR");
    }
    return false;
  }
}
