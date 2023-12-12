/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.spring;

import java.util.List;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6863")
public class StatusCodesOnResponseCheck extends IssuableSubscriptionVisitor {

  public static final String RESPONSE_ENTITY = "org.springframework.http.ResponseEntity";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!isClassController(classTree)) {
      return;
    }

    MethodInvocationVisitor methodInvocationVisitor = new MethodInvocationVisitor();
    classTree.accept(methodInvocationVisitor);
  }

  private class MethodInvocationVisitor extends BaseTreeVisitor {

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocationTree) {

      MethodMatchers statusMethodMatchers = MethodMatchers.create()
        .ofTypes(RESPONSE_ENTITY)
        .names("status")
        .addParametersMatcher("org.springframework.http.HttpStatus")
        .build();

      if (statusMethodMatchers.matches(methodInvocationTree)) {
        checkTryCatchForStatus(methodInvocationTree);
      }

      MethodMatchers okMethodMatchersWithParam = MethodMatchers.create()
        .ofTypes(RESPONSE_ENTITY)
        .names("ok")
        .withAnyParameters()
        .build();

      if (okMethodMatchersWithParam.matches(methodInvocationTree)) {
        Tree catchParent = checkCatchWithErrorStatus(methodInvocationTree, false);

        if (catchParent == null) {
          checkTryWithOkStatus(methodInvocationTree, true);
        }
      }

      MethodMatchers errorMethodMatchersWithParam = MethodMatchers.create()
        .ofTypes(RESPONSE_ENTITY)
        .names("badRequest", "notFound")
        .addWithoutParametersMatcher()
        .build();

      if (errorMethodMatchersWithParam.matches(methodInvocationTree)) {
        Tree catchParent = checkCatchWithErrorStatus(methodInvocationTree, true);

        if (catchParent == null) {
          checkTryWithOkStatus(methodInvocationTree, false);
        }
      }

      super.visitMethodInvocation(methodInvocationTree);
    }

    private void checkTryCatchForStatus(MethodInvocationTree methodInvocationTree) {
      Tree catchParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.CATCH);
      boolean isError = methodInvocationTree.arguments().stream()
        .map(MemberSelectExpressionTree.class::cast)
        .anyMatch(arg -> "INTERNAL_SERVER_ERROR".equals(arg.identifier().name()) || "NOT_FOUND".equals(arg.identifier().name()));

      if (catchParent != null && !isError) {
        reportIssue(methodInvocationTree,
          "Use the \"ResponseEntity.badRequest()\" or \"ResponseEntity.notFound()\" method" +
            "or set the status to \"HttpStatus.INTERNAL_SERVER_ERROR\" or \"HttpStatus.NOT_FOUND\".");
      }

      if (catchParent == null) {
        Tree tryParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.TRY_STATEMENT);
        boolean isOk = methodInvocationTree.arguments().stream()
          .map(MemberSelectExpressionTree.class::cast)
          .anyMatch(arg -> "OK".equals(arg.identifier().name()));

        if (tryParent != null && !isOk) {
          reportIssue(methodInvocationTree, "Use the \"ResponseEntity.ok()\" method or set the status to \"HttpStatus.OK\".");
        }
      }
    }

    private Tree checkCatchWithErrorStatus(MethodInvocationTree methodInvocationTree, boolean isError) {
      Tree catchParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.CATCH);

      if (catchParent != null && !isError) {
        reportIssue(methodInvocationTree,
          "Use the \"ResponseEntity.badRequest()\" or \"ResponseEntity.notFound()\" method" +
            "or set the status to \"HttpStatus.INTERNAL_SERVER_ERROR\" or \"HttpStatus.NOT_FOUND\".");
      }
      return catchParent;
    }

    private void checkTryWithOkStatus(MethodInvocationTree methodInvocationTree, boolean isOk) {
      Tree tryParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.TRY_STATEMENT);

      if (tryParent != null && !isOk) {
        reportIssue(methodInvocationTree, "Use the \"ResponseEntity.ok()\" method or set the status to \"HttpStatus.OK\".");
      }
    }
  }

  private static boolean isClassController(ClassTree classTree) {
    return Stream.of("org.springframework.stereotype.Controller", "org.springframework.web.bind.annotation.RestController")
      .anyMatch(annotation -> classTree.symbol().metadata().isAnnotatedWith(annotation));
  }

}
