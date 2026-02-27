/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8471")
public class MinimizeBoundScopedValuesCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final int THRESHOLD = 3;
  private static final String MESSAGE = "Consider grouping the %d scoped values bound in this chain of method calls into a record class to maintain good performance.";

  private final Set<Tree> visitedMethodInvocations = new HashSet<>();

  private static final MethodMatchers WHERE_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.ScopedValue", "java.lang.ScopedValue$Carrier")
    .names("where")
    .withAnyParameters()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return WHERE_MATCHER;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodInvocation) {
    Pair<Integer, Tree> whereCountAndChainStart = countWhereChain(methodInvocation);
    int whereCount = whereCountAndChainStart.getLeft();
    Tree chainStart = whereCountAndChainStart.getRight();
    if (whereCount >= THRESHOLD) {
      reportIssue(chainStart, methodInvocation, String.format(MESSAGE, whereCount));
    }
  }

  private Pair<Integer, Tree> countWhereChain(MethodInvocationTree methodInvocation) {
    int count = 0;
    while (visitedMethodInvocations.add(methodInvocation)) {
      if (WHERE_MATCHER.matches(methodInvocation)) {
        count++;
      }
      if (methodInvocation.methodSelect() instanceof MemberSelectExpressionTree memberSelect &&
        memberSelect.expression() instanceof MethodInvocationTree previousInvocation) {
        methodInvocation = previousInvocation;
      }
    }
    return Pair.of(count, methodInvocation);
  }

}
