/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.tests;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9017")
public class MockitoStubbingChainCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String WHEN_MESSAGE = "Complete this stubbing by adding \"thenReturn()\", \"thenThrow()\", \"thenAnswer()\", or \"thenCallRealMethod()\".";
  private static final String DO_MESSAGE = "Complete this stubbing by adding \".when(mock).method()\".";
  private static final String STUBBER_WHEN_MESSAGE = "Complete this stubbing by adding the method to stub.";

  private static final MethodMatchers MOCKITO_WHEN = MethodMatchers.create()
    .ofTypes("org.mockito.Mockito")
    .names("when")
    .withAnyParameters()
    .build();

  private static final MethodMatchers MOCKITO_DO_METHODS = MethodMatchers.create()
    .ofTypes("org.mockito.Mockito")
    .names("doReturn", "doThrow", "doAnswer", "doNothing", "doCallRealMethod")
    .withAnyParameters()
    .build();

  // Detects do*().when(mock) that is not followed by a method call on the mock
  private static final MethodMatchers STUBBER_WHEN = MethodMatchers.create()
    .ofSubTypes("org.mockito.stubbing.Stubber")
    .names("when")
    .withAnyParameters()
    .build();

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (context.getSemanticModel() == null) {
      return;
    }
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    // skip: the stub may be stored in a variable and completed later
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    // skip: the stub may be stored in a pre-declared variable and completed later
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    // skip: returning a partial stub is valid (e.g. helper methods)
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    visitMethodInvocation(mit, false);
    // also check stubs inside block-bodied lambda arguments (e.g. assertThrows(() -> { when(...); }))
    for (ExpressionTree arg : mit.arguments()) {
      if (arg instanceof LambdaExpressionTree lambda
        && lambda.body() instanceof BlockTree block) {
        visitBlock(block);
      }
    }
  }

  private void visitMethodInvocation(MethodInvocationTree mit, boolean isChained) {
    if (!isChained && isIncompleteStubbing(mit)) {
      return;
    }
    if (mit.methodSelect() instanceof MemberSelectExpressionTree mset
      && mset.expression() instanceof MethodInvocationTree innerMit) {
      visitMethodInvocation(innerMit, true);
    }
  }

  private boolean isIncompleteStubbing(MethodInvocationTree mit) {
    if (MOCKITO_WHEN.matches(mit)) {
      context.reportIssue(this, mit, WHEN_MESSAGE);
      return true;
    }
    if (MOCKITO_DO_METHODS.matches(mit)) {
      context.reportIssue(this, mit, DO_MESSAGE);
      return true;
    }
    if (STUBBER_WHEN.matches(mit)) {
      context.reportIssue(this, mit, STUBBER_WHEN_MESSAGE);
      return true;
    }
    return false;
  }
}
