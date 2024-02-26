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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.TreeHelper.findClosestParentOfKind;

/**
 * The rule checks for the use of "execute", "executeQuery" and "executeUpdate" methods 
 * on a Statement inside a loop or a "forEach" on Iterables, Maps and Streams.
 */
@Rule(key = "S6912")
public class BatchSQLStatementsCheck extends AbstractMethodDetection {
  private static final String MESSAGE = "Use \"addBatch\" and \"executeBatch\" to execute multiple SQL statements in a single call.";
  private static final Set<Tree.Kind> LOOP_TREE_KINDS = EnumSet.of(Tree.Kind.FOR_STATEMENT, Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT);
  private static final MethodMatchers EXECUTE_METHODS = MethodMatchers.create()
    .ofSubTypes("java.sql.Statement")
    .names("execute", "executeQuery", "executeUpdate")
    .withAnyParameters()
    .build();
  private static final MethodMatchers FOR_EACH_MATCHER = MethodMatchers.create()
    .ofSubTypes("java.lang.Iterable", "java.util.stream.Stream", "java.util.Map")
    .names("forEach")
    .withAnyParameters()
    .build();

  private final Set<MethodInvocationTree> invocations = new HashSet<>();

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    invocations.clear();
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    // Collecting invocations in a Set and reporting issues when leaving the file to avoid duplicates in case of nested loops or lambdas
    invocations.forEach(mit -> reportIssue(mit, MESSAGE));
    invocations.clear();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return EXECUTE_METHODS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (hasLoopParent(mit) || isLambdaInsideForEach(mit)) {
      invocations.add(mit);
    }
  }

  private static boolean hasLoopParent(MethodInvocationTree mit) {
    return Optional.ofNullable(mit.parent()) // ExpressionStatementTree
      .map(Tree::parent)// BlockTree
      .map(Tree::parent)
      .map(Tree::kind)
      .filter(LOOP_TREE_KINDS::contains)
      .isPresent();
  }

  private static boolean isLambdaInsideForEach(MethodInvocationTree mit) {
    return findClosestParentOfKind(mit, Set.of(Tree.Kind.LAMBDA_EXPRESSION)) instanceof LambdaExpressionTree enclosingLambda
      && findClosestParentOfKind(enclosingLambda, Set.of(Tree.Kind.METHOD_INVOCATION)) instanceof MethodInvocationTree enclosingMit
      && FOR_EACH_MATCHER.matches(enclosingMit);
  }
}
