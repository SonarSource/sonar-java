/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.checks.methods.NameCriteria;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayDeque;
import java.util.Deque;

@Rule(
  key = "S2699",
  name = "Tests should include assertions",
  tags = {"junit"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleConstantRemediation("10min")
public class AssertionsInTestsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final MethodMatcher MOCKITO_VERIFY = MethodMatcher.create()
    .typeDefinition("org.mockito.Mockito").name("verify").withNoParameterConstraint();
  private static final MethodMatcher ASSERT_THAT = MethodMatcher.create()
    .typeDefinition(TypeCriteria.anyType()).name("assertThat").addParameter(TypeCriteria.anyType());
  private static final MethodMatcher FEST_AS_METHOD = MethodMatcher.create()
    .typeDefinition(TypeCriteria.anyType()).name("as").withNoParameterConstraint();
  private static final MethodMatcher FEST_DESCRIBED_AS_METHOD = MethodMatcher.create()
    .typeDefinition(TypeCriteria.anyType()).name("describedAs").withNoParameterConstraint();
  private static final MethodMatcher FEST_OVERRIDE_ERROR_METHOD = MethodMatcher.create()
    .typeDefinition(TypeCriteria.anyType()).name("overridingErrorMessage").withNoParameterConstraint();
  private static final MethodInvocationMatcherCollection ASSERTION_INVOCATION_MATCHERS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create().typeDefinition("org.junit.Assert").name(NameCriteria.startsWith("assert")).withNoParameterConstraint(),
    MethodMatcher.create().typeDefinition("org.junit.Assert").name("fail").withNoParameterConstraint(),
    MethodMatcher.create().typeDefinition("org.junit.rules.ExpectedException").name(NameCriteria.startsWith("expect")).withNoParameterConstraint(),
    MethodMatcher.create().typeDefinition("junit.framework.Assert").name(NameCriteria.startsWith("assert")).withNoParameterConstraint(),
    MethodMatcher.create().typeDefinition("junit.framework.Assert").name(NameCriteria.startsWith("fail")).withNoParameterConstraint(),
    // fest 1.x
    MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("org.fest.assertions.GenericAssert")).name(NameCriteria.any()).withNoParameterConstraint(),
    MethodMatcher.create().typeDefinition("org.fest.assertions.Fail").name(NameCriteria.startsWith("fail")).withNoParameterConstraint(),
    // fest 2.x
    MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("org.fest.assertions.api.AbstractAssert")).name(NameCriteria.any()).withNoParameterConstraint(),
    MethodMatcher.create().typeDefinition("org.fest.assertions.api.Fail").name(NameCriteria.startsWith("fail")).withNoParameterConstraint(),
    // Mockito
    MethodMatcher.create().typeDefinition("org.mockito.Mockito").name("verifyNoMoreInteractions").withNoParameterConstraint()
  );

  private final Deque<Boolean> methodContainsAssertion = new ArrayDeque<>();
  private final Deque<Boolean> inUnitTest = new ArrayDeque<>();
  private final Deque<ChainedMethods> chainedTo = new ArrayDeque<>();
  private JavaFileScannerContext context;

  private enum ChainedMethods {
    NONE,
    ASSERT_THAT,
    MOCKITO_VERIFY
  }

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree methodTree) {
    boolean isUnitTest = isUnitTest(methodTree);
    inUnitTest.push(isUnitTest);
    methodContainsAssertion.push(false);
    super.visitMethod(methodTree);
    inUnitTest.pop();
    Boolean containsAssertion = methodContainsAssertion.pop();
    if (isUnitTest && !containsAssertion) {
      context.addIssue(methodTree, this, "Add at least one assertion to this test case.");
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    if (inUnitTest.isEmpty() || !inUnitTest.peek() || methodContainsAssertion.peek()) {
      // if not in test case or assertion already found let's skip method
      return;
    }
    chainedTo.push(ChainedMethods.NONE);
    super.visitMethodInvocation(mit);
    ChainedMethods chainedToResult = chainedTo.pop();
    if (containsAssertion(mit, chainedToResult)) {
      methodContainsAssertion.pop();
      methodContainsAssertion.push(Boolean.TRUE);
    }
    if (!chainedTo.isEmpty()) {
      if (ChainedMethods.ASSERT_THAT.equals(chainedToResult) || ASSERT_THAT.matches(mit)) {
        chainedTo.pop();
        chainedTo.push(ChainedMethods.ASSERT_THAT);
      } else if (MOCKITO_VERIFY.matches(mit)) {
        chainedTo.pop();
        chainedTo.push(ChainedMethods.MOCKITO_VERIFY);
      }
    }
  }

  private static boolean containsAssertion(MethodInvocationTree mit, ChainedMethods chainedToResult) {
    // ignore assertThat chained with bad resolution method invocations
    boolean isChainedToAssertThatWithBadResolution = ChainedMethods.ASSERT_THAT.equals(chainedToResult) && mit.symbol().isUnknown();
    boolean isChainedToVerify = ChainedMethods.MOCKITO_VERIFY.equals(chainedToResult);
    return isChainedToVerify || isChainedToAssertThatWithBadResolution || isAssertion(mit);
  }

  private static boolean isAssertion(MethodInvocationTree mit) {
    return ASSERTION_INVOCATION_MATCHERS.anyMatch(mit) &&
      !FEST_AS_METHOD.matches(mit) &&
      !FEST_OVERRIDE_ERROR_METHOD.matches(mit) &&
      !FEST_DESCRIBED_AS_METHOD.matches(mit);
  }

  private static boolean isUnitTest(MethodTree methodTree) {
    if (methodTree.symbol().metadata().isAnnotatedWith("org.junit.Test")) {
      return true;
    }
    Symbol.TypeSymbol enclosingClass = methodTree.symbol().enclosingClass();
    return enclosingClass != null && enclosingClass.type().isSubtypeOf("junit.framework.TestCase") && methodTree.simpleName().name().startsWith("test");
  }

}
