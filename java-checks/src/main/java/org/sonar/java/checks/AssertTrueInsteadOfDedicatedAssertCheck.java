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
import javax.annotation.Nonnull;
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

  private enum Assertions {
    NULL("Null", "A null-check"),
    NOT_NULL("NotNull", "A null-check"),
    SAME("Same", "An object reference comparison"),
    NOT_SAME("NotSame", "An object reference comparison"),
    EQUALS("Equals", "An equals check"),
    NOT_EQUALS("NotEquals", "An equals check");

    public final String methodName;
    public final String actionDescription;

    Assertions(String namePostfix, String actionDescription) {
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

    Assertions replacementAssertion = getReplacementAssertion(argumentExpression);

    if (replacementAssertion != null) {
      IdentifierTree problematicAssertionCallIdentifier = ExpressionUtils.methodName(mit);
      if (problematicAssertionCallIdentifier.name().equals("assertFalse")) {
        replacementAssertion = complement(replacementAssertion);

        if (replacementAssertion == null) {
          return;
        }
      }

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
  @Nullable
  private static Assertions getReplacementAssertion(@Nullable ExpressionTree argumentExpression) {
    if (argumentExpression == null) {
      return null;
    }

    switch (argumentExpression.kind()) {
      case EQUAL_TO:
        if (isCheckForNull((BinaryExpressionTree) argumentExpression)) {
          return Assertions.NULL;
        } else if (isPrimitiveComparison((BinaryExpressionTree) argumentExpression)) {
          return Assertions.EQUALS;
        } else {
          return Assertions.SAME;
        }
      case NOT_EQUAL_TO:
        if (isCheckForNull((BinaryExpressionTree) argumentExpression)) {
          return Assertions.NOT_NULL;
        } else if (isPrimitiveComparison((BinaryExpressionTree) argumentExpression)) {
          return Assertions.NOT_EQUALS;
        } else {
          return Assertions.NOT_SAME;
        }
      case METHOD_INVOCATION:
        if (ExpressionUtils.methodName((MethodInvocationTree) argumentExpression).name().equals("equals")) {
          return Assertions.EQUALS;
        } else {
          return null;
        }
      case LOGICAL_COMPLEMENT:
        return complement(getReplacementAssertion(((UnaryExpressionTree) argumentExpression).expression()));
      default:
        return null;
    }
  }

  private static boolean isCheckForNull(@Nonnull BinaryExpressionTree bet) {
    return bet.leftOperand().is(NULL_LITERAL) || bet.rightOperand().is(NULL_LITERAL);
  }

  private static boolean isPrimitiveComparison(@Nonnull BinaryExpressionTree bet) {
    return bet.leftOperand().symbolType().isPrimitive() || bet.rightOperand().symbolType().isPrimitive();
  }

  private static Assertions complement(@Nullable Assertions assertion) {
    if (assertion == null) {
      return null;
    }

    switch (assertion) {
      case NULL:
        return Assertions.NOT_NULL;
      case NOT_NULL:
        return Assertions.NULL;
      case SAME:
        return Assertions.NOT_SAME;
      case NOT_SAME:
        return Assertions.SAME;
      case EQUALS:
        return Assertions.NOT_EQUALS;
      case NOT_EQUALS:
        return Assertions.EQUALS;
      default:
        return null;
    }
  }
}
