/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.spring;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4502")
public class SpringSecurityDisableCSRFCheck extends AbstractMethodDetection {

  private static final String CSRF_CONFIGURER_CLASS = "org.springframework.security.config.annotation.web.configurers.CsrfConfigurer";
  private static final String MESSAGE = "Make sure disabling Spring Security's CSRF protection is safe here.";

  private static final MethodMatchers DISALLOWED_METHODS = MethodMatchers.create()
      .ofSubTypes("org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer")
      .names("disable", "ignoringAntMatchers", "requireCsrfProtectionMatcher", "ignoringRequestMatchers")
      .withAnyParameters()
      .build();


  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return DISALLOWED_METHODS;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree selectExpression = (MemberSelectExpressionTree) mit.methodSelect();
      if (selectExpression.expression().symbolType().is(CSRF_CONFIGURER_CLASS)) {
        reportIssue(selectExpression.identifier(), MESSAGE);
      }
    }
  }

  @Override
  protected void onMethodReferenceFound(MethodReferenceTree methodReferenceTree) {
    var typeArgs = methodReferenceTree.symbolType().typeArguments();
    if (typeArgs.size() == 1 && typeArgs.get(0).is(CSRF_CONFIGURER_CLASS)) {
      reportIssue(methodReferenceTree.method(), MESSAGE);
    }
  }
}
