/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import static org.sonar.plugins.java.api.tree.Tree.Kind.NULL_LITERAL;

@Rule(key = "S5785")
public class AssertTrueInsteadOfDedicatedAssertCheck extends AbstractMethodDetection {

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

  private enum Assertion {
    NULL("Null", "A null-check"),
    NOT_NULL("NotNull", "A null-check"),
    SAME("Same", "An object reference comparison"),
    NOT_SAME("NotSame", "An object reference comparison"),
    EQUALS("Equals", "An equals check"),
    NOT_EQUALS("NotEquals", "An equals check");

    public final String methodName;
    public final String actionDescription;

    Assertion(String namePostfix, String actionDescription) {
      methodName = "assert" + namePostfix;
      this.actionDescription = actionDescription;
    }
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create().ofTypes(ASSERTION_CLASSES).names(ASSERT_METHOD_NAMES).withAnyParameters().build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!hasSemantic()) {
      return;
    }

    Arguments arguments = mit.arguments();

    ExpressionTree argumentExpression;
    if (hasBooleanArgumentAtPosition(arguments, 0)) {
      argumentExpression = arguments.get(0);
    } else if (hasBooleanArgumentAtPosition(arguments, arguments.size() - 1)) {
      argumentExpression = arguments.get(arguments.size() - 1);
    } else {
      // We encountered a JUnit5 assert[True|False] method that accepts a BooleanSupplier - not supported.
      return;
    }

    Optional<Assertion> replacementAssertionOpt = getReplacementAssertion(argumentExpression);

    if (replacementAssertionOpt.isPresent()) {
      IdentifierTree problematicAssertionCallIdentifier = ExpressionUtils.methodName(mit);
      if (problematicAssertionCallIdentifier.name().equals("assertFalse")) {
        replacementAssertionOpt = complement(replacementAssertionOpt.get());

        if (!replacementAssertionOpt.isPresent()) {
          return;
        }
      }

      Assertion replacementAssertion = replacementAssertionOpt.get();

      List<JavaFileScannerContext.Location> secondaryLocation = Collections.singletonList(new JavaFileScannerContext.Location(
        String.format("%s is performed here, which is better expressed with %s.",
          replacementAssertion.actionDescription, replacementAssertion.methodName),
        argumentExpression));
      String message = String.format("Use %s instead.", replacementAssertion.methodName);

      reportIssue(problematicAssertionCallIdentifier, message, secondaryLocation, null);
    }
  }

  private static boolean hasBooleanArgumentAtPosition(Arguments arguments, int index) {
    return arguments.size() > index && arguments.get(index).symbolType().isPrimitive(Type.Primitives.BOOLEAN);
  }

  /**
   * Returns the assertX method that should be used instead of assertTrue, if applicable.
   *
   * @param argumentExpression the boolean expression passed to assertTrue
   * @return the assertion method to be used instead of assertTrue, or {@code null} if no better assertion method was determined
   */
  private static Optional<Assertion> getReplacementAssertion(@Nullable ExpressionTree argumentExpression) {
    if (argumentExpression == null) {
      return Optional.empty();
    }

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
        if (ExpressionUtils.methodName((MethodInvocationTree) argumentExpression).name().equals("equals")) {
          assertion = Assertion.EQUALS;
        }
        break;
      case LOGICAL_COMPLEMENT:
        return complement(getReplacementAssertion(((UnaryExpressionTree) argumentExpression).expression()).orElse(null));
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

  private static Optional<Assertion> complement(@Nullable Assertion assertion) {
    if (assertion == null) {
      return Optional.empty();
    }

    Assertion complement = null;
    switch (assertion) {
      case NULL:
        complement = Assertion.NOT_NULL;
        break;
      case NOT_NULL:
        complement = Assertion.NULL;
        break;
      case SAME:
        complement = Assertion.NOT_SAME;
        break;
      case NOT_SAME:
        complement = Assertion.SAME;
        break;
      case EQUALS:
        complement = Assertion.NOT_EQUALS;
        break;
      case NOT_EQUALS:
        complement = Assertion.EQUALS;
        break;
    }

    return Optional.of(complement);
  }
}
