/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4034")
public class PreferStreamAnyMatchCheck extends AbstractMethodDetection {

  private static final String[] STREAM_TYPES = {
    "java.util.stream.Stream",
    "java.util.stream.IntStream",
    "java.util.stream.LongStream",
    "java.util.stream.DoubleStream"
  };

  private static final MethodMatchers FIND_METHODS = MethodMatchers.create()
    .ofTypes(STREAM_TYPES).names("findFirst", "findAny").addWithoutParametersMatcher().build();

  private static final MethodMatchers MAP_METHODS = MethodMatchers.create()
    .ofTypes(STREAM_TYPES).names("map").addParametersMatcher("java.util.function.Function").build();

  private static final MethodMatchers FILTER_METHODS = MethodMatchers.create()
    .ofTypes(STREAM_TYPES).names("filter").withAnyParameters().build();

  private static final MethodMatchers BOOLEAN_VALUE = MethodMatchers.create()
    .ofTypes("java.lang.Boolean")
    .names("booleanValue")
    .addWithoutParametersMatcher()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes("java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble")
        .names("isPresent")
        .addWithoutParametersMatcher()
        .build(),
      MethodMatchers.create()
        .ofTypes(STREAM_TYPES)
        .names("anyMatch")
        .addParametersMatcher("java.util.function.Predicate")
        .build());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    String methodName = mit.methodSymbol().name();
    if ("isPresent".equals(methodName)) {
      handleIsPresent(mit);
    } else if ("anyMatch".equals(methodName)) {
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
        .filter(MAP_METHODS::matches)
        .ifPresent(mapMIT -> context.reportIssue(this, reportTree,
          "Use mapper from \"map()\" directly as predicate in \"anyMatch()\"."));
    }
  }

  private static boolean isBooleanValueReference(MethodReferenceTree predicate) {
    return BOOLEAN_VALUE.matches(predicate.method().symbol());
  }

  private void handleIsPresent(MethodInvocationTree isPresentMIT) {
    previousMITInChain(isPresentMIT)
      .filter(FIND_METHODS::matches)
      .ifPresent(findMIT ->
        previousMITInChain(findMIT).filter(FILTER_METHODS::matches)
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
