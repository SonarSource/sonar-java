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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.TreeHelper.findClosestParentOfKind;

/**
 * The rule checks for the use of "execute", "executeQuery" and "executeUpdate" methods 
 * on a Statement inside a loop or a "forEach" on Iterables, Maps and Streams.
 */
@Rule(key = "S6912")
public class BatchSQLStatementsCheck extends IssuableSubscriptionVisitor {
  private static final String MESSAGE = "Use \"addBatch\" and \"executeBatch\" to execute multiple SQL statements in a single call.";
  private static final MethodMatchers FOR_EACH_MATCHER = MethodMatchers.create()
    .ofSubTypes("java.lang.Iterable", "java.util.stream.Stream", "java.util.Map")
    .names("forEach")
    .withAnyParameters()
    .build();

  private final StatementExecuteInvocationsCollector invocationsCollector = new StatementExecuteInvocationsCollector();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(
      Tree.Kind.FOR_STATEMENT,
      Tree.Kind.FOR_EACH_STATEMENT,
      Tree.Kind.WHILE_STATEMENT,
      Tree.Kind.DO_STATEMENT,
      Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    // Collecting invocations in a Set and reporting issues when leaving the file to avoid duplicates in case of nested loops or lambdas
    invocationsCollector.invocations.forEach(invocation -> reportIssue(invocation, MESSAGE));
    invocationsCollector.invocations.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case FOR_STATEMENT, WHILE_STATEMENT, DO_STATEMENT, FOR_EACH_STATEMENT -> tree.accept(invocationsCollector);
      case LAMBDA_EXPRESSION -> {
        if (findClosestParentOfKind(tree, Set.of(Tree.Kind.METHOD_INVOCATION)) instanceof MethodInvocationTree mit
          && FOR_EACH_MATCHER.matches(mit)) {
          tree.accept(invocationsCollector);
        }
      }
      default -> {
        // All expected nodes are handled, no need to do anything
      }
    }
  }

  private static class StatementExecuteInvocationsCollector extends BaseTreeVisitor {
    private static final MethodMatchers EXECUTE_METHODS = MethodMatchers.create()
      .ofSubTypes("java.sql.Statement")
      .names("execute", "executeQuery", "executeUpdate")
      .withAnyParameters()
      .build();

    private final Set<MethodInvocationTree> invocations = new HashSet<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (EXECUTE_METHODS.matches(tree)) {
        invocations.add(tree);
      }
    }
  }
}
