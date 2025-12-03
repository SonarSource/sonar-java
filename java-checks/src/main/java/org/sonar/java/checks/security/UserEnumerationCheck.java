/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5804")
public class UserEnumerationCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make sure allowing user enumeration is safe here.";
  private static final String ABSTRACT_USER_DETAILS_AUTHENTICATION_PROVIDER = "org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider";
  private static final String SPRING_SEC_LDAP_AUTHENTICATION_PROVIDER = "org.springframework.security.ldap.authentication.LdapAuthenticationProvider";
  private static final String USER_DETAILS_SERVICE = "org.springframework.security.core.userdetails.UserDetailsService";
  private static final String USERNAME_NOT_FOUND_EXCEPTION = "org.springframework.security.core.userdetails.UsernameNotFoundException";
  private static final String HIDE_USER_NOT_FOUND_EXCEPTIONS = "setHideUserNotFoundExceptions";
  private static final String LOAD_USER_BY_USERNAME = "loadUserByUsername";
  private static final String BOOLEAN = "boolean";
  private static final String STRING = "java.lang.String";
  private static final String THROWABLE = "java.lang.Throwable";

  private final Deque<MethodTree> stack = new ArrayDeque<>();

  private static final MethodMatchers SET_HIDE_USER_MATCHER = MethodMatchers.create()
    .ofSubTypes(ABSTRACT_USER_DETAILS_AUTHENTICATION_PROVIDER, SPRING_SEC_LDAP_AUTHENTICATION_PROVIDER)
    .names(HIDE_USER_NOT_FOUND_EXCEPTIONS)
    .addParametersMatcher(BOOLEAN)
    .build();

  private static final MethodMatchers LOAD_USER_MATCHER = MethodMatchers.create()
    .ofSubTypes(USER_DETAILS_SERVICE)
    .names(LOAD_USER_BY_USERNAME)
    .addParametersMatcher(STRING)
    .build();


  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    stack.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CONSTRUCTOR, Tree.Kind.METHOD_INVOCATION, Tree.Kind.THROW_STATEMENT, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    // We push down references to methods and constructors before diving into other expressions
    if (tree instanceof MethodTree method) {
      stack.push(method);
      return;
    }

    if (tree instanceof ThrowStatementTree throwStatementTree) {
      if (throwStatementTree.expression().symbolType().is(USERNAME_NOT_FOUND_EXCEPTION) && !isInsideLoadUserByUserName()) {
        reportIssue(throwStatementTree.expression(), MESSAGE);
      }
      return;
    }

    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
    Arguments arguments = methodInvocationTree.arguments();
    if (arguments.isEmpty()) {
      return;
    }
    ExpressionTree firstArgument = arguments.get(0);

    checkHiddenUserNotFoundException(methodInvocationTree, firstArgument);
    checkLoadUserArgUsedInExceptions(methodInvocationTree, firstArgument);
  }

  @Override
  public void leaveNode(Tree tree) {
    // Pop the reference to the method or constructor we are leaving
    if (tree instanceof MethodTree) {
      stack.pop();
    }
  }

  private void checkLoadUserArgUsedInExceptions(MethodInvocationTree methodInvocationTree, ExpressionTree expression) {
    if (LOAD_USER_MATCHER.matches(methodInvocationTree) && expression instanceof IdentifierTree identifierTree) {
      identifierTree.symbol().usages()
        .stream().filter(UserEnumerationCheck::checkParentIsThrowable)
        .forEach(value -> reportIssue(value, MESSAGE));
    }
  }

  private void checkHiddenUserNotFoundException(MethodInvocationTree methodInvocationTree, ExpressionTree expression) {
    if (SET_HIDE_USER_MATCHER.matches(methodInvocationTree) && !expression.asConstant(Boolean.class).orElse(true)) {
      reportIssue(methodInvocationTree, MESSAGE);
    }
  }

  private static boolean checkParentIsThrowable(Tree tree) {
    Tree current = tree.parent();
    while (current instanceof ExpressionTree || current instanceof Arguments) {
      if (current instanceof NewClassTree newClassTree && newClassTree.symbolType().isSubtypeOf(THROWABLE)) {
        return true;
      }
      current = current.parent();
    }
    return false;
  }

  private boolean isInsideLoadUserByUserName() {
    return LOAD_USER_MATCHER.matches(stack.peekFirst());
  }
}
