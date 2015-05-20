/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.java.checks.methods.MethodInvocationMatcher;
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

  private static final String GENERIC_ASSERT = "org.fest.assertions.GenericAssert";
  private static final MethodInvocationMatcher FEST_AS_METHOD = MethodInvocationMatcher.create()
    .typeDefinition(GENERIC_ASSERT).name("as").withNoParameterConstraint();
  private static final MethodInvocationMatcher FEST_DESCRIBED_AS_METHOD = MethodInvocationMatcher.create()
    .typeDefinition(GENERIC_ASSERT).name("describedAs").withNoParameterConstraint();
  private static final MethodInvocationMatcher FEST_OVERRIDE_ERROR_METHOD = MethodInvocationMatcher.create()
    .typeDefinition(GENERIC_ASSERT).name("overridingErrorMessage").withNoParameterConstraint();
  private static final MethodInvocationMatcherCollection ASSERTION_INVOCATION_MATCHERS = MethodInvocationMatcherCollection.create(
    MethodInvocationMatcher.create().typeDefinition("org.junit.Assert").name(NameCriteria.startsWith("assert")).withNoParameterConstraint(),
    MethodInvocationMatcher.create().typeDefinition("org.junit.Assert").name("fail").withNoParameterConstraint(),
    MethodInvocationMatcher.create().typeDefinition("junit.framework.Assert").name(NameCriteria.startsWith("assert")).withNoParameterConstraint(),
    MethodInvocationMatcher.create().typeDefinition("junit.framework.Assert").name(NameCriteria.startsWith("fail")).withNoParameterConstraint(),
    MethodInvocationMatcher.create().typeDefinition("org.fest.assertions.Fail").name(NameCriteria.startsWith("fail")).withNoParameterConstraint(),
    MethodInvocationMatcher.create().typeDefinition(TypeCriteria.subtypeOf(GENERIC_ASSERT)).name(NameCriteria.any()).withNoParameterConstraint()
  );

  private Deque<Boolean> methodContainsAssertion = new ArrayDeque<>();
  private Deque<Boolean> inUnitTest = new ArrayDeque<>();
  private JavaFileScannerContext context;

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
    Boolean methodContainsAssertion = this.methodContainsAssertion.pop();
    if (isUnitTest && !methodContainsAssertion) {
      context.addIssue(methodTree, this, "Add at least one assertion to this test case.");
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    super.visitMethodInvocation(mit);
    if (!inUnitTest.isEmpty() && inUnitTest.peek() && !methodContainsAssertion.peek() && isAssertion(mit)) {
      methodContainsAssertion.pop();
      methodContainsAssertion.push(Boolean.TRUE);
    }
  }

  private boolean isAssertion(MethodInvocationTree mit) {
    if (ASSERTION_INVOCATION_MATCHERS.anyMatch(mit) &&
        !FEST_AS_METHOD.matches(mit) &&
        !FEST_OVERRIDE_ERROR_METHOD.matches(mit) &&
        !FEST_DESCRIBED_AS_METHOD.matches(mit)) {
      return true;
    }
    return false;
  }

  private boolean isUnitTest(MethodTree methodTree) {
    if (methodTree.symbol().metadata().isAnnotatedWith("org.junit.Test")) {
      return true;
    }
    Symbol.TypeSymbol enclosingClass = methodTree.symbol().enclosingClass();
    return enclosingClass != null && enclosingClass.type().isSubtypeOf("junit.framework.TestCase") && methodTree.simpleName().name().startsWith("test");
  }

}
