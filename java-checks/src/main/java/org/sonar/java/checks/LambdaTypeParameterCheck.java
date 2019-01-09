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

import com.google.common.collect.Iterables;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Rule(key = "S2211")
public class LambdaTypeParameterCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    LambdaExpressionTree lambdaExpressionTree = (LambdaExpressionTree) tree;
    List<VariableTree> parameters = lambdaExpressionTree.parameters();
    if(parameters.size() <= 2 && !lambdaExpressionTree.body().is(Tree.Kind.BLOCK)) {
      // ignore lambdas with one or two params and a non-block body
      return;
    }
    String missingTypeParameters = parameters.stream()
      .filter(variable -> variable.type().is(Tree.Kind.INFERED_TYPE))
      .map(VariableTree::simpleName)
      .map(IdentifierTree::name)
      .map(parameterName -> "'" + parameterName + "'")
      .collect(Collectors.joining(", "));

    if (!missingTypeParameters.isEmpty()) {
      reportIssue(parameters.get(0), Iterables.getLast(parameters), String.format("Specify a type for: %s", missingTypeParameters));
    }
  }
}
