/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.tests;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;

@Rule(key = "S5785")
public class AssertTrueInsteadOfDedicatedAssertCheck extends AbstractMethodDetection {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";

  private static final MethodMatchers EQUALS_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofAnyType().names("equals").addParametersMatcher(JAVA_LANG_OBJECT).build(),
    MethodMatchers.create().ofTypes("java.util.Objects").names("equals").addParametersMatcher(JAVA_LANG_OBJECT, JAVA_LANG_OBJECT)
      .build());

  private static final String[] ASSERT_METHOD_NAMES = {"assertTrue", "assertFalse"};
  private static final String[] ASSERTION_CLASSES = {
    // JUnit4
    "org.junit.Assert",
    "junit.framework.TestCase",
    // JUnit4 (deprecated)
    "junit.framework.Assert",
    // JUnit5
    "org.junit.jupiter.api.Assertions"
  };

  private static final Map<Assertion, Assertion> COMPLEMENTS = MapBuilder.<Assertion, Assertion>newMap()
    .put(Assertion.NULL, Assertion.NOT_NULL)
    .put(Assertion.NOT_NULL, Assertion.NULL)
    .put(Assertion.SAME, Assertion.NOT_SAME)
    .put(Assertion.NOT_SAME, Assertion.SAME)
    .put(Assertion.EQUALS, Assertion.NOT_EQUALS)
    .put(Assertion.NOT_EQUALS, Assertion.EQUALS)
    .build();

  private enum Assertion {
    NULL("Null", "A null-check"),
    NOT_NULL("NotNull", "A null-check"),
    SAME("Same", "An object reference comparison"),
    NOT_SAME("NotSame", "An object reference comparison"),
    EQUALS("Equals", "An equals check"),
    NOT_EQUALS("NotEquals", "An equals check");

    public final String methodName;
    public final String useInsteadMessage;
    public final String secondaryExplanationMessage;

    Assertion(String namePostfix, String actionDescription) {
      this.methodName = "assert" + namePostfix;
      this.useInsteadMessage = String.format("Use %s instead.", methodName);
      this.secondaryExplanationMessage =
        String.format("%s is performed here, which is better expressed with %s.", actionDescription, methodName);
    }
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create().ofTypes(ASSERTION_CLASSES).names(ASSERT_METHOD_NAMES).withAnyParameters().build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    mit.arguments().stream()
      .filter(argument -> argument.symbolType().isPrimitive(Type.Primitives.BOOLEAN))
      .findFirst()
      .ifPresent(argument -> checkBooleanExpressionInAssertMethod(ExpressionUtils.methodName(mit), argument));
  }

  private void checkBooleanExpressionInAssertMethod(IdentifierTree problematicAssertionCallIdentifier, ExpressionTree argumentExpression) {
    Optional<Assertion> replacementAssertionOpt = getReplacementAssertion(argumentExpression);
    if (problematicAssertionCallIdentifier.name().equals("assertFalse")) {
      replacementAssertionOpt = replacementAssertionOpt.map(COMPLEMENTS::get);
    }

    replacementAssertionOpt.ifPresent(replacementAssertion -> reportIssue(
      problematicAssertionCallIdentifier,
      replacementAssertion.useInsteadMessage,
      Collections.singletonList(new JavaFileScannerContext.Location(replacementAssertion.secondaryExplanationMessage, argumentExpression)),
      null));
  }

  /**
   * Returns the assertX method that should be used instead of assertTrue, if applicable.
   *
   * @param argumentExpression the boolean expression passed to assertTrue
   * @return the assertion method to be used instead of assertTrue, or {@code null} if no better assertion method was determined
   */
  private static Optional<Assertion> getReplacementAssertion(ExpressionTree argumentExpression) {
    Assertion assertion = null;

    switch (argumentExpression.kind()) {
      case EQUAL_TO:
        if (isCheckForNull((BinaryExpressionTree) argumentExpression)) {
          assertion = Assertion.NULL;
        } else if (isPrimitiveComparison((BinaryExpressionTree) argumentExpression)) {
          assertion = Assertion.EQUALS;
        } else {
          assertion = Assertion.SAME;
        }
        break;
      case NOT_EQUAL_TO:
        if (isCheckForNull((BinaryExpressionTree) argumentExpression)) {
          assertion = Assertion.NOT_NULL;
        } else if (isPrimitiveComparison((BinaryExpressionTree) argumentExpression)) {
          assertion = Assertion.NOT_EQUALS;
        } else {
          assertion = Assertion.NOT_SAME;
        }
        break;
      case METHOD_INVOCATION:
        if (EQUALS_METHODS.matches((MethodInvocationTree) argumentExpression)) {
          assertion = Assertion.EQUALS;
        }
        break;
      case LOGICAL_COMPLEMENT:
        return getReplacementAssertion(((UnaryExpressionTree) argumentExpression).expression()).map(COMPLEMENTS::get);
      default:
    }

    return Optional.ofNullable(assertion);
  }

  private static boolean isCheckForNull(BinaryExpressionTree bet) {
    return bet.leftOperand().is(NULL_LITERAL) || bet.rightOperand().is(NULL_LITERAL);
  }

  private static boolean isPrimitiveComparison(BinaryExpressionTree bet) {
    return bet.leftOperand().symbolType().isPrimitive() || bet.rightOperand().symbolType().isPrimitive();
  }
}
