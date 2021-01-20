/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5917")
public class DateTimeFormatterMismatchCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers APPEND_VALUE_MATCHER = MethodMatchers.create()
    .ofTypes("java.time.format.DateTimeFormatterBuilder")
    .names("appendValue")
    .addParametersMatcher("java.time.temporal.TemporalField", "int")
    .build();

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

  private static final String CHANGE_YEAR_FORMAT_WEEK_BASED_MESSAGE = "Change this year format to use the week-based year instead.";
  private static final String CHANGE_YEAR_FORMAT_TO_CHRONOFIELD_MESSAGE = "Change this year format to use ChronoField.YEAR instead.";
  private static final String SECONDARY_LOCATION_MESSAGE = "";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (APPEND_VALUE_MATCHER.matches(invocation)) {
      ChainedInvocationVisitor visitor = new ChainedInvocationVisitor();
      invocation.accept(visitor);
      if (!visitor.usesWeek || !visitor.usesYear) {
        return;
      }
      if (visitor.usesWeekBasedYear && !visitor.usesWeekOfWeekBasedYear) {
        reportIssue(visitor.primary, CHANGE_YEAR_FORMAT_TO_CHRONOFIELD_MESSAGE, visitor.secondaries, null);
      } else if (!visitor.usesWeekBasedYear && visitor.usesWeekOfWeekBasedYear) {
        reportIssue(visitor.primary, CHANGE_YEAR_FORMAT_WEEK_BASED_MESSAGE, visitor.secondaries, null);
      }
    }
  }

  private static class ChainedInvocationVisitor extends BaseTreeVisitor {
    private boolean usesWeek = false;
    private boolean usesWeekOfWeekBasedYear = false;
    private boolean usesYear = false;
    private boolean usesWeekBasedYear = false;
    private final List<JavaFileScannerContext.Location> secondaries = new ArrayList<>();
    private ExpressionTree primary = null;

    @Override
    public void visitMethodInvocation(MethodInvocationTree callToAppendValue) {
      MethodInvocationTree tree = callToAppendValue;
      inspectCall(tree);
      Optional<MethodInvocationTree> current = MethodTreeUtils.subsequentMethodInvocation(tree, APPEND_VALUE_MATCHER);
      while (current.isPresent()) {
        tree = current.get();
        inspectCall(tree);
        current = MethodTreeUtils.subsequentMethodInvocation(tree, APPEND_VALUE_MATCHER);
      }
    }

    private void inspectCall(MethodInvocationTree invocation) {
      ExpressionTree argument = invocation.arguments().get(0);
      if (refersToYear(argument)) {
        usesYear = true;
        primary = argument;
        usesWeekBasedYear |= isWeekBasedYearUsed(argument);
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
      IdentifierTree identifier = select.identifier();
      return identifier.name().equals("ALIGNED_WEEK_OF_YEAR");
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
        IdentifierTree identifier = select.identifier();
        return select.symbolType().is("java.time.temporal.ChronoField") &&
          (identifier.name().equals("YEAR") || identifier.name().equals("YEAR_OF_ERA"));
      }
      return false;
    }
  }
}
