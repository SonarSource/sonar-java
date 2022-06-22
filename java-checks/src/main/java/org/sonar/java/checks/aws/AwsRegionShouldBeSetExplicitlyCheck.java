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
package org.sonar.java.checks.aws;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6241")
public class AwsRegionShouldBeSetExplicitlyCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers BUILD_METHOD = MethodMatchers.create()
    .ofSubTypes("software.amazon.awssdk.awscore.client.builder.AwsClientBuilder")
    .names("build")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers REGION_METHOD = MethodMatchers.create()
    .ofSubTypes("software.amazon.awssdk.awscore.client.builder.AwsClientBuilder")
    .names("region")
    .addParametersMatcher("software.amazon.awssdk.regions.Region")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (BUILD_METHOD.matches(invocation) && !chainSetsRegion(invocation)) {
      reportIssue(invocation, "Region should be set explicitly when creating a new \"AwsClient\"");
    }
  }

  private static boolean chainSetsRegion(MethodInvocationTree terminalCall) {
    ExpressionTree expression = terminalCall.methodSelect();
    while (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) expression;
      ExpressionTree currentExpression = memberSelectExpressionTree.expression();
      if (!currentExpression.is(Tree.Kind.METHOD_INVOCATION)) {
        return false;
      }
      MethodInvocationTree currentInvocation = (MethodInvocationTree) currentExpression;
      if (REGION_METHOD.matches(currentInvocation)) {
        return true;
      }
      expression = currentInvocation.methodSelect();
    }
    return false;
  }
}
