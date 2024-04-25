/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
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
import org.sonarsource.analyzer.commons.quickfixes.QuickFix;

@Rule(key = "S3415")
public class AssertionArgumentOrderCheck extends AbstractMethodDetection {

  private static final String ASSERT_ARRAY_EQUALS = "assertArrayEquals";
  private static final String ASSERT_EQUALS = "assertEquals";
  private static final String ASSERT_ITERABLE_EQUALS = "assertIterableEquals";
  private static final String ASSERT_LINES_MATCH = "assertLinesMatch";
  private static final String ASSERT_NOT_EQUALS = "assertNotEquals";
  private static final String ASSERT_NOT_SAME = "assertNotSame";
  private static final String ASSERT_SAME = "assertSame";

  private static final String EXPECTED_VALUE_ACTUAL_VALUE = "expected value, actual value";
  private static final String ACTUAL_VALUE_EXPECTED_VALUE = "actual value, expected value";

  private static final String ORG_JUNIT_ASSERT = "org.junit.Assert";
  private static final String ORG_TESTNG_ASSERT = "org.testng.Assert";
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
        .names(ASSERT_EQUALS, ASSERT_SAME, ASSERT_NOT_SAME)
        .withAnyParameters()
        .build(),
      // TestNG
      MethodMatchers.create().ofTypes(ORG_TESTNG_ASSERT)
        .names(ASSERT_EQUALS, ASSERT_NOT_EQUALS, ASSERT_SAME, ASSERT_NOT_SAME)
        .withAnyParameters()
        .build(),
      // JUnit 5
      MethodMatchers.create().ofTypes(ORG_JUNIT5_ASSERTIONS)
        .names(ASSERT_ARRAY_EQUALS, ASSERT_EQUALS, ASSERT_ITERABLE_EQUALS, ASSERT_LINES_MATCH, ASSERT_NOT_EQUALS, ASSERT_NOT_SAME, ASSERT_SAME)
        .withAnyParameters()
        .build(),
      // AssertJ
      MethodMatchers.create().ofTypes("org.assertj.core.api.Assertions")
        .names("assertThat", "assertThatObject")
        .addParametersMatcher(MethodMatchers.ANY)
        .build()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Type ownerType = mit.methodSymbol().owner().type();
    if (ownerType.is(ORG_JUNIT5_ASSERTIONS)) {
      checkArguments(mit.arguments().get(0), mit.arguments().get(1), EXPECTED_VALUE_ACTUAL_VALUE);
    } else if (ownerType.is(ORG_JUNIT_ASSERT)) {
      ExpressionTree argToCheck = getActualArgument(mit);
      checkArguments(previousArg(argToCheck, mit), argToCheck, EXPECTED_VALUE_ACTUAL_VALUE);
    } else if (ownerType.is(ORG_TESTNG_ASSERT)) {
      checkArguments(mit.arguments().get(1), mit.arguments().get(0), ACTUAL_VALUE_EXPECTED_VALUE);
    } else {
      Optional<ExpressionTree> expectedValue = getExpectedValue(mit);
      ExpressionTree actualValue = mit.arguments().get(0);
      if (expectedValue.isPresent()) {
        checkArguments(expectedValue.get(), actualValue, ACTUAL_VALUE_EXPECTED_VALUE);
      } else {
        checkArgument(actualValue);
      }
    }
  }

  private void checkArguments(ExpressionTree expectedArgument, ExpressionTree actualArgument, String correctOrder) {
    if (actualArgument.is(LITERAL_KINDS)) {
      // When we have a literal as actual, we are sure to have an issue
      if (expectedArgument.is(LITERAL_KINDS)) {
        // no quick-fixes when both are literals... the fix is something else
        newIssue(expectedArgument, actualArgument, MESSAGE_TWO_LITERALS).report();
      } else {
        newIssue(expectedArgument, actualArgument, MESSAGE_SWAP, correctOrder)
          .withQuickFix(() -> swap(expectedArgument, actualArgument))
          .report();
      }
    } else if (isExpectedPattern(actualArgument) && !isExpectedPattern(expectedArgument)) {
      newIssue(expectedArgument, actualArgument, MESSAGE_SWAP, correctOrder)
        .withQuickFix(() -> swap(expectedArgument, actualArgument))
        .report();
    }
  }

  private QuickFix swap(Tree x, Tree y) {
    String newX = QuickFixHelper.contentForTree(y, context);
    String newY = QuickFixHelper.contentForTree(x, context);
    return QuickFix.newQuickFix("Swap arguments")
      .addTextEdit(AnalyzerMessage.replaceTree(x, newX))
      .addTextEdit(AnalyzerMessage.replaceTree(y, newY))
      .build();
  }

  private void checkArgument(ExpressionTree actualArgument) {
    if (actualArgument.is(LITERAL_KINDS)) {
      // no quick-fixes
      newIssue(actualArgument, MESSAGE_REPLACE).report();
    }
  }

  private InternalJavaIssueBuilder newIssue(ExpressionTree actualArgument, String message, Object... args) {
    return QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(actualArgument)
      .withMessage(message, args);
  }

  private InternalJavaIssueBuilder newIssue(ExpressionTree expectedArgument, ExpressionTree actualArgument, String message, Object... args) {
    return newIssue(actualArgument, message, args)
      .withSecondaries(new JavaFileScannerContext.Location("Other argument to swap.", expectedArgument));
  }

  /**
   * Find the related expected value from an assertThat, if the expression is "simple enough":
   * - exactly one subsequent method call
   * - one argument
   */
  private static Optional<ExpressionTree> getExpectedValue(MethodInvocationTree mit) {
    return MethodTreeUtils.consecutiveMethodInvocation(mit)
      .filter(secondInvocation -> {
        Tree parent = secondInvocation.parent();
        return parent != null && parent.is(Tree.Kind.EXPRESSION_STATEMENT) && secondInvocation.arguments().size() == 1;
      })
      .map(secondInvocation -> secondInvocation.arguments().get(0));
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
    if (arity > 2 && (arity == 4 || mit.methodSymbol().parameterTypes().stream().allMatch(AssertionArgumentOrderCheck::isDoubleOrFloat))) {
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
