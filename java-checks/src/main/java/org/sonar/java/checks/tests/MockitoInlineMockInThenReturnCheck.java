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
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9016")
public class MockitoInlineMockInThenReturnCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String MESSAGE = "Extract this mock creation to a local variable.";

  private static final MethodMatchers THEN_RETURN = MethodMatchers.create()
    .ofSubTypes("org.mockito.stubbing.OngoingStubbing")
    .names("thenReturn")
    .withAnyParameters()
    .build();

  private static final MethodMatchers MOCKITO_MOCK = MethodMatchers.create()
    .ofTypes("org.mockito.Mockito")
    .names("mock")
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
  public void visitMethodInvocation(MethodInvocationTree mit) {
    if (THEN_RETURN.matches(mit)) {
      for (ExpressionTree arg : mit.arguments()) {
        if (arg.is(Tree.Kind.METHOD_INVOCATION) && MOCKITO_MOCK.matches((MethodInvocationTree) arg)) {
          context.reportIssue(this, arg, MESSAGE);
        }
      }
    }
    super.visitMethodInvocation(mit);
  }
}
