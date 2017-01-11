/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.NameCriteria;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;

@Rule(key = "S2699")
public class AssertionsInTestsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String VERIFY = "verify";
  private static final String ASSERT_NAME = "assert";

  private static final TypeCriteria ORG_MOCKITO_MOCKITO = TypeCriteria.is("org.mockito.Mockito");
  private static final TypeCriteria ORG_ASSERTJ_ASSERTIONS = TypeCriteria.is("org.assertj.core.api.Assertions");
  private static final TypeCriteria ORG_ASSERTJ_FAIL = TypeCriteria.is("org.assertj.core.api.Fail");

  private static final TypeCriteria ANY_TYPE = TypeCriteria.anyType();
  private static final NameCriteria ANY_NAME = NameCriteria.any();
  private static final NameCriteria STARTS_WITH_FAIL = NameCriteria.startsWith("fail");
  private static final NameCriteria STARTS_WITH_ASSERT = NameCriteria.startsWith(ASSERT_NAME);

  private static final MethodMatcherCollection ASSERTION_INVOCATION_MATCHERS = MethodMatcherCollection.create(
    // junit
    method("org.junit.Assert", STARTS_WITH_ASSERT).withAnyParameters(),
    method("org.junit.Assert", STARTS_WITH_FAIL).withAnyParameters(),
    method("org.junit.rules.ExpectedException", NameCriteria.startsWith("expect")).withAnyParameters(),
    method(TypeCriteria.subtypeOf("junit.framework.Assert"), STARTS_WITH_ASSERT).withAnyParameters(),
    method(TypeCriteria.subtypeOf("junit.framework.Assert"), STARTS_WITH_FAIL).withAnyParameters(),
    method("org.junit.rules.ErrorCollector", "checkThat").withAnyParameters(),
    // fest 1.x
    method(TypeCriteria.subtypeOf("org.fest.assertions.GenericAssert"), ANY_NAME).withAnyParameters(),
    method("org.fest.assertions.Assertions", STARTS_WITH_ASSERT).withAnyParameters(),
    method("org.fest.assertions.Fail", STARTS_WITH_FAIL).withAnyParameters(),
    // fest 2.x
    method(TypeCriteria.subtypeOf("org.fest.assertions.api.AbstractAssert"), ANY_NAME).withAnyParameters(),
    method("org.fest.assertions.api.Fail", STARTS_WITH_FAIL).withAnyParameters(),
    // assertJ
    method(TypeCriteria.subtypeOf("org.assertj.core.api.AbstractAssert"), ANY_NAME).withAnyParameters(),
    method(ORG_ASSERTJ_FAIL, STARTS_WITH_FAIL).withAnyParameters(),
    method(ORG_ASSERTJ_FAIL, "shouldHaveThrown").withAnyParameters(),
    method(ORG_ASSERTJ_ASSERTIONS, STARTS_WITH_FAIL).withAnyParameters(),
    method(ORG_ASSERTJ_ASSERTIONS, "shouldHaveThrown").withAnyParameters(),
    method(ORG_ASSERTJ_ASSERTIONS, STARTS_WITH_ASSERT).withAnyParameters(),
    method(TypeCriteria.subtypeOf("org.assertj.core.api.AbstractSoftAssertions"), STARTS_WITH_ASSERT).withAnyParameters(),
    // hamcrest
    method("org.hamcrest.MatcherAssert", STARTS_WITH_ASSERT).withAnyParameters(),
    // Mockito
    method(ORG_MOCKITO_MOCKITO, NameCriteria.startsWith(VERIFY)).withAnyParameters(),
    // spring
    method("org.springframework.test.web.servlet.ResultActions", "andExpect").addParameter(ANY_TYPE),
    // EasyMock
    method("org.easymock.EasyMock", VERIFY).withAnyParameters(),
    method(TypeCriteria.subtypeOf("org.easymock.IMocksControl"), VERIFY).withAnyParameters(),
    method(TypeCriteria.subtypeOf("org.easymock.EasyMockSupport"), "verifyAll").withAnyParameters(),
    // Truth Framework
    method("com.google.common.truth.Truth", STARTS_WITH_ASSERT).withAnyParameters());

  private final Deque<Boolean> methodContainsAssertion = new ArrayDeque<>();
  private final Deque<Boolean> inUnitTest = new ArrayDeque<>();
  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    inUnitTest.push(false);
    methodContainsAssertion.push(false);
    scan(context.getTree());
    methodContainsAssertion.pop();
    inUnitTest.pop();
  }

  @Override
  public void visitMethod(MethodTree methodTree) {
    if (ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.ABSTRACT)) {
      return;
    }
    boolean isUnitTest = isUnitTest(methodTree);
    inUnitTest.push(isUnitTest);
    methodContainsAssertion.push(false);
    super.visitMethod(methodTree);
    Boolean containsAssertion = methodContainsAssertion.pop();
    inUnitTest.pop();
    if (isUnitTest && !expectAssertion(methodTree) && !containsAssertion) {
      context.reportIssue(this, methodTree.simpleName(), "Add at least one assertion to this test case.");
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    super.visitMethodInvocation(mit);
    if (!methodContainsAssertion.peek() && inUnitTest.peek() && ASSERTION_INVOCATION_MATCHERS.anyMatch(mit)) {
      methodContainsAssertion.pop();
      methodContainsAssertion.push(true);
    }
  }

  private static boolean expectAssertion(MethodTree methodTree) {
    List<SymbolMetadata.AnnotationValue> annotationValues = methodTree.symbol().metadata().valuesForAnnotation("org.junit.Test");
    if (annotationValues != null) {
      for (SymbolMetadata.AnnotationValue annotationValue : annotationValues) {
        if ("expected".equals(annotationValue.name())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isUnitTest(MethodTree methodTree) {
    JavaSymbol.MethodJavaSymbol symbol = (JavaSymbol.MethodJavaSymbol) methodTree.symbol();
    while (symbol != null) {
      if (symbol.metadata().isAnnotatedWith("org.junit.Test")) {
        return true;
      }
      symbol = symbol.overriddenSymbol();
    }
    Symbol.TypeSymbol enclosingClass = methodTree.symbol().enclosingClass();
    return enclosingClass != null && enclosingClass.type().isSubtypeOf("junit.framework.TestCase") && methodTree.simpleName().name().startsWith("test");
  }

  private static MethodMatcher method(String typeDefinition, String methodName) {
    return method(TypeCriteria.is(typeDefinition), NameCriteria.is(methodName));
  }

  private static MethodMatcher method(TypeCriteria typeDefinitionCriteria, String methodName) {
    return method(typeDefinitionCriteria, NameCriteria.is(methodName));
  }

  private static MethodMatcher method(String typeDefinition, NameCriteria nameCriteria) {
    return MethodMatcher.create().typeDefinition(TypeCriteria.is(typeDefinition)).name(nameCriteria);
  }

  private static MethodMatcher method(TypeCriteria typeDefinitionCriteria, NameCriteria nameCriteria) {
    return MethodMatcher.create().typeDefinition(typeDefinitionCriteria).name(nameCriteria);
  }

}
