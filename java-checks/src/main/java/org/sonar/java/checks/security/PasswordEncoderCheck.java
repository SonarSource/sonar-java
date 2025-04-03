/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5344")
public class PasswordEncoderCheck extends IssuableSubscriptionVisitor {

  private static final String JAVAX_CRYPTO_MESSAGE_FORMAT = "Use at least %d PBKDF2 iterations.";

  private static final Map<String, Integer> MIN_ITERATIONS_BY_ALGORITHM = Map.of(
    "PBKDF2withHmacSHA1",
    1_300_000,
    "PBKDF2withHmacSHA256",
    600_000,
    "PBKDF2withHmacSHA512",
    210_000
  );

  private static final MethodMatchers JDBC_AUTHENTICATION = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder")
    .names("jdbcAuthentication")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers USER_DETAIL_SERVICE = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder")
    .names("userDetailsService")
    .withAnyParameters()
    .build();

  private static final MethodMatchers PASSWORD_ENCODER_SETTER = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.config.annotation.authentication.configurers.userdetails.AbstractDaoAuthenticationConfigurer")
    .names("passwordEncoder")
    .withAnyParameters()
    .build();

  private static final MethodMatchers UNSAFE_PASSWORD_ENCODER_CONSTRUCTORS = MethodMatchers.create()
    .ofTypes(
      "org.springframework.security.authentication.encoding.ShaPasswordEncoder",
      "org.springframework.security.authentication.encoding.Md5PasswordEncoder",
      "org.springframework.security.crypto.password.LdapShaPasswordEncoder",
      "org.springframework.security.crypto.password.Md4PasswordEncoder",
      "org.springframework.security.crypto.password.MessageDigestPasswordEncoder",
      "org.springframework.security.crypto.password.StandardPasswordEncoder",
      "org.springframework.security.crypto.scrypt.SCryptPasswordEncoder")
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers UNSAFE_PASSWORD_ENCODER_METHODS = MethodMatchers.create()
    .ofTypes("org.springframework.security.crypto.password.NoOpPasswordEncoder")
    .names("getInstance")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers SECRET_KEY_FACTORY_GENERATE_SECRET_METHOD = MethodMatchers.create()
    .ofTypes("javax.crypto.SecretKeyFactory")
    .names("generateSecret")
    .addParametersMatcher("java.security.spec.KeySpec")
    .build();

  private static final MethodMatchers SECRET_KEY_FACTORY_GET_INSTANCE_METHOD = MethodMatchers.create()
    .ofTypes("javax.crypto.SecretKeyFactory")
    .names("getInstance")
    .addParametersMatcher("java.lang.String")
    .build();

