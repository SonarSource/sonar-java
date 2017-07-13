/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableSet;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Rule(key = "S4034")
public class PreferStreamAnyMatchCheck extends AbstractMethodDetection {

  private static final Set<String> STREAM_TYPES = ImmutableSet.of("java.util.stream.Stream", "java.util.stream.IntStream", "java.util.stream.LongStream",
    "java.util.stream.DoubleStream");

  private static final MethodMatcherCollection FIND_METHODS = MethodMatcherCollection.create();

  static {
    STREAM_TYPES.forEach(type -> {
      FIND_METHODS.add(MethodMatcher.create().typeDefinition(type).name("findFirst").withoutParameter());
      FIND_METHODS.add(MethodMatcher.create().typeDefinition(type).name("findAny").withoutParameter());
    });
  }

  private static final MethodMatcherCollection FILTER_METHODS = MethodMatcherCollection.create();

  static {
    STREAM_TYPES.forEach(type -> FILTER_METHODS.add(MethodMatcher.create().typeDefinition(type).name("filter").withAnyParameters()));
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Stream.of("java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble")
      .map(type -> MethodMatcher.create().typeDefinition(type).name("isPresent").withoutParameter())
      .collect(Collectors.toList());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree isPresentMIT) {
    previousMITInChain(isPresentMIT)
      .filter(FIND_METHODS::anyMatch)
      .ifPresent(findMIT ->
        previousMITInChain(findMIT).filter(FILTER_METHODS::anyMatch)
        .ifPresent(filterMIT ->
          context.reportIssue(this, MethodsHelper.methodName(filterMIT), MethodsHelper.methodName(isPresentMIT),
            "Replace this \"filter()." + MethodsHelper.methodName(findMIT).name() +"().isPresent()\" chain with \"anyMatch()\"")));
  }

  private static Optional<MethodInvocationTree> previousMITInChain(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree previousInvocation = (MethodInvocationTree) expression;
        return Optional.of(previousInvocation);
      }
    }
    return Optional.empty();
  }

}
