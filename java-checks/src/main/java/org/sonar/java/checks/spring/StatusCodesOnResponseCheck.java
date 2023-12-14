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
  public static final String ISSUE_MESSAGE = "Set a HttpStatus code reflective of the operation.";

  /*
   * Values for the okCodes list are extracted from:
   * https://docs.spring.io/spring-framework/docs/5.0.6.RELEASE/javadoc-api/index.html?org/springframework/http/HttpStatus.html
   * by taking all that return 1xx, 2xx, 3xx code.
   */
  private static final List<String> OK_CODES = List.of("ACCEPTED",
    "ALREADY_REPORTED",
    "CHECKPOINT",
    "CONTINUE",
    "CREATED",
    "FOUND",
    "IM_USED",
    "MOVED_PERMANENTLY",
    "MULTIPLE_CHOICES",
    "MULTI_STATUS",
    "NON_AUTHORITATIVE_INFORMATION",
    "NOT_MODIFIED",
    "NO_CONTENT",
    "OK",
    "PARTIAL_CONTENT",
    "PERMANENT_REDIRECT",
    "PROCESSING",
    "RESET_CONTENT",
    "SEE_OTHER",
    "SWITCHING_PROTOCOLS",
    "TEMPORARY_REDIRECT");

  /*
   * Values for the errorCodes list are extracted from:
   * https://docs.spring.io/spring-framework/docs/5.0.6.RELEASE/javadoc-api/index.html?org/springframework/http/HttpStatus.html
   * by taking all that return 4xx, 5xx code.
   */
  private static final List<String> ERROR_CODES = List.of("BAD_GATEWAY",
    "BAD_REQUEST",
    "BANDWIDTH_LIMIT_EXCEEDED",
    "CONFLICT",
    "EXPECTATION_FAILED",
    "FAILED_DEPENDENCY",
    "FORBIDDEN",
    "GATEWAY_TIMEOUT",
    "GONE",
    "HTTP_VERSION_NOT_SUPPORTED",
    "INSUFFICIENT_STORAGE",
    "INTERNAL_SERVER_ERROR",
    "I_AM_A_TEAPOT",
    "LENGTH_REQUIRED",
    "LOCKED",
    "LOOP_DETECTED",
    "METHOD_NOT_ALLOWED",
    "NETWORK_AUTHENTICATION_REQUIRED",
    "NOT_ACCEPTABLE",
    "NOT_EXTENDED",
    "NOT_FOUND",
    "NOT_IMPLEMENTED",
    "PAYLOAD_TOO_LARGE",
    "PAYMENT_REQUIRED",
    "PRECONDITION_FAILED",
    "PRECONDITION_REQUIRED",
    "PROXY_AUTHENTICATION_REQUIRED",
    "REQUESTED_RANGE_NOT_SATISFIABLE",
    "REQUEST_HEADER_FIELDS_TOO_LARGE",
    "REQUEST_TIMEOUT",
    "SERVICE_UNAVAILABLE",
    "TOO_MANY_REQUESTS",
    "UNAUTHORIZED",
    "UNAVAILABLE_FOR_LEGAL_REASONS",
    "UNPROCESSABLE_ENTITY",
    "UNSUPPORTED_MEDIA_TYPE",
    "UPGRADE_REQUIRED",
    "URI_TOO_LONG",
    "VARIANT_ALSO_NEGOTIATES");

  private static final MethodMatchers STATUS_METHOD_MATCHERS = MethodMatchers.create()
    .ofTypes(RESPONSE_ENTITY)
    .names("status")
    .addParametersMatcher("org.springframework.http.HttpStatus")
    .build();

  private static final MethodMatchers OK_METHOD_MATCHERS = MethodMatchers.create()
    .ofTypes(RESPONSE_ENTITY)
    .names("ok", "created", "accepted", "noContent")
    .withAnyParameters()
    .build();

  private static final MethodMatchers ERROR_METHODS_MATCHER = MethodMatchers.create()
    .ofTypes(RESPONSE_ENTITY)
    .names("badRequest", "notFound", "unprocessableEntity")
    .addWithoutParametersMatcher()
    .build();

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

      if (STATUS_METHOD_MATCHERS.matches(methodInvocationTree)) {
        checkTryCatch(methodInvocationTree);
        return;
      }

      if (OK_METHOD_MATCHERS.matches(methodInvocationTree)) {
        Tree catchParent = checkCatch(methodInvocationTree, false);
        if (catchParent == null) {
          checkTry(methodInvocationTree, true);
        }
        return;
      }

      if (ERROR_METHODS_MATCHER.matches(methodInvocationTree)) {
        Tree catchParent = checkCatch(methodInvocationTree, true);
        if (catchParent == null) {
          checkTry(methodInvocationTree, false);
        }
        return;
      }

      super.visitMethodInvocation(methodInvocationTree);
    }

    private void checkTryCatch(MethodInvocationTree methodInvocationTree) {
      Tree catchParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.CATCH);
      boolean isError = isCodeInList(methodInvocationTree, ERROR_CODES);

      if (catchParent != null && !isError) {
        reportIssue(methodInvocationTree, ISSUE_MESSAGE);
        return;
      }

      if (catchParent == null) {
        Tree tryParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.TRY_STATEMENT);
        boolean isOk = isCodeInList(methodInvocationTree, OK_CODES);

        if (tryParent != null && !isOk) {
          reportIssue(methodInvocationTree, ISSUE_MESSAGE);
        }
      }
    }

    private boolean isCodeInList(MethodInvocationTree methodInvocationTree, List<String> codes) {
      ExpressionTree arg = methodInvocationTree.arguments().get(0);
      if (arg.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) arg;
        return codes.contains(memberSelectExpressionTree.identifier().name());
      } else if (arg.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) arg;
        return codes.contains(identifierTree.name());
      }
      return true;
    }

    private Tree checkCatch(MethodInvocationTree methodInvocationTree, boolean isError) {
      Tree catchParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.CATCH);
      if (catchParent != null && !isError) {
        reportIssue(methodInvocationTree, ISSUE_MESSAGE);
      }
      return catchParent;
    }

    private Tree checkTry(MethodInvocationTree methodInvocationTree, boolean isOk) {
      Tree tryParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.TRY_STATEMENT);

      if (tryParent != null) {
        Tree ifParent = ExpressionUtils.getParentOfType(methodInvocationTree, Tree.Kind.IF_STATEMENT);
        if (ifParent == null) {
          if (!isOk) {
            reportIssue(methodInvocationTree, ISSUE_MESSAGE);
          }
        } else {
          tryParent = ExpressionUtils.getParentOfType(ifParent, Tree.Kind.TRY_STATEMENT);
          if (tryParent == null && !isOk) {
            reportIssue(methodInvocationTree, ISSUE_MESSAGE);
          }
        }
      }
      return tryParent;
    }
  }

  private static boolean isClassController(ClassTree classTree) {
    return Stream.of("org.springframework.stereotype.Controller", "org.springframework.web.bind.annotation.RestController")
      .anyMatch(annotation -> classTree.symbol().metadata().isAnnotatedWith(annotation));
  }

}
