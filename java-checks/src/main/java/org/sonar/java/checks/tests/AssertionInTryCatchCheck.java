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
package org.sonar.java.checks.tests;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.UnitTestUtils.COMMON_ASSERTION_MATCHER;

@Rule(key = "S5779")
public class AssertionInTryCatchCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    TryStatementTree tryStatementTree = (TryStatementTree) tree;
    getUnusedCatchAssertionErrorParameter(tryStatementTree.catches()).ifPresent(catchTree ->
      tryStatementTree.block().accept(new TryBodyVisitor(catchTree))
    );
  }

  private static Optional<VariableTree> getUnusedCatchAssertionErrorParameter(List<CatchTree> catches) {
    return catches.stream()
      .map(CatchTree::parameter)
      .filter(param -> {
        Type symbolType = param.type().symbolType();
        return param.symbol().usages().isEmpty() &&
          (symbolType.isSubtypeOf("java.lang.AssertionError")
            || symbolType.is("java.lang.Error")
            || symbolType.is("java.lang.Throwable"));
      }).findFirst();
  }

  private class TryBodyVisitor extends BaseTreeVisitor {
    private final List<JavaFileScannerContext.Location> secondaryLocation;

    public TryBodyVisitor(VariableTree catchTree) {
      this.secondaryLocation = Collections.singletonList(new JavaFileScannerContext.Location(
        "This parameter will catch the AssertionError",
        catchTree.type()));
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (COMMON_ASSERTION_MATCHER.matches(methodInvocation)) {
        IdentifierTree identifier = ExpressionUtils.methodName(methodInvocation);
        reportIssue(identifier,
          String.format("Don't use %s() inside a try-catch catching an AssertionError.", identifier.name()),
          secondaryLocation,
          null);
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Skip class
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // Skip lambdas
    }

  }

}
