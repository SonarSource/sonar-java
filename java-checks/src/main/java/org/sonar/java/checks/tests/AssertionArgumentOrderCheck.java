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
package org.sonar.java.checks.tests;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3415")
public class AssertionArgumentOrderCheck extends AbstractMethodDetection {

  private static final String ORG_JUNIT_ASSERT = "org.junit.Assert";
  private static final String ORG_JUNIT5_ASSERTIONS = "org.junit.jupiter.api.Assertions";
  private static final Tree.Kind[] LITERAL_KINDS = {Tree.Kind.STRING_LITERAL, Tree.Kind.INT_LITERAL, Tree.Kind.LONG_LITERAL, Tree.Kind.CHAR_LITERAL,
    Tree.Kind.NULL_LITERAL, Tree.Kind.BOOLEAN_LITERAL, Tree.Kind.DOUBLE_LITERAL, Tree.Kind.FLOAT_LITERAL};
  private static final String MESSAGE_TWO_LITERALS = "Change this assertion to not compare two literals.";
  private static final String MESSAGE_SWAP = "Swap these 2 arguments so they are in the correct order: %s.";
  private static final String MESSAGE_REPLACE = "Replace this literal with the actual expression you want to assert.";

  private static final MethodMatchers COLLECTION_CREATION_CALL = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.util.Collections")
      .name(name -> name.startsWith("singleton") || name.startsWith("empty"))
      .withAnyParameters()
      .build(),
    MethodMatchers.create().ofTypes("java.util.Arrays").names("asList").withAnyParameters().build());

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofTypes(ORG_JUNIT_ASSERT)
        .names("assertEquals", "assertSame", "assertNotSame")
        .withAnyParameters()
        .build(),
      // JUnit 5
      MethodMatchers.create().ofTypes(ORG_JUNIT5_ASSERTIONS)
        .names("assertArrayEquals", "assertEquals", "assertIterableEquals", "assertLinesMatch", "assertNotEquals", "assertNotSame", "assertSame")
        .withAnyParameters()
        .build(),
      // AssertJ
      MethodMatchers.create().ofTypes("org.assertj.core.api.Assertions")
        .names("assertThat")
        .addParametersMatcher(MethodMatchers.ANY)
        .build()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.symbol().owner().type().is(ORG_JUNIT5_ASSERTIONS)) {
      checkArguments(mit.arguments().get(0), mit.arguments().get(1), "expected value, actual value");
    } else if (mit.symbol().owner().type().is(ORG_JUNIT_ASSERT)) {
      ExpressionTree argToCheck = getActualArgument(mit);
      checkArguments(previousArg(argToCheck, mit), argToCheck, "expected value, actual value");
    } else {
      Optional<ExpressionTree> expectedValue = getExpectedValue(mit);
      ExpressionTree actualValue = mit.arguments().get(0);
      if (expectedValue.isPresent()) {
        checkArguments(expectedValue.get(), actualValue, "actual value, expected value");
      } else {
        checkArgument(actualValue);
      }
    }
  }

  private void checkArguments(ExpressionTree expectedArgument, ExpressionTree actualArgument, String correctOrder) {
    if (actualArgument.is(LITERAL_KINDS)) {
      // When we have a literal as actual, we are sure to have an issue
      if (expectedArgument.is(LITERAL_KINDS)) {
        reportIssue(expectedArgument, actualArgument, MESSAGE_TWO_LITERALS);
      } else {
        reportIssue(expectedArgument, actualArgument, String.format(MESSAGE_SWAP, correctOrder));
      }
    } else if (isExpectedPattern(actualArgument) && !isExpectedPattern(expectedArgument)) {
      reportIssue(expectedArgument, actualArgument, String.format(MESSAGE_SWAP, correctOrder));
    }
  }

  private void checkArgument(ExpressionTree actualArgument) {
    if (actualArgument.is(LITERAL_KINDS)) {
      reportIssue(actualArgument, MESSAGE_REPLACE);
    }
  }

  /**
   * Find the related expected value from an assertThat, if the expression is "simple enough":
   * - exactly one subsequent method call
   * - one argument
   */
  private static Optional<ExpressionTree> getExpectedValue(MethodInvocationTree mit) {
    Tree parentOfParent = mit.parent();
    parentOfParent = parentOfParent == null ? null : parentOfParent.parent();
    if (parentOfParent == null || !parentOfParent.is(Tree.Kind.METHOD_INVOCATION)) {
      return Optional.empty();
    }
    MethodInvocationTree secondInvocation = (MethodInvocationTree) parentOfParent;
    Tree secondInvocationParent = secondInvocation.parent();

    if (secondInvocationParent != null && secondInvocationParent.is(Tree.Kind.EXPRESSION_STATEMENT) && secondInvocation.arguments().size() == 1) {
      return Optional.of(secondInvocation.arguments().get(0));
    }
    return Optional.empty();
  }

  private void reportIssue(ExpressionTree expectedArgument, ExpressionTree actualArgument, String message) {
    List<JavaFileScannerContext.Location> secondaries = Collections.singletonList(new JavaFileScannerContext.Location("", expectedArgument));
    context.reportIssue(this, actualArgument, message, secondaries, null);
  }

  private static boolean isNewArrayWithConstants(ExpressionTree actualArgument) {
    if (actualArgument.is(Tree.Kind.NEW_ARRAY)) {
      NewArrayTree newArrayTree = (NewArrayTree) actualArgument;
      return newArrayTree.initializers().stream().allMatch(AssertionArgumentOrderCheck::isConstant);
    }
    return false;
  }

  private static boolean isCollectionCreationWithConstants(ExpressionTree actualArgument) {
    if (actualArgument.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) actualArgument;
      return COLLECTION_CREATION_CALL.matches(mit) && mit.arguments().stream().allMatch(AssertionArgumentOrderCheck::isConstant);
    }
    return false;
  }

  private static ExpressionTree previousArg(ExpressionTree argToCheck, MethodInvocationTree mit) {
    return mit.arguments().get(mit.arguments().indexOf(argToCheck) - 1);
  }

  private static ExpressionTree getActualArgument(MethodInvocationTree mit) {
    int arity = mit.arguments().size();
    ExpressionTree arg = mit.arguments().get(arity - 1);
    // Check for assert equals method with delta
    if (arity > 2 && (arity == 4 || ((Symbol.MethodSymbol) mit.symbol()).parameterTypes().stream().allMatch(AssertionArgumentOrderCheck::isDoubleOrFloat))) {
      // last arg is actually delta, take the previous last to get the actual arg.
      arg = mit.arguments().get(arity - 2);
    }
    return arg;
  }

  private static boolean isDoubleOrFloat(Type type) {
    return type.isPrimitive(Type.Primitives.DOUBLE) || type.isPrimitive(Type.Primitives.FLOAT);
  }

  private static boolean isExpectedPattern(ExpressionTree actualArgument) {
    return isConstant(actualArgument) || isNewArrayWithConstants(actualArgument) || isCollectionCreationWithConstants(actualArgument);
  }

  private static boolean isConstant(Tree argToCheck) {
    return argToCheck.is(LITERAL_KINDS)
      || (argToCheck.is(Tree.Kind.IDENTIFIER) && isStaticFinal(((IdentifierTree) argToCheck).symbol()))
      || (argToCheck.is(Tree.Kind.MEMBER_SELECT) && isStaticFinal(((MemberSelectExpressionTree) argToCheck).identifier().symbol()));
  }

  private static boolean isStaticFinal(Symbol symbol) {
    return symbol.isStatic() && symbol.isFinal();
  }
}
