/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Map;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4266")
public class RedundantStreamCollectCheck extends AbstractMethodDetection {

  private static final String[] STREAM_TYPES = {
    "java.util.stream.Stream",
    "java.util.stream.IntStream",
    "java.util.stream.LongStream",
    "java.util.stream.DoubleStream"
  };

  private static final MethodMatchers COUNTING = streamCollectorsMatcher().names("counting").addWithoutParametersMatcher().build();
  private static final MethodMatchers MAX_BY = streamCollectorsMatcher().names("maxBy").withAnyParameters().build();
  private static final MethodMatchers MIN_BY = streamCollectorsMatcher().names("minBy").withAnyParameters().build();
  private static final MethodMatchers MAPPING = streamCollectorsMatcher().names("mapping").withAnyParameters().build();
  private static final MethodMatchers REDUCING = streamCollectorsMatcher().names("reducing").withAnyParameters().build();
  private static final MethodMatchers SUMMING_INT = streamCollectorsMatcher().names("summingInt").withAnyParameters().build();
  private static final MethodMatchers SUMMING_LONG = streamCollectorsMatcher().names("summingLong").withAnyParameters().build();
  private static final MethodMatchers SUMMING_DOUBLE = streamCollectorsMatcher().names("summingDouble").withAnyParameters().build();

  private static MethodMatchers.NameBuilder streamCollectorsMatcher() {
    return MethodMatchers.create().ofTypes("java.util.stream.Collectors");
  }

  private static final Map<MethodMatchers, String> REPLACEMENTS = MapBuilder.<MethodMatchers, String>newMap()
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
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes(STREAM_TYPES).names("collect")
      .addParametersMatcher("java.util.stream.Collector")
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree collectMIT) {
    ExpressionTree argument = collectMIT.arguments().get(0);
    if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) argument;
      REPLACEMENTS.entrySet().stream()
        .filter(e -> e.getKey().matches(methodInvocation))
        .findFirst()
        .ifPresent(e -> context.reportIssue(this, ExpressionUtils.methodName(methodInvocation),
            "Use \"" + e.getValue() + "\" instead."));
    }
  }
}
