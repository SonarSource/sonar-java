/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.UnitTestUtils.ASSERTIONS_METHOD_MATCHER;

@Rule(key = "S5777")
public class TestAnnotationWithExpectedExceptionCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;
    findExpectedException(method.modifiers().annotations()).ifPresent(expected -> {
      AssertionCollector assertionCollector = new AssertionCollector();
      method.accept(assertionCollector);
      if (!assertionCollector.assertions.isEmpty()) {
        reportIssue(
          expected,
          "Move assertions into separate method or use assertThrows or try-catch instead.",
          assertionCollector.assertions,
          null
        );
      }
    });
  }

  private static Optional<ExpressionTree> findExpectedException(List<AnnotationTree> annotations) {
    for (AnnotationTree annotation : annotations) {
      if (annotation.annotationType().symbolType().is("org.junit.Test")) {
        for (ExpressionTree argument : annotation.arguments()) {
          if (argument.is(Tree.Kind.ASSIGNMENT)) {
            AssignmentExpressionTree assignment = (AssignmentExpressionTree) argument;
            if ("expected".equals(((IdentifierTree) assignment.variable()).name())) {
              return Optional.of(assignment);
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  private static class AssertionCollector extends BaseTreeVisitor {

    private final List<JavaFileScannerContext.Location> assertions = new ArrayList<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (ASSERTIONS_METHOD_MATCHER.matches(methodInvocation)) {
        assertions.add(new JavaFileScannerContext.Location(
          "Assertion in method with expected exception",
          ExpressionUtils.methodName(methodInvocation)
        ));
      }
    }

  }

}