  private static final MethodMatchers PBE_KEY_SPEC_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes("javax.crypto.spec.PBEKeySpec")
    .constructor()
    .addParametersMatcher("char[]", "byte[]", "int")
    .addParametersMatcher("char[]", "byte[]", "int", "int")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof NewClassTree nct && UNSAFE_PASSWORD_ENCODER_CONSTRUCTORS.matches(nct)) {
      reportIssue(nct.identifier(), "Use secure \"PasswordEncoder\" implementation.");
    } else if (tree instanceof MethodInvocationTree mit && UNSAFE_PASSWORD_ENCODER_METHODS.matches(mit)) {
      reportIssue(ExpressionUtils.methodName(mit), "Use secure \"PasswordEncoder\" implementation.");
    } else if (tree instanceof MethodInvocationTree mit && SECRET_KEY_FACTORY_GENERATE_SECRET_METHOD.matches(mit)) {
      checkJavaxCrypto(mit);
    } else if (tree.is(Tree.Kind.METHOD)) {
      MethodInvocationVisitor visitor = new MethodInvocationVisitor();
      tree.accept(visitor);
      if (visitor.hasAuthentication && !visitor.setsPasswordEncoder) {
        reportIssue(visitor.tree, "Don't use the default \"PasswordEncoder\" relying on plain-text.");
      }
    }
  }

  private void checkJavaxCrypto(MethodInvocationTree mit) {
    var algorithmValueExpressionAndValue = Optional.of(mit.methodSelect())
      .filter(e -> e.is(Tree.Kind.MEMBER_SELECT))
      .map(MemberSelectExpressionTree.class::cast)
      .map(MemberSelectExpressionTree::expression)
      .flatMap(PasswordEncoderCheck::extractAlgorithm);
    if (algorithmValueExpressionAndValue.isEmpty()) {
      return;
    }

    var algorithmValueExpression = algorithmValueExpressionAndValue.get().initializerExpression();
    var algorithm = algorithmValueExpressionAndValue.get().value();
    if (!MIN_ITERATIONS_BY_ALGORITHM.containsKey(algorithm)) {
      return;
    }

    var iterationCountExpressionsAndValue = extractIterationCount(mit.arguments().get(0));
    if (iterationCountExpressionsAndValue.isEmpty()) {
      return;
    }

    var iterationCountExpression = iterationCountExpressionsAndValue.get().expression();
    var iterationCountValueExpression = iterationCountExpressionsAndValue.get().initializerExpression();
    var iterationCount = iterationCountExpressionsAndValue.get().value();

    var minIteration = MIN_ITERATIONS_BY_ALGORITHM.get(algorithm);
    if (iterationCount < minIteration) {
      var secondaryLocations = new ArrayList<Location>();
      secondaryLocations.add(new Location("", algorithmValueExpression));
      if (!Objects.equals(iterationCountValueExpression.firstToken(), iterationCountExpression.firstToken())) {
        secondaryLocations.add(new Location("", iterationCountValueExpression));
      }

      reportIssue(iterationCountExpression, JAVAX_CRYPTO_MESSAGE_FORMAT.formatted(minIteration), secondaryLocations, null);
    }
  }

  // Given secretKeyFactory.generateSecret(keySpec), where var keySpec = new PBEKeySpec(..., iterations, ...), where
  // iterations is var iterations = y, returns the expressions "iterations", "y", as well as "y" value (e.g. 120000).
  private static Optional<ExpressionsAndValue<Integer>> extractIterationCount(ExpressionTree keySpecArgumentExpression) {
    return getInitializerIfExpressionIsVariableIdentifier(keySpecArgumentExpression)
      // try to resolve the expression itself to a constructor invocation
      .or(() -> Optional.of(keySpecArgumentExpression))
      .filter(e -> e.is(Tree.Kind.NEW_CLASS))
      .map(NewClassTree.class::cast)
      .filter(PBE_KEY_SPEC_CONSTRUCTOR::matches)
      .map(gi -> gi.arguments().get(2))
      .flatMap(e -> getValueIfExpressionIsVariableIdentifier(e, Integer.class));
  }

  // Given secretKeyFactory.getInstance(algorithm), where var algorithm = y, returns the expressions "algorithm",
  // "y", as well as "y" value (e.g. "PBKDF2withHmacSHA512").
  private static Optional<ExpressionsAndValue<String>> extractAlgorithm(ExpressionTree secretKeyFactoryExpression) {
    return getInitializerIfExpressionIsVariableIdentifier(secretKeyFactoryExpression)
      // try to resolve the expression itself to a method invocation
      .or(() -> Optional.of(secretKeyFactoryExpression))
      .filter(e -> e.is(Tree.Kind.METHOD_INVOCATION))
      .map(MethodInvocationTree.class::cast)
      .filter(SECRET_KEY_FACTORY_GET_INSTANCE_METHOD::matches)
      .map(gi -> gi.arguments().get(0))
      .flatMap(e -> getValueIfExpressionIsVariableIdentifier(e, String.class));
  }

  // Given any expression e, attempts constant value resolution of the expression itself and of the initializer
  // of the declaration of e, if any.
  private static <T> Optional<ExpressionsAndValue<T>> getValueIfExpressionIsVariableIdentifier(ExpressionTree expression, Class<T> type) {
    var initializerExpression = getInitializerIfExpressionIsVariableIdentifier(expression);
    return initializerExpression
      .flatMap(e -> e.asConstant(type))
      .map(v -> new ExpressionsAndValue<>(expression, initializerExpression.get(), v))
      .or(() -> expression.asConstant(type).map(t -> new ExpressionsAndValue<>(expression, expression, t)));
  }

  // Given any identifier "x", returns the initializer y from the declaration of x: "var x = y" (whether in the same
  // statement or not, whether it was reassigned between declaration and reference or not).
  private static Optional<ExpressionTree> getInitializerIfExpressionIsVariableIdentifier(ExpressionTree expression) {
    return Optional.of(expression)
      .filter(e -> e.is(Tree.Kind.IDENTIFIER))
      .map(IdentifierTree.class::cast)
      .map(IdentifierTree::symbol)
      .map(Symbol::declaration)
      .filter(e -> e.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .map(VariableTree::initializer);
  }

  static class MethodInvocationVisitor extends BaseTreeVisitor {

    private boolean hasAuthentication;
    private boolean setsPasswordEncoder;
    private MethodInvocationTree tree;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (JDBC_AUTHENTICATION.matches(tree) || USER_DETAIL_SERVICE.matches(tree)) {
        hasAuthentication = true;
        this.tree = tree;
      }
      if (PASSWORD_ENCODER_SETTER.matches(tree)) {
        setsPasswordEncoder = true;
      }
      super.visitMethodInvocation(tree);
    }
  }

  private record ExpressionsAndValue<T>(ExpressionTree expression, ExpressionTree initializerExpression, T value) {
  }
}
