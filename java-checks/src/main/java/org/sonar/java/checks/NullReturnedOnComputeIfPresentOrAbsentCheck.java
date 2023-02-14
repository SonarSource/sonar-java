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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6104")
public class NullReturnedOnComputeIfPresentOrAbsentCheck extends AbstractMethodDetection {
  private static final String PRIMARY_MESSAGE = "Use \"Map.containsKey(key)\" followed by \"Map.put(key, null)\" to add null values.";
  private static final String SECONDARY_MESSAGE = "null literal in the arguments";
  private static final MethodMatchers COMPUTE_IF = MethodMatchers.create()
    .ofTypes("java.util.Map")
    .names("computeIfPresent", "computeIfAbsent")
    .addParametersMatcher(MethodMatchers.ANY, MethodMatchers.ANY)
    .build();

  /**
   * The method is overridden for optimisation by restricting node visits to method invocations.
   */
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return COMPUTE_IF;
  }

  @Override
  public void onMethodInvocationFound(MethodInvocationTree invocation) {
    Arguments arguments = invocation.arguments();
    getNullReturnInLambda(arguments.get(1)).ifPresent(nullReturn ->
      reportIssue(ExpressionUtils.methodName(invocation),
        PRIMARY_MESSAGE,
        Collections.singletonList(new JavaFileScannerContext.Location(SECONDARY_MESSAGE, nullReturn)),
        null));
  }

  private static Optional<Tree> getNullReturnInLambda(Tree tree) {
    if (tree.is(Tree.Kind.LAMBDA_EXPRESSION)) {
      Tree body = ((LambdaExpressionTree) tree).body();
      if (body.is(Tree.Kind.NULL_LITERAL)) {
        return Optional.of(body);
      }
    }
    return Optional.empty();
  }
}
