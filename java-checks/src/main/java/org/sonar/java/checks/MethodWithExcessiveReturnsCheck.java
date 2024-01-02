/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1142")
public class MethodWithExcessiveReturnsCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "This method has %d returns, which is more than the %d allowed.";

  private static final int DEFAULT_MAX = 3;

  @RuleProperty(description = "Maximum allowed return statements per method", defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private final Map<Tree, List<Tree>> returnStatements = new HashMap<>();
  private final Deque<Tree> methodsOrLambdas = new LinkedList<>();

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    returnStatements.clear();
    methodsOrLambdas.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.RETURN_STATEMENT, Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.RETURN_STATEMENT)) {
      returnStatements.computeIfAbsent(methodsOrLambdas.peek(), k -> new LinkedList<>())
        .add(((ReturnStatementTree) tree).returnKeyword());
    } else {
      methodsOrLambdas.push(tree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (!tree.is(Tree.Kind.RETURN_STATEMENT)) {
      reportTree(tree).ifPresent(reportTree -> report(tree, reportTree));
      returnStatements.remove(tree);
      methodsOrLambdas.pop();
    }
  }

  private static Optional<Tree> reportTree(Tree methodOrLambda) {
    if (methodOrLambda.is(Tree.Kind.LAMBDA_EXPRESSION)) {
      return Optional.of(((LambdaExpressionTree) methodOrLambda).arrowToken());
    }
    MethodTree method = (MethodTree) methodOrLambda;
    if (!MethodTreeUtils.isEqualsMethod(method)) {
      return Optional.of(method.simpleName());
    }
    // equals can have many returns and it's OK
    return Optional.empty();
  }

  private void report(Tree currentTree, Tree reportTree) {
    List<Tree> returns = returnStatements.getOrDefault(currentTree, Collections.emptyList());
    int count = returns.size();
    if (count > max) {
      String message = String.format(ISSUE_MESSAGE, count, max);
      List<JavaFileScannerContext.Location> secondaries = returns.stream()
        .map(token -> new JavaFileScannerContext.Location("return", token))
        .collect(Collectors.toList());
      reportIssue(reportTree, message, secondaries, null);
    }
  }
}
