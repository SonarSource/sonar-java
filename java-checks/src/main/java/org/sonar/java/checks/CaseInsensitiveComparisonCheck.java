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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1157")
public class CaseInsensitiveComparisonCheck extends AbstractMethodDetection {

  private static final MethodMatchers TO_LOWER_UPPER_CASE = MethodMatchers.create()
    .ofSubTypes("java.lang.String")
    .names("toLowerCase", "toUpperCase")
    .addWithoutParametersMatcher()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofAnyType()
      .names("equals")
      .addParametersMatcher("java.lang.Object")
      .build();
  }

  @Override
  public void onMethodInvocationFound(MethodInvocationTree tree) {
    if (tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree.methodSelect();
      if (ignoresCase(memberSelect.expression(), tree.arguments().get(0))) {
        reportIssue(tree, "Replace these toUpperCase()/toLowerCase() and equals() calls with a single equalsIgnoreCase() call.");
      }
    }
  }

  private static boolean ignoresCase(ExpressionTree lhs, ExpressionTree rhs) {
    boolean lhsConverted = isToUpperCaseOrToLowerCase(lhs);
    boolean rhsConverted = isToUpperCaseOrToLowerCase(rhs);
    return (lhsConverted && (rhsConverted || isStringConstant(rhs))) || (rhsConverted && isStringConstant(lhs));
  }

  private static boolean isStringConstant(ExpressionTree expression) {
    return expression.asConstant(String.class).isPresent();
  }

  private static boolean isToUpperCaseOrToLowerCase(ExpressionTree expression) {
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocation = (MethodInvocationTree) expression;
      return TO_LOWER_UPPER_CASE.matches(methodInvocation);
    }
    return false;
  }

}
