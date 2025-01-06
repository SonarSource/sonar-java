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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2211")
public class LambdaTypeParameterCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.LAMBDA_EXPRESSION);
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
      reportIssue(parameters.get(0), ListUtils.getLast(parameters), String.format("Specify a type for: %s", missingTypeParameters));
    }
  }
}
