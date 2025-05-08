/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.tests;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.UnitTestUtils.FAIL_METHOD_MATCHER;

@Rule(key = "S5778")
public class OneExpectedRuntimeExceptionCheck extends AbstractOneExpectedExceptionRule {

  private static final MethodMatchers MOCKITO_MOCK_METHOD_MATCHERS = MethodMatchers.create()
    .ofTypes("org.mockito.Mockito")
    .names("mock")
    .addParametersMatcher("java.lang.Class").addParametersMatcher("java.lang.Class", "java.lang.String")
    .build();
  private static final MethodMatchers ENUM_FINAL_METHODS = MethodMatchers.create()
    .ofSubTypes("java.lang.Enum")
    .names("name", "ordinal", "equals", "hashCode", "compareTo", "getDeclaringClass", "describeConstable")
    .withAnyParameters()
    .build();
  private static final MethodMatchers AUTHORIZED_METHODS = MethodMatchers.or(
    FAIL_METHOD_MATCHER,
    MOCKITO_MOCK_METHOD_MATCHERS,
    ENUM_FINAL_METHODS
  );

  @Override
  void reportMultipleCallInTree(List<Type> expectedExceptions, Tree treeToVisit, Tree reportLocation, String placeToRefactor) {
    List<Type> checkedTypes = expectedExceptions.stream()
      .filter(e -> !isChecked(e))
      .toList();

    if (checkedTypes.isEmpty()) {
      return;
    }

    MethodTreeUtils.MethodInvocationCollector visitor = new MethodTreeUtils.MethodInvocationCollector(symbol -> !AUTHORIZED_METHODS.matches(symbol));
    treeToVisit.accept(visitor);
    List<Tree> invocationTree = visitor.getInvocationTree();
    if (invocationTree.size() > 1) {
      reportIssue(reportLocation,
        String.format("Refactor the %s to have only one invocation possibly throwing a runtime exception.", placeToRefactor),
        secondaryLocations(invocationTree, "May throw a runtime exception"),
        null);
    }
  }

}
