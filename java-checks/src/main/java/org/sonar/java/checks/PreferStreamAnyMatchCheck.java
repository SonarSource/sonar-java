/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

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

  private static final MethodMatcherCollection MAP_METHODS = MethodMatcherCollection.create();
  static {
    STREAM_TYPES.forEach(type ->
      MAP_METHODS.add(MethodMatcher.create().typeDefinition(type).name("map").addParameter("java.util.function.Function"))
    );
  }

  private static final MethodMatcherCollection FILTER_METHODS = MethodMatcherCollection.create();

  static {
    STREAM_TYPES.forEach(type -> FILTER_METHODS.add(MethodMatcher.create().typeDefinition(type).name("filter").withAnyParameters()));
  }

  private static final MethodMatcher BOOLEAN_VALUE = MethodMatcher.create().typeDefinition("java.lang.Boolean")
    .name("booleanValue").withoutParameter();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    List<MethodMatcher> matchers = new ArrayList<>();
    Stream.of("java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble")
      .map(type -> MethodMatcher.create().typeDefinition(type).name("isPresent").withoutParameter())
      .forEach(matchers::add);
    STREAM_TYPES.stream()
      .map(type -> MethodMatcher.create().typeDefinition(type).name("anyMatch").addParameter("java.util.function.Predicate"))
      .forEach(matchers::add);
    return matchers;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String methodName = mit.symbol().name();
    if (methodName.equals("isPresent")) {
      handleIsPresent(mit);
    } else if (methodName.equals("anyMatch")) {
      handleAnyMatch(mit);
    }
  }

  private void handleAnyMatch(MethodInvocationTree anyMatchMIT) {
    ExpressionTree predicate = anyMatchMIT.arguments().get(0);
    IdentifierTree reportTree = ExpressionUtils.methodName(anyMatchMIT);
    if (anyMatchMIT.parent().is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      if (predicate.is(Tree.Kind.LAMBDA_EXPRESSION) && ((LambdaExpressionTree) predicate).body().is(Tree.Kind.LOGICAL_COMPLEMENT)) {
        // !stream.anyMatch(x -> !(...))
        context.reportIssue(this, reportTree,
          "Replace this double negation with \"allMatch()\" and positive predicate.");
      } else {
        context.reportIssue(this, reportTree,
          "Replace this negation and \"anyMatch()\" with \"noneMatch()\".");
      }
    }
    if (predicate.is(Tree.Kind.METHOD_REFERENCE) && isBooleanValueReference((MethodReferenceTree) predicate)) {
      previousMITInChain(anyMatchMIT)
        .filter(MAP_METHODS::anyMatch)
        .ifPresent(mapMIT -> context.reportIssue(this, reportTree,
          "Use mapper from \"map()\" directly as predicate in \"anyMatch()\"."));
    }
  }

  private static boolean isBooleanValueReference(MethodReferenceTree predicate) {
    return BOOLEAN_VALUE.matches(predicate.method().symbol());
  }

  private void handleIsPresent(MethodInvocationTree isPresentMIT) {
    previousMITInChain(isPresentMIT)
      .filter(FIND_METHODS::anyMatch)
      .ifPresent(findMIT ->
        previousMITInChain(findMIT).filter(FILTER_METHODS::anyMatch)
          .ifPresent(filterMIT ->
    context.reportIssue(this, ExpressionUtils.methodName(filterMIT), ExpressionUtils.methodName(isPresentMIT),
      "Replace this \"filter()." + ExpressionUtils.methodName(findMIT).name() + "().isPresent()\" chain with \"anyMatch()\".")));
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
