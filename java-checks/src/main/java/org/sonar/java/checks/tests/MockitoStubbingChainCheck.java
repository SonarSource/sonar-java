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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
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

  private Boolean isChained = null;
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
    if (isIncompleteStubbing(mit)) {
      return;
    }
    Boolean previous = isChained;
    isChained = true;
    scan(mit.methodSelect());
    // Scan lambda/anonymous-class arguments as independent chains (reset isChained so
    // incomplete stubs inside their bodies are detected, but they are not part of this chain)
    isChained = null;
    for (ExpressionTree arg : mit.arguments()) {
      if (arg.is(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.NEW_CLASS)) {
        scan(arg);
      }
    }
    isChained = previous;
  }

  private boolean isIncompleteStubbing(MethodInvocationTree mit) {
    if (Boolean.TRUE.equals(isChained)) {
      return false;
    }
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
