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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4635")
public class StringOffsetMethodsCheck extends AbstractMethodDetection {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String INT = "int";
  private static final MethodMatchers SUBSTRING = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("substring")
    .addParametersMatcher(INT)
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes(JAVA_LANG_STRING)
        .names("indexOf", "lastIndexOf")
        .addParametersMatcher(INT)
        .build(),
      MethodMatchers.create()
        .ofTypes(JAVA_LANG_STRING)
        .names("indexOf", "lastIndexOf", "startsWith")
        .addParametersMatcher(JAVA_LANG_STRING)
        .build());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    // defensive programming : methodSelect can only be a MemberSelect (methods are instance method of java.lang.String).
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION) && SUBSTRING.matches(((MethodInvocationTree) expression).methodSymbol())) {
        reportIssue(ExpressionUtils.methodName((MethodInvocationTree) expression), mit,
          String.format("Replace \"%s\" with the overload that accepts an offset parameter.", mit.methodSymbol().name()));
      }
    }
  }
}
