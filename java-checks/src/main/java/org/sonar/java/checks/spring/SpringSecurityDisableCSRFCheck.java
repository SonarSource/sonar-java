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
package org.sonar.java.checks.spring;

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4502")
public class SpringSecurityDisableCSRFCheck extends AbstractMethodDetection {

  private static final String CSRF_CONFIGURER_CLASS = "org.springframework.security.config.annotation.web.configurers.CsrfConfigurer";
  private static final String HTTP_SECURITY = "org.springframework.security.config.annotation.web.builders.HttpSecurity";
  private static final String SESSION_CONFIGURER = "org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer";
  private static final String SESSION_POLICY = "org.springframework.security.config.http.SessionCreationPolicy";
  private static final String RESOURCE_SERVER = "org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer";
  private static final String MESSAGE = "Make sure disabling Spring Security's CSRF protection is safe here.";

  private static final MethodMatchers DISALLOWED_METHODS = MethodMatchers.create()
      .ofSubTypes("org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer")
      .names("disable", "ignoringAntMatchers", "requireCsrfProtectionMatcher", "ignoringRequestMatchers")
      .withAnyParameters()
      .build();

  private static final MethodMatchers DISABLE = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer")
    .names("disable").addWithoutParametersMatcher().build();
  private static final MethodMatchers SESSION_CREATION_POLICY = MethodMatchers.create()
    .ofSubTypes(SESSION_CONFIGURER)
    .names("sessionCreationPolicy").addParametersMatcher(SESSION_POLICY).build();
  private static final MethodMatchers OAUTH2_RESOURCE_SERVER = MethodMatchers.create()
    .ofTypes(HTTP_SECURITY).names("oauth2ResourceServer").withAnyParameters().build();
  private static final MethodMatchers SESSION_MANAGEMENT = MethodMatchers.create()
    .ofTypes(HTTP_SECURITY).names("sessionManagement").withAnyParameters().build();
  private static final MethodMatchers GET_SHARED_OBJECT = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder")
    .names("getSharedObject").addParametersMatcher("java.lang.Class").build();
  private static final MethodMatchers DEFAULT_CUSTOMIZER = MethodMatchers.create()
    .ofTypes("org.springframework.security.config.Customizer").names("withDefaults").addWithoutParametersMatcher().build();
  private static final MethodMatchers BROWSER_AUTHENTICATION = MethodMatchers.create()
    .ofTypes(HTTP_SECURITY).names("httpBasic", "formLogin", "rememberMe", "oauth2Login", "x509", "jee", "saml2Login")
    .withAnyParameters().build();
  private static final MethodMatchers REQUIRE_CSRF_MATCHER = MethodMatchers.create()
    .ofTypes(CSRF_CONFIGURER_CLASS).names("requireCsrfProtectionMatcher")
    .addParametersMatcher("org.springframework.security.web.util.matcher.RequestMatcher").build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return DISALLOWED_METHODS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree selectExpression = (MemberSelectExpressionTree) mit.methodSelect();
      if (selectExpression.expression().symbolType().is(CSRF_CONFIGURER_CLASS) && !isSafeDisabling(mit)) {
        reportIssue(selectExpression.identifier(), MESSAGE);
      }
    }
  }

  @Override
  protected void onMethodReferenceFound(MethodReferenceTree methodReferenceTree) {
    var typeArgs = methodReferenceTree.symbolType().typeArguments();
    if (typeArgs.size() == 1 && typeArgs.get(0).is(CSRF_CONFIGURER_CLASS)
      && !(DISABLE.matches(methodReferenceTree) && isStatelessResourceServer(methodReferenceTree))) {
      reportIssue(methodReferenceTree.method(), MESSAGE);
    }
  }

  private static boolean isSafeDisabling(MethodInvocationTree invocation) {
    return (DISABLE.matches(invocation) || isAlwaysFalseMatcher(invocation)) && isStatelessResourceServer(invocation);
  }

  private static boolean isAlwaysFalseMatcher(MethodInvocationTree invocation) {
    if (!REQUIRE_CSRF_MATCHER.matches(invocation)) {
      return false;
    }
    ExpressionTree matcher = ExpressionUtils.skipParentheses(invocation.arguments().get(0));
    if (!(matcher instanceof LambdaExpressionTree lambda)) {
      return false;
    }
    Tree body = lambda.body();
    if (body instanceof BlockTree block && block.body().size() == 1 && block.body().get(0) instanceof ReturnStatementTree returned) {
      body = returned.expression();
    }
    return body instanceof ExpressionTree expression && LiteralUtils.isFalse(ExpressionUtils.skipParentheses(expression));
  }

  private static boolean isStatelessResourceServer(Tree disabling) {
    MethodTree method = (MethodTree) ExpressionUtils.getParentOfType(disabling, Tree.Kind.METHOD);
    if (method == null || method.block() == null
      || !method.symbol().returnType().type().isSubtypeOf("org.springframework.security.web.SecurityFilterChain")
      || !method.symbol().metadata().isAnnotatedWith("org.springframework.context.annotation.Bean")
      || method.block().body().stream().anyMatch(statement -> !statement.is(Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.RETURN_STATEMENT))) {
      return false;
    }
    var builders = method.parameters().stream().filter(parameter -> parameter.symbol().type().is(HTTP_SECURITY)).toList();
    if (builders.size() != 1) {
      return false;
    }
    Symbol builder = builders.get(0).symbol();
    if (!builder.equals(builderOf(disabling)) || builder.usages().stream().anyMatch(usage -> !isReceiverUsage(usage))) {
      return false;
    }
    List<MethodInvocationTree> invocations = new ArrayList<>();
    List<IdentifierTree> escapingConfigurers = new ArrayList<>();
    method.block().accept(new BaseTreeVisitor() {
      @Override
      public void visitMethodInvocation(MethodInvocationTree invocation) {
        invocations.add(invocation);
        super.visitMethodInvocation(invocation);
      }

      @Override
      public void visitLambdaExpression(LambdaExpressionTree lambda) {
        lambda.parameters().stream()
          .filter(parameter -> parameter.symbol().type().is(SESSION_CONFIGURER) || parameter.symbol().type().is(RESOURCE_SERVER))
          .filter(parameter -> parameter.symbol().usages().stream().anyMatch(usage -> !isReceiverUsage(usage)))
          .forEach(parameter -> escapingConfigurers.add(parameter.simpleName()));
        super.visitLambdaExpression(lambda);
      }
    });
    var returns = method.block().body().stream().filter(statement -> statement.is(Tree.Kind.RETURN_STATEMENT)).toList();
    if (returns.size() != 1 || !builder.equals(builderOf(((ReturnStatementTree) returns.get(0)).expression()))) {
      return false;
    }
    if (escapingConfigurers.stream().anyMatch(identifier -> builder.equals(builderOf(identifier)))) {
      return false;
    }
    var configuration = invocations.stream().filter(invocation -> builder.equals(builderOf(invocation))).toList();
    return configuration.stream().anyMatch(invocation -> OAUTH2_RESOURCE_SERVER.matches(invocation) && !isConditional(invocation))
      && configuration.stream().noneMatch(invocation -> BROWSER_AUTHENTICATION.matches(invocation) && !isDisabledConfigurer(invocation))
      && configuration.stream().noneMatch(SpringSecurityDisableCSRFCheck::hasUnresolvedRequiredCustomizer)
      && configuration.stream().noneMatch(invocation -> DISABLE.matches(invocation) && isRequiredConfigurerReceiver(invocation))
      && configuration.stream().anyMatch(SpringSecurityDisableCSRFCheck::isStatelessPolicy)
      && configuration.stream().noneMatch(invocation -> SESSION_CREATION_POLICY.matches(invocation) && !isStatelessPolicy(invocation));
  }

  private static boolean isReceiverUsage(IdentifierTree identifier) {
    Tree expression = ExpressionUtils.skipParenthesesUpwards(identifier);
    boolean receiver = false;
    while (expression.parent() instanceof MemberSelectExpressionTree select && select.expression() == expression
      && select.parent() instanceof MethodInvocationTree invocation && invocation.methodSelect() == select) {
      receiver = true;
      expression = ExpressionUtils.skipParenthesesUpwards(invocation);
    }
    return receiver && (expression.parent().is(Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.LAMBDA_EXPRESSION)
      || (expression.parent().is(Tree.Kind.RETURN_STATEMENT)
        && ((ExpressionTree) expression).symbolType().isSubtypeOf("org.springframework.security.web.SecurityFilterChain")));
  }

  private static boolean hasUnresolvedRequiredCustomizer(MethodInvocationTree invocation) {
    return (OAUTH2_RESOURCE_SERVER.matches(invocation) || SESSION_MANAGEMENT.matches(invocation)) && invocation.arguments().stream()
      .map(ExpressionUtils::skipParentheses)
      .anyMatch(argument -> !argument.is(Tree.Kind.LAMBDA_EXPRESSION)
        && !(argument instanceof MethodInvocationTree customizer && DEFAULT_CUSTOMIZER.matches(customizer)));
  }

  /**
   * Whether a configurer call such as {@code httpBasic} only disables that configurer, in one of the three forms
   * {@code httpBasic().disable()}, {@code httpBasic(AbstractHttpConfigurer::disable)} or {@code httpBasic(c -> c.disable())}.
   */
  private static boolean isDisabledConfigurer(MethodInvocationTree invocation) {
    if (invocation.arguments().isEmpty()) {
      return invocation.parent() instanceof MemberSelectExpressionTree select
        && select.parent() instanceof MethodInvocationTree disable
        && disable.methodSelect() == select
        && DISABLE.matches(disable);
    }
    ExpressionTree customizer = ExpressionUtils.skipParentheses(invocation.arguments().get(0));
    if (customizer instanceof MethodReferenceTree reference) {
      return DISABLE.matches(reference);
    }
    return customizer instanceof LambdaExpressionTree lambda
      && lambda.parameters().size() == 1
      && lambda.body() instanceof MethodInvocationTree body
      && DISABLE.matches(body)
      && body.methodSelect() instanceof MemberSelectExpressionTree select
      && select.expression() instanceof IdentifierTree receiver
      && receiver.symbol().equals(lambda.parameters().get(0).symbol());
  }

  private static boolean isRequiredConfigurerReceiver(MethodInvocationTree invocation) {
    return invocation.methodSelect() instanceof MemberSelectExpressionTree select
      && (select.expression().symbolType().is(RESOURCE_SERVER) || select.expression().symbolType().is(SESSION_CONFIGURER));
  }

  private static boolean isStatelessPolicy(MethodInvocationTree invocation) {
    if (!SESSION_CREATION_POLICY.matches(invocation) || isConditional(invocation)) {
      return false;
    }
    ExpressionTree policy = ExpressionUtils.skipParentheses(invocation.arguments().get(0));
    if (policy instanceof MemberSelectExpressionTree select) {
      policy = select.identifier();
    }
    return policy instanceof IdentifierTree identifier
      && identifier.symbol().owner().type().is(SESSION_POLICY) && "STATELESS".equals(identifier.name());
  }

  private static boolean isConditional(Tree tree) {
    for (Tree parent = tree.parent(); parent != null && !parent.is(Tree.Kind.METHOD); parent = parent.parent()) {
      if (parent instanceof LambdaExpressionTree lambda && lambda.body() instanceof BlockTree block
        && block.body().stream().anyMatch(statement -> !statement.is(Tree.Kind.EXPRESSION_STATEMENT))) {
        return true;
      }
      if (parent instanceof LambdaExpressionTree lambda
        && !(lambda.symbolType().is("org.springframework.security.config.Customizer")
          && lambda.parent().parent() instanceof MethodInvocationTree invocation
          && invocation.methodSelect() instanceof MemberSelectExpressionTree select && select.expression().symbolType().is(HTTP_SECURITY))) {
        return true;
      }
      if (parent.is(Tree.Kind.IF_STATEMENT, Tree.Kind.CONDITIONAL_EXPRESSION, Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR,
        Tree.Kind.SWITCH_STATEMENT, Tree.Kind.SWITCH_EXPRESSION, Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT,
        Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.TRY_STATEMENT)) {
        return true;
      }
    }
    return false;
  }

  private static Symbol builderOf(Tree tree) {
    if (tree instanceof MethodReferenceTree reference) {
      return builderOf(ExpressionUtils.getParentOfType(reference, Tree.Kind.METHOD_INVOCATION));
    }
    if (tree instanceof MethodInvocationTree invocation && !GET_SHARED_OBJECT.matches(invocation)
      && invocation.methodSelect() instanceof MemberSelectExpressionTree select) {
      return builderOf(select.expression());
    }
    if (tree instanceof IdentifierTree identifier) {
      if (identifier.symbolType().is(HTTP_SECURITY)) {
        return identifier.symbol();
      }
      Tree declaration = identifier.symbol().declaration();
      Tree lambda = declaration == null ? null : ExpressionUtils.getParentOfType(declaration, Tree.Kind.LAMBDA_EXPRESSION);
      if (lambda instanceof LambdaExpressionTree expression && expression.parameters().contains(declaration)) {
        return builderOf(ExpressionUtils.getParentOfType(lambda, Tree.Kind.METHOD_INVOCATION));
      }
    }
    return null;
  }
}
