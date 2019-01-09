/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
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
  private static final MethodMatcher GET_BYTES = MethodMatcher.create().typeDefinition("java.lang.String").name("getBytes").withAnyParameters();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(JAVA_SECURITY_SECURE_RANDOM).name("<init>").parameters("byte[]"),
      MethodMatcher.create().typeDefinition(JAVA_SECURITY_SECURE_RANDOM).name("setSeed").parameters("byte[]"),
      MethodMatcher.create().typeDefinition(JAVA_SECURITY_SECURE_RANDOM).name("setSeed").parameters("long")
      );
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
      || isStaticFinal(expressionTree)
      || expressionTree.is(Tree.Kind.NEW_ARRAY)
      || isStringLiteralToBytes(expressionTree);
  }

  private static boolean isStringLiteralToBytes(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.METHOD_INVOCATION) && GET_BYTES.matches(((MethodInvocationTree) expressionTree))) {
      return ((MemberSelectExpressionTree) ((MethodInvocationTree) expressionTree).methodSelect()).expression().is(Tree.Kind.STRING_LITERAL);
    }
    return false;
  }

  private static boolean isStaticFinal(ExpressionTree expressionTree) {
    return expressionTree.is(Tree.Kind.IDENTIFIER)
      && ((IdentifierTree) expressionTree).symbol().isStatic()
      && ((IdentifierTree) expressionTree).symbol().isFinal();
  }
}
