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

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.sonar.java.checks.helpers.UnitTestUtils.ASSERTIONS_METHOD_MATCHER;

@Rule(key = "S5776")
public class ExpectedExceptionCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Consider using org.junit.Assert.assertThrows before other assertions.";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create().ofTypes("org.junit.rules.ExpectedException").names("expect").withAnyParameters().build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    MethodTree enclosingMethod = ExpressionUtils.getEnclosingMethod(mit);
    BlockTree methodBody = enclosingMethod != null ? enclosingMethod.block() : null;
    if (methodBody == null) {
      return;
    }
    IdentifierTree methodIdentifier = ExpressionUtils.methodName(mit);
    int collectAfterLine = LineUtils.startLine(methodIdentifier.identifierToken());
    AssertionCollector assertionCollector = new AssertionCollector(collectAfterLine);
    methodBody.accept(assertionCollector);
    if (!assertionCollector.assertions.isEmpty()) {
      reportIssue(methodIdentifier, MESSAGE, assertionCollector.assertions, null);
    }
  }

  private static class AssertionCollector extends BaseTreeVisitor {

    private int collectAfterLine;
    private List<Location> assertions = new ArrayList<>();

    public AssertionCollector(int collectAfterLine) {
      this.collectAfterLine = collectAfterLine;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (LineUtils.startLine(methodInvocation) > collectAfterLine &&
        ASSERTIONS_METHOD_MATCHER.matches(methodInvocation)) {
        assertions.add(new Location("Other assertion", ExpressionUtils.methodName(methodInvocation)));
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
