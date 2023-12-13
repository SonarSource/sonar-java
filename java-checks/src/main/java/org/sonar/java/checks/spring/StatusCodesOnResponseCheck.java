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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6863")
public class StatusCodesOnResponseCheck extends IssuableSubscriptionVisitor {

  public static final String RESPONSE_ENTITY = "org.springframework.http.ResponseEntity";
  public static final String OK_ISSUE_MESSAGE = "Use the \"ResponseEntity.ok()\" method or set the status to \"HttpStatus.OK\".";
  public static final String ERROR_ISSUE_MESSAGE = "Use the \"ResponseEntity.badRequest()\" or \"ResponseEntity.notFound()\" method" +
    "or set the status to \"HttpStatus.INTERNAL_SERVER_ERROR\" or \"HttpStatus.NOT_FOUND\".";

  private final List<String> okCodes = List.of("CONTINUE",
    "SWITCHING_PROTOCOLS",
    "PROCESSING",
    "CHECKPOINT",
    "OK",
    "CREATED",
    "ACCEPTED",
    "NON_AUTHORITATIVE_INFORMATION",
    "NO_CONTENT",
    "RESET_CONTENT",
    "PARTIAL_CONTENT",
    "MULTI_STATUS",
    "ALREADY_REPORTED",
    "IM_USED",
    "MULTIPLE_CHOICES",
    "MOVED_PERMANENTLY",
    "FOUND",
    "SEE_OTHER",
    "NOT_MODIFIED",
    "TEMPORARY_REDIRECT",
    "PERMANENT_REDIRECT");

  private final List<String> errorCodes = List.of("BAD_REQUEST",
    "UNAUTHORIZED",
    "PAYMENT_REQUIRED",
    "FORBIDDEN",
    "NOT_FOUND",
    "METHOD_NOT_ALLOWED",
    "NOT_ACCEPTABLE",
    "PROXY_AUTHENTICATION_REQUIRED",
    "REQUEST_TIMEOUT",
    "CONFLICT",
    "GONE",
    "LENGTH_REQUIRED",
    "PRECONDITION_FAILED",
    "PAYLOAD_TOO_LARGE",
    "URI_TOO_LONG",
    "UNSUPPORTED_MEDIA_TYPE",
    "REQUESTED_RANGE_NOT_SATISFIABLE",
    "EXPECTATION_FAILED",
    "I_AM_A_TEAPOT",
    "UNPROCESSABLE_ENTITY",
    "LOCKED",
    "FAILED_DEPENDENCY",
    "UPGRADE_REQUIRED",
    "PRECONDITION_REQUIRED",
    "TOO_MANY_REQUESTS",
    "REQUEST_HEADER_FIELDS_TOO_LARGE",
    "UNAVAILABLE_FOR_LEGAL_REASONS",
    "INTERNAL_SERVER_ERROR",
    "NOT_IMPLEMENTED",
    "BAD_GATEWAY",
    "SERVICE_UNAVAILABLE",
    "GATEWAY_TIMEOUT",
    "HTTP_VERSION_NOT_SUPPORTED",
    "VARIANT_ALSO_NEGOTIATES",
    "INSUFFICIENT_STORAGE",
    "LOOP_DETECTED",
    "BANDWIDTH_LIMIT_EXCEEDED",
    "NOT_EXTENDED",
    "NETWORK_AUTHENTICATION_REQUIRED");

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

    MethodMatchers statusMethodMatchers = MethodMatchers.create()
      .ofTypes(RESPONSE_ENTITY)
      .names("status")
      .addParametersMatcher("org.springframework.http.HttpStatus")
      .build();

    MethodMatchers okMethodMatchers = MethodMatchers.create()
      .ofTypes(RESPONSE_ENTITY)
      .names("ok", "created", "accepted", "noContent")
      .withAnyParameters()
      .build();

    MethodMatchers errorMethodsMatcher = MethodMatchers.create()
      .ofTypes(RESPONSE_ENTITY)
      .names("badRequest", "notFound", "unprocessableEntity")
      .addWithoutParametersMatcher()
      .build();

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocationTree) {

      if (statusMethodMatchers.matches(methodInvocationTree)) {
        checkTryCatch(methodInvocationTree);
      }

      if (okMethodMatchers.matches(methodInvocationTree)) {
        Tree catchParent = checkCatch(methodInvocationTree, false);
        if (catchParent == null) {
          checkTry(methodInvocationTree, true);
        }
      }

      if (errorMethodsMatcher.matches(methodInvocationTree)) {
        Tree catchParent = checkCatch(methodInvocationTree, true);
        if (catchParent == null) {
          checkTry(methodInvocationTree, false);
        }
      }

      super.visitMethodInvocation(methodInvocationTree);
    }

    private void checkTryCatch(MethodInvocationTree methodInvocationTree) {
      Tree catchParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.CATCH);
      boolean isError = isCodeInList(methodInvocationTree, errorCodes);

      if (catchParent != null && !isError) {
        reportIssue(methodInvocationTree, ERROR_ISSUE_MESSAGE);
      }

      if (catchParent == null) {
        Tree tryParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.TRY_STATEMENT);
        boolean isOk = isCodeInList(methodInvocationTree, okCodes);

        if (tryParent != null && !isOk) {
          reportIssue(methodInvocationTree, OK_ISSUE_MESSAGE);
        }
      }
    }

    private boolean isCodeInList(MethodInvocationTree methodInvocationTree, List<String> codes) {
      for (ExpressionTree arg : methodInvocationTree.arguments()) {
        if (arg.is(Tree.Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) arg;
          return codes.contains(memberSelectExpressionTree.identifier().name());
        } else if (arg.is(Tree.Kind.IDENTIFIER)) {
          IdentifierTree identifierTree = (IdentifierTree) arg;
          return codes.contains(identifierTree.name());
        }
      }
      return true;
    }

    private Tree checkCatch(MethodInvocationTree methodInvocationTree, boolean isError) {
      Tree catchParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.CATCH);

      if (catchParent != null && !isError) {
        reportIssue(methodInvocationTree, ERROR_ISSUE_MESSAGE);
      }
      return catchParent;
    }

    private Tree checkTry(MethodInvocationTree methodInvocationTree, boolean isOk) {
      Tree tryParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.TRY_STATEMENT);

      if (tryParent != null && !isOk) {
        reportIssue(methodInvocationTree, OK_ISSUE_MESSAGE);
      }
      return tryParent;
    }

  }

  private static boolean isClassController(ClassTree classTree) {
    return Stream.of("org.springframework.stereotype.Controller", "org.springframework.web.bind.annotation.RestController")
      .anyMatch(annotation -> classTree.symbol().metadata().isAnnotatedWith(annotation));
  }

}
