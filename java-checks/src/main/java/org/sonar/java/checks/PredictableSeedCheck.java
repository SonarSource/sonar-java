/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4347")
public class PredictableSeedCheck extends AbstractMethodDetection {
  private static final Tree.Kind[] LITERAL_KINDS = {Tree.Kind.STRING_LITERAL, Tree.Kind.INT_LITERAL, Tree.Kind.LONG_LITERAL, Tree.Kind.CHAR_LITERAL,
    Tree.Kind.NULL_LITERAL, Tree.Kind.BOOLEAN_LITERAL, Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL};
  private static final String JAVA_SECURITY_SECURE_RANDOM = "java.security.SecureRandom";
  private static final MethodMatchers GET_BYTES = MethodMatchers.create()
    .ofTypes("java.lang.String")
    .names("getBytes")
    .withAnyParameters()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes(JAVA_SECURITY_SECURE_RANDOM)
        .constructor()
        .addParametersMatcher("byte[]")
        .build(),
      MethodMatchers.create()
        .ofTypes(JAVA_SECURITY_SECURE_RANDOM)
        .names("setSeed")
        .addParametersMatcher("byte[]")
        .addParametersMatcher("long")
        .build());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    checkSeed(mit.arguments().get(0));
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    checkSeed(newClassTree.arguments().get(0));
  }

  private void checkSeed(ExpressionTree seedExpression) {
    if (isPredictable(seedExpression)) {
      reportIssue(seedExpression, "Change this seed value to something unpredictable, or remove the seed.");
    }
  }

  private static boolean isPredictable(ExpressionTree expressionTree) {
    return expressionTree.is(LITERAL_KINDS)
      || expressionTree.is(Tree.Kind.NEW_ARRAY)
      || isStaticFinal(expressionTree)
      || isStringLiteralToBytes(expressionTree);
  }

  private static boolean isStringLiteralToBytes(ExpressionTree expressionTree) {
    if (!expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    MethodInvocationTree mit = (MethodInvocationTree) expressionTree;
    return GET_BYTES.matches(mit) && ((MemberSelectExpressionTree) mit.methodSelect()).expression().is(Tree.Kind.STRING_LITERAL);
  }

  private static boolean isStaticFinal(ExpressionTree expressionTree) {
    if (!expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return false;
    }
    Symbol symbol = ((IdentifierTree) expressionTree).symbol();
    return symbol.isStatic() && symbol.isFinal();
  }
}
