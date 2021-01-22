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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5917")
public class DateTimeFormatterMismatchCheck extends AbstractMethodDetection {
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

  private Map<MethodInvocationTree, ChainedInvocationVisitor> chains = new HashMap<>();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return APPEND_VALUE_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree invocation) {
    ChainedInvocationVisitor visitor = new ChainedInvocationVisitor();
    invocation.accept(visitor);
    if (visitor.chain.size() <= 1) {
      return;
    }
    MethodInvocationTree lastLink = visitor.chain.get(visitor.chain.size() - 1);
    ChainedInvocationVisitor candidateVisitor = chains.computeIfAbsent(lastLink, ignored -> new ChainedInvocationVisitor());
    if (candidateVisitor.chain.size() < visitor.chain.size()) {
      chains.put(lastLink, visitor);
    }
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    super.leaveFile(context);
    for (ChainedInvocationVisitor visitor : chains.values()) {
      visitor.inspectChain();
      if (visitor.usesWeekBasedYear && !visitor.usesWeekOfWeekBasedYear) {
        reportIssue(visitor.primary, CHANGE_YEAR_FORMAT_TO_CHRONOFIELD_MESSAGE, visitor.secondaries, null);
      } else if (!visitor.usesWeekBasedYear && visitor.usesWeekOfWeekBasedYear) {
        reportIssue(visitor.primary, CHANGE_YEAR_FORMAT_WEEK_BASED_MESSAGE, visitor.secondaries, null);
      }
    }
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

    private boolean usesWeekOfWeekBasedYear = false;
    private boolean usesWeekBasedYear = false;
    private final List<JavaFileScannerContext.Location> secondaries = new ArrayList<>();
    private ExpressionTree primary = null;
    List<MethodInvocationTree> chain = new ArrayList<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree callToAppendValue) {
      MethodInvocationTree tree = callToAppendValue;
      chain.add(callToAppendValue);
      Optional<MethodInvocationTree> current = MethodTreeUtils.subsequentMethodInvocation(tree, APPEND_VALUE_MATCHER);
      while (current.isPresent()) {
        tree = current.get();
        chain.add(tree);
        current = MethodTreeUtils.subsequentMethodInvocation(tree, APPEND_VALUE_MATCHER);
      }
    }

    private void inspectChain() {
      for (MethodInvocationTree invocation : chain) {
        inspectCall(invocation);
      }
    }

    private void inspectCall(MethodInvocationTree invocation) {
      ExpressionTree argument = invocation.arguments().get(0);
      if (refersToYear(argument)) {
        if (primary == null) {
          primary = argument;
        }
        boolean isWeekBasedYearArgument = isWeekBasedYearUsed(argument);
        usesWeekBasedYear |= isWeekBasedYearArgument;
        if (isWeekBasedYearArgument) {
          primary = argument;
        }
      } else if (refersToWeek(argument)) {
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
