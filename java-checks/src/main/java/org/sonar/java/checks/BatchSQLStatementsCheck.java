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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6912")
public class BatchSQLStatementsCheck extends IssuableSubscriptionVisitor {
  private static final String MESSAGE = "Use \"addBatch\" and \"executeBatch\" to execute multiple SQL statements in a single call.";
  private static final MethodMatchers EXECUTE_METHODS = MethodMatchers.create()
    .ofSubTypes("java.sql.Statement")
    .names("execute", "executeQuery", "executeUpdate")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    var methodTree = (MethodTree) tree;

    var invocationsCollector = new StatementExecuteInvocationsCollector();
    methodTree.accept(invocationsCollector);

    var inLambdaInvocationsCollector = new InLambdaStatementExecuteInvocationsCollector();
    methodTree.accept(inLambdaInvocationsCollector);

    if (invocationsCollector.shouldReport() || inLambdaInvocationsCollector.shouldReport()) {
      invocationsCollector.invocations().forEach(invocation -> reportIssue(invocation, MESSAGE));
      inLambdaInvocationsCollector.invocations().forEach(invocation -> reportIssue(invocation, MESSAGE));
    }
  }

  private static class StatementExecuteInvocationsCollector extends BaseTreeVisitor {

    private final List<MethodInvocationTree> invocations = new LinkedList<>();
    private boolean foundInvocationInLoop = false;

    public List<MethodInvocationTree> invocations() {
      return invocations;
    }

    public boolean shouldReport() {
      return foundInvocationInLoop || invocations.size() > 1;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (EXECUTE_METHODS.matches(tree)) {
        invocations.add(tree);

        if (!foundInvocationInLoop) {
          foundInvocationInLoop = findClosestParentOfKind(tree, Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT, Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT)
            .isPresent();
        }
      }
    }
  }

  private static class InLambdaStatementExecuteInvocationsCollector extends BaseTreeVisitor {
    private static final MethodMatchers FOR_EACH_MATCHER = MethodMatchers.create()
      .ofSubTypes("java.lang.Iterable", "java.util.stream.Stream", "java.util.Map")
      .names("forEach")
      .withAnyParameters()
      .build();

    private final StatementExecuteInvocationsCollector invocationsCollector = new StatementExecuteInvocationsCollector();
    private boolean foundInvocationInForEach = false;

    public List<MethodInvocationTree> invocations() {
      return invocationsCollector.invocations;
    }

    public boolean shouldReport() {
      return foundInvocationInForEach || invocationsCollector.shouldReport();
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      lambdaExpressionTree.accept(invocationsCollector);

      if (!foundInvocationInForEach) {
        foundInvocationInForEach = findClosestParentOfKind(lambdaExpressionTree, Tree.Kind.METHOD_INVOCATION, MethodInvocationTree.class)
          .filter(FOR_EACH_MATCHER::matches)
          .isPresent();
      }
    }
  }

  static Optional<Tree.Kind> findClosestParentOfKind(Tree tree, Tree.Kind... nodeKinds) {
    while (tree != null) {
      if (tree.is(nodeKinds)) {
        return Optional.of(tree).map(Tree::kind);
      }
      tree = tree.parent();
    }
    return Optional.empty();
  }

  // todo use it form the TreeHelper once Marco PR is merged
  static <T extends Tree> Optional<T> findClosestParentOfKind(Tree tree, Tree.Kind nodeKind, Class<T> type) {
    while (tree != null) {
      if (tree.is(nodeKind)) {
        return Optional.of(tree).map(type::cast);
      }
      tree = tree.parent();
    }
    return Optional.empty();
  }
}
