/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Rule(key = "S2699")
public class AssertionsInTestsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String VERIFY = "verify";
  private static final String ASSERT_NAME = "assert";
  private static final String ASSERT_THAT_NAME = "assertThat";
  private static final String ORG_MOCKITO_MOCKITO = "org.mockito.Mockito";

  private static final TypeCriteria ANY_TYPE = TypeCriteria.anyType();
  private static final NameCriteria ANY_NAME = NameCriteria.any();
  private static final NameCriteria STARTS_WITH_FAIL = NameCriteria.startsWith("fail");

  private static final MethodMatcher MOCKITO_VERIFY = methodWithoutParameter(ORG_MOCKITO_MOCKITO, VERIFY);
  private static final MethodMatcher ASSERTJ_ASSERT_ALL = methodWithParameters("org.assertj.core.api.SoftAssertions", "assertAll");
  private static final MethodMatcher ASSERT_THAT = methodWithParameters(ANY_TYPE, ASSERT_THAT_NAME).addParameter(ANY_TYPE);
  private static final MethodMatcher FEST_AS_METHOD = methodWithoutParameter(ANY_TYPE, "as");
  private static final MethodMatcher FEST_DESCRIBED_AS_METHOD = methodWithoutParameter(ANY_TYPE, "describedAs");
  private static final MethodMatcher FEST_OVERRIDE_ERROR_METHOD = methodWithoutParameter(ANY_TYPE, "overridingErrorMessage");

  private static final MethodMatcherCollection ASSERTION_INVOCATION_MATCHERS = MethodMatcherCollection.create(
    // junit
    methodWithoutParameter("org.junit.Assert", NameCriteria.startsWith(ASSERT_NAME)),
    methodWithoutParameter("org.junit.Assert", "fail"),
    methodWithoutParameter("org.junit.rules.ExpectedException", NameCriteria.startsWith("expect")),
    methodWithoutParameter(TypeCriteria.subtypeOf("junit.framework.Assert"), NameCriteria.startsWith(ASSERT_NAME)),
    methodWithoutParameter(TypeCriteria.subtypeOf("junit.framework.Assert"), STARTS_WITH_FAIL),
    // fest 1.x
    methodWithoutParameter(TypeCriteria.subtypeOf("org.fest.assertions.GenericAssert"), ANY_NAME),
    methodWithoutParameter("org.fest.assertions.Fail", STARTS_WITH_FAIL),
    // fest 2.x
    methodWithoutParameter(TypeCriteria.subtypeOf("org.fest.assertions.api.AbstractAssert"), ANY_NAME),
    methodWithoutParameter("org.fest.assertions.api.Fail", STARTS_WITH_FAIL),
    // assertJ
    methodWithoutParameter(TypeCriteria.subtypeOf("org.assertj.core.api.AbstractAssert"), ANY_NAME),
    methodWithoutParameter("org.assertj.core.api.Fail", STARTS_WITH_FAIL),
    methodWithoutParameter("org.assertj.core.api.Fail", "shouldHaveThrown"),
    methodWithoutParameter("org.assertj.core.api.Assertions", STARTS_WITH_FAIL),
    methodWithoutParameter("org.assertj.core.api.Assertions", "shouldHaveThrown"),
    // hamcrest
    methodWithParameters("org.hamcrest.MatcherAssert", ASSERT_THAT_NAME).addParameter(ANY_TYPE).addParameter(ANY_TYPE),
    methodWithParameters("org.hamcrest.MatcherAssert", ASSERT_THAT_NAME).addParameter(ANY_TYPE).addParameter(ANY_TYPE).addParameter(ANY_TYPE),
    // Mockito
    methodWithoutParameter(ORG_MOCKITO_MOCKITO, "verifyNoMoreInteractions"),
    methodWithoutParameter(ORG_MOCKITO_MOCKITO, "verifyZeroInteractions"),
    // spring
    methodWithParameters("org.springframework.test.web.servlet.ResultActions", "andExpect").addParameter(ANY_TYPE),
    // EasyMock
    methodWithoutParameter("org.easymock.EasyMock", VERIFY),
    methodWithoutParameter(TypeCriteria.subtypeOf("org.easymock.IMocksControl"), VERIFY),
    methodWithoutParameter(TypeCriteria.subtypeOf("org.easymock.EasyMockSupport"), "verifyAll"),
    // Truth Framework
    methodWithoutParameter("com.google.common.truth.Truth", NameCriteria.startsWith(ASSERT_NAME))
  );

  private final Deque<Boolean> methodContainsAssertion = new ArrayDeque<>();
  private final Deque<Boolean> methodContainsAssertjSoftAssertionUsage = new ArrayDeque<>();
  private final Deque<Boolean> methodContainsJunitSoftAssertionUsage = new ArrayDeque<>();
  private final Deque<Boolean> methodContainsAssertjAssertAll = new ArrayDeque<>();
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
    if (ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.ABSTRACT)) {
      return;
    }
    boolean isUnitTest = isUnitTest(methodTree);
    inUnitTest.push(isUnitTest);
    methodContainsAssertion.push(false);
    methodContainsAssertjSoftAssertionUsage.push(false);
    methodContainsAssertjAssertAll.push(false);
    methodContainsJunitSoftAssertionUsage.push(false);
    super.visitMethod(methodTree);
    inUnitTest.pop();
    Boolean containsAssertion = methodContainsAssertion.pop();
    Boolean containsSoftAssertionDecl = methodContainsAssertjSoftAssertionUsage.pop();
    Boolean containsAssertjAssertAll = methodContainsAssertjAssertAll.pop();
    Boolean containsJunitSoftAssertionUsage = methodContainsJunitSoftAssertionUsage.pop();
    if (isUnitTest &&
        !expectAssertion(methodTree) &&
        (!containsAssertion || badSoftAssertionUsage(containsSoftAssertionDecl, containsAssertjAssertAll, containsJunitSoftAssertionUsage))) {
      context.reportIssue(this, methodTree.simpleName(), "Add at least one assertion to this test case.");
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

  private static boolean badSoftAssertionUsage(Boolean containsSoftAssertionDecl, Boolean containsAssertjAssertAll, Boolean containsJunitSoftAssertionUsage) {
    return containsSoftAssertionDecl && !containsJunitSoftAssertionUsage && !containsAssertjAssertAll;
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree mit) {
    if (!inUnitTest()) {
      return;
    }
    checkForAssertjSoftAssertions(mit);
    if (methodContainsAssertion.peek()) {
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

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    if (inUnitTest()) {
      Symbol symbol = tree.symbol();
      Type type = symbol.type();
      if (type != null && type.isSubtypeOf("org.assertj.core.api.AbstractStandardSoftAssertions")){
        setTrue(methodContainsAssertjSoftAssertionUsage);
        if (symbol.metadata().isAnnotatedWith("org.junit.Rule")) {
          setTrue(methodContainsJunitSoftAssertionUsage);
        }
      }
    }
    super.visitIdentifier(tree);
  }

  private boolean inUnitTest() {
    return !inUnitTest.isEmpty() && inUnitTest.peek();
  }

  private static void setTrue(Deque<Boolean> collection) {
    if (!collection.peek()) {
      collection.pop();
      collection.push(true);
    }
  }

  private void checkForAssertjSoftAssertions(MethodInvocationTree mit) {
    if (ASSERTJ_ASSERT_ALL.matches(mit)) {
      setTrue(methodContainsAssertjAssertAll);
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

  private static final MethodMatcher methodWithoutParameter(String typeDefinition, String name) {
    return methodWithoutParameter(TypeCriteria.is(typeDefinition), NameCriteria.is(name));
  }

  private static final MethodMatcher methodWithoutParameter(String typeDefinition, NameCriteria methodNameCriteria) {
    return methodWithoutParameter(TypeCriteria.is(typeDefinition), methodNameCriteria);
  }

  private static final MethodMatcher methodWithoutParameter(TypeCriteria typeDefinitionCriteria, String methodName) {
    return methodWithoutParameter(typeDefinitionCriteria, NameCriteria.is(methodName));
  }

  private static final MethodMatcher methodWithoutParameter(TypeCriteria typeDefinitionCriteria, NameCriteria nameCriteria) {
    return MethodMatcher.create().typeDefinition(typeDefinitionCriteria).name(nameCriteria).withNoParameterConstraint();
  }

  private static final MethodMatcher methodWithParameters(String typeDefinition, String methodName) {
    return methodWithParameters(TypeCriteria.is(typeDefinition), methodName);
  }

  private static final MethodMatcher methodWithParameters(TypeCriteria typeDefinitionCriteria, String methodName) {
    return MethodMatcher.create().typeDefinition(typeDefinitionCriteria).name(methodName);
  }

}
