/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5804")
public class UserEnumerationCheck extends IssuableSubscriptionVisitor {

  private final MethodMatchers setHideUserMatcher = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider")
    .names("setHideUserNotFoundExceptions")
    .addParametersMatcher("boolean")
    .build();

  private final MethodMatchers loadUserMatcher = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.core.userdetails.UserDetailsService")
    .names("loadUserByUsername")
    .addParametersMatcher("java.lang.String")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
    ExpressionTree expression = methodInvocationTree.arguments().get(0);
    if (setHideUserMatcher.matches(methodInvocationTree) && isFalseLiteral(expression)) {
      reportIssue(tree, "Make sure allowing user enumeration is safe here.");
    }
    if (loadUserMatcher.matches(methodInvocationTree) && expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) expression;
      Optional<IdentifierTree> incompliantUsage = identifierTree.symbol().usages()
        .stream().filter(UserEnumerationCheck::isExceptionArgument).findFirst();
      incompliantUsage.ifPresent(value -> reportIssue(value, "Make sure allowing user enumeration is safe here."));
    }
  }

  private static boolean isFalseLiteral(ExpressionTree expression) {
    return expression.is(Tree.Kind.BOOLEAN_LITERAL) && !Boolean.parseBoolean(((LiteralTree) expression).value());
  }

  private static boolean isExceptionArgument(Tree tree) {
    return checkParentRecursively(tree,3);
  }

  private static boolean checkParentRecursively(Tree tree, int attempts) {
    Tree current = tree.parent();
    while (current != null && attempts > 0) {
      if (current.is(Tree.Kind.NEW_CLASS)) {
        NewClassTree newClassTree = (NewClassTree) current;
        if (newClassTree.symbolType().isSubtypeOf("java.lang.Throwable")) {
          return true;
        }
      }
      current = current.parent();
      attempts--;
    }
    return false;
  }
}
