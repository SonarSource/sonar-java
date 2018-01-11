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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4266")
public class RedundantStreamCollectCheck extends AbstractMethodDetection {

  private static final Set<String> STREAM_TYPES = ImmutableSet.of("java.util.stream.Stream", "java.util.stream.IntStream", "java.util.stream.LongStream",
    "java.util.stream.DoubleStream");

  private static final String COLLECTORS = "java.util.stream.Collectors";

  private static final MethodMatcher COUNTING = MethodMatcher.create().typeDefinition(COLLECTORS).name("counting").withoutParameter();
  private static final MethodMatcher MAX_BY = MethodMatcher.create().typeDefinition(COLLECTORS).name("maxBy").withAnyParameters();
  private static final MethodMatcher MIN_BY = MethodMatcher.create().typeDefinition(COLLECTORS).name("minBy").withAnyParameters();
  private static final MethodMatcher MAPPING = MethodMatcher.create().typeDefinition(COLLECTORS).name("mapping").withAnyParameters();
  private static final MethodMatcher REDUCING = MethodMatcher.create().typeDefinition(COLLECTORS).name("reducing").withAnyParameters();
  private static final MethodMatcher SUMMING_INT = MethodMatcher.create().typeDefinition(COLLECTORS).name("summingInt").withAnyParameters();
  private static final MethodMatcher SUMMING_LONG = MethodMatcher.create().typeDefinition(COLLECTORS).name("summingLong").withAnyParameters();
  private static final MethodMatcher SUMMING_DOUBLE = MethodMatcher.create().typeDefinition(COLLECTORS).name("summingDouble").withAnyParameters();

  private static final Map<MethodMatcher, String> REPLACEMENTS = ImmutableMap.<MethodMatcher, String>builder()
    .put(COUNTING, "count()")
    .put(MAX_BY, "max()")
    .put(MIN_BY, "min()")
    .put(MAPPING, "map(...).collect()")
    .put(REDUCING, "reduce(...).collect()")
    .put(SUMMING_INT, "mapToInt(...).sum()")
    .put(SUMMING_LONG, "mapToLong(...).sum()")
    .put(SUMMING_DOUBLE, "mapToDouble(...).sum()")
    .build();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return STREAM_TYPES.stream()
      .map(type -> MethodMatcher.create().typeDefinition(type).name("collect").addParameter("java.util.stream.Collector"))
      .collect(Collectors.toList());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree collectMIT) {
    ExpressionTree argument = collectMIT.arguments().get(0);
    if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) argument;
      REPLACEMENTS.entrySet().stream()
        .filter(e -> e.getKey().matches(methodInvocation))
        .findFirst()
        .ifPresent(e -> context.reportIssue(this,  MethodsHelper.methodName(methodInvocation),
            "Use \"" + e.getValue() + "\" instead."));
    }
  }
}
