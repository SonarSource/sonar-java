/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5917")
public class DateTimeFormatterMismatchCheck extends IssuableSubscriptionVisitor {
  private static final String TEMPORAL_FIELD_TYPE = "java.time.temporal.TemporalField";
  private static final String INT_TYPE = "int";

  private static final MethodMatchers APPEND_VALUE_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.format.DateTimeFormatterBuilder")
    .names("appendValue")
    .addParametersMatcher(TEMPORAL_FIELD_TYPE)
    .addParametersMatcher(TEMPORAL_FIELD_TYPE, INT_TYPE)
    .addParametersMatcher(TEMPORAL_FIELD_TYPE, INT_TYPE, INT_TYPE, "java.time.format.SignStyle")
    .build();

  private static final String CHANGE_YEAR_FORMAT_WEEK_BASED_MESSAGE = "Change this year format to use the week-based year instead" +
    " (or the week format to Chronofield.ALIGNED_WEEK_OF_YEAR).";
  private static final String CHANGE_YEAR_FORMAT_TO_CHRONOFIELD_MESSAGE = "Change this year format to use ChronoField.YEAR instead" +
    " (or the week format to WeekFields.ISO.weekOfWeekBasedYear()).";
  private static final String SECONDARY_LOCATION_MESSAGE = "Week format is inconsistent with year.";

  private int depth = 0;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (APPEND_VALUE_MATCHER.matches(invocation)) {
      if (depth == 0) {
        ChainedInvocationVisitor visitor = new ChainedInvocationVisitor();
        invocation.accept(visitor);
        if (visitor.usesWeek && visitor.usesYear) {
          if (visitor.usesWeekBasedYear && !visitor.usesWeekOfWeekBasedYear) {
            reportIssue(visitor.primary, CHANGE_YEAR_FORMAT_TO_CHRONOFIELD_MESSAGE, visitor.secondaries, null);
          } else if (!visitor.usesWeekBasedYear && visitor.usesWeekOfWeekBasedYear) {
            reportIssue(visitor.primary, CHANGE_YEAR_FORMAT_WEEK_BASED_MESSAGE, visitor.secondaries, null);
          }
        }
      }
      depth++;
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (APPEND_VALUE_MATCHER.matches((MethodInvocationTree) tree)) {
      depth--;
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    depth = 0;
  }


  private static class ChainedInvocationVisitor extends BaseTreeVisitor {
    private static final MethodMatchers WEEK_BASED_YEAR_MATCHER = MethodMatchers.create()
      .ofTypes("java.time.temporal.WeekFields")
      .names("weekBasedYear")
      .addWithoutParametersMatcher()
      .build();

    private static final MethodMatchers WEEK_OF_WEEK_BASED_YEAR_MATCHER = MethodMatchers.create()
      .ofTypes("java.time.temporal.WeekFields")
      .names("weekOfWeekBasedYear")
      .addWithoutParametersMatcher()
      .build();

    private boolean usesWeek = false;
    private boolean usesWeekOfWeekBasedYear = false;
    private boolean usesYear = false;
    private boolean usesWeekBasedYear = false;
    private final List<JavaFileScannerContext.Location> secondaries = new ArrayList<>();
    private ExpressionTree primary = null;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      inspectCall(tree);
      ExpressionTree expressionTree = tree.methodSelect();
      expressionTree.accept(this);
    }

    private void inspectCall(MethodInvocationTree invocation) {
      if (!APPEND_VALUE_MATCHER.matches(invocation)) {
        return;
      }
      ExpressionTree argument = invocation.arguments().get(0);
      if (refersToYear(argument)) {
        usesYear = true;
        if (primary == null) {
          primary = argument;
        }
        boolean isWeekBasedYearArgument = isWeekBasedYearUsed(argument);
        usesWeekBasedYear |= isWeekBasedYearArgument;
        if (isWeekBasedYearArgument) {
          primary = argument;
        }
      } else if (refersToWeek(argument)) {
        usesWeek = true;
        secondaries.add(new JavaFileScannerContext.Location(SECONDARY_LOCATION_MESSAGE, argument));
        usesWeekOfWeekBasedYear |= isWeekOfWeekBasedYearUsed(argument);
      }
    }

    private static boolean refersToWeek(ExpressionTree argument) {
      return isChronoFieldWeek(argument) || isWeekOfWeekBasedYearUsed(argument);
    }

    private static boolean isWeekOfWeekBasedYearUsed(ExpressionTree argument) {
      if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree call = (MethodInvocationTree) argument;
        return WEEK_OF_WEEK_BASED_YEAR_MATCHER.matches(call);
      }
      return false;
    }

    private static boolean isChronoFieldWeek(ExpressionTree argument) {
      if (!argument.is(Tree.Kind.MEMBER_SELECT)) {
        return false;
      }
      MemberSelectExpressionTree select = (MemberSelectExpressionTree) argument;
      if (!select.symbolType().is("java.time.temporal.ChronoField")) {
        return false;
      }
      return "ALIGNED_WEEK_OF_YEAR".equals(select.identifier().name());
    }

    private static boolean refersToYear(ExpressionTree argument) {
      return isChronoFieldYear(argument) || isWeekBasedYearUsed(argument);
    }

    private static boolean isWeekBasedYearUsed(ExpressionTree argument) {
      if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree call = (MethodInvocationTree) argument;
        return WEEK_BASED_YEAR_MATCHER.matches(call);
      }
      return false;
    }

    private static boolean isChronoFieldYear(ExpressionTree argument) {
      if (argument.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree select = (MemberSelectExpressionTree) argument;
        String name = select.identifier().name();
        return select.symbolType().is("java.time.temporal.ChronoField") && ("YEAR".equals(name) || "YEAR_OF_ERA".equals(name));
      }
      return false;
    }
  }
}
