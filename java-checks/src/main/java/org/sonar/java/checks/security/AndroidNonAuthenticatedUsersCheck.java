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
package org.sonar.java.checks.security;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.MethodTreeUtils.subsequentMethodInvocation;

@Rule(key = "S6288")
public class AndroidNonAuthenticatedUsersCheck extends AbstractMethodDetection {

  private static final String KEY_GEN_PARAMETER_SPEC_BUILDER_TYPE = "android.security.keystore.KeyGenParameterSpec$Builder";

  private static final MethodMatchers KEY_GEN_BUILDER_BUILD = MethodMatchers.create()
    .ofTypes(KEY_GEN_PARAMETER_SPEC_BUILDER_TYPE)
    .names("build")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers KEY_GEN_BUILDER_SET_AUTH_MATCHER = MethodMatchers.create()
    .ofTypes(KEY_GEN_PARAMETER_SPEC_BUILDER_TYPE)
    .names("setUserAuthenticationRequired")
    .addParametersMatcher("boolean")
    .build();

  private static final MethodMatchers KEY_GEN_BUILDER = MethodMatchers.create()
    .ofTypes(KEY_GEN_PARAMETER_SPEC_BUILDER_TYPE)
    .constructor()
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return KEY_GEN_BUILDER_BUILD;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    // Bottom-up approach, start from the method select of the "build()" method
    getNotAuthenticatedConstructor(mit.methodSelect()).ifPresent(newClassTree ->
      reportIssue(newClassTree.identifier(), "Make sure authorizing non-authenticated users to use this key is safe here.")
    );
  }

  private static Optional<NewClassTree> getNotAuthenticatedConstructor(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      expression = ((MemberSelectExpressionTree) expression).expression();
    }

    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      if (!authenticate(mit)) {
        return getNotAuthenticatedConstructor(mit.methodSelect());
      }
    } else if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      if (symbol.usages().stream().anyMatch(AndroidNonAuthenticatedUsersCheck::canAuthenticate)) {
        return Optional.empty();
      }
      return getNotAuthenticatedConstructorInDeclaration(symbol);
    } else if (expression.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) expression;
      if (KEY_GEN_BUILDER.matches(newClassTree)) {
        return Optional.of(newClassTree);
      }
    }
    return Optional.empty();
  }

  private static Optional<NewClassTree> getNotAuthenticatedConstructorInDeclaration(Symbol symbol) {
    if (symbol.isLocalVariable()) {
      Tree declaration = symbol.declaration();
      if (declaration instanceof VariableTree) {
        ExpressionTree initializer = ((VariableTree) declaration).initializer();
        if (initializer != null) {
          return getNotAuthenticatedConstructor(initializer);
        }
      }
    }
    return Optional.empty();
  }

  private static boolean canAuthenticate(IdentifierTree tokenIdentifier) {
    Tree parent = tokenIdentifier.parent();
    if (parent != null && parent.is(Tree.Kind.ARGUMENTS)) {
      // When given as argument, we consider it as signed to avoid FP.
      return true;
    }
    Optional<MethodInvocationTree> subsequentInvocation = subsequentMethodInvocation(tokenIdentifier, KEY_GEN_BUILDER_SET_AUTH_MATCHER);
    return subsequentInvocation.isPresent() && LiteralUtils.isTrue(subsequentInvocation.get().arguments().get(0));
  }

  private static boolean authenticate(MethodInvocationTree mit) {
    return KEY_GEN_BUILDER_SET_AUTH_MATCHER.matches(mit) && LiteralUtils.isTrue(mit.arguments().get(0));
  }

}
