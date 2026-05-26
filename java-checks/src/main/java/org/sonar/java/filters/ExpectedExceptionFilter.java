/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.filters;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.java.checks.DateTimeConversionsCheck;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;

import static org.sonar.java.checks.helpers.MethodTreeUtils.consecutiveMethodInvocation;
import static org.sonar.java.checks.helpers.UnitTestUtils.isTryCatchFail;

/**
 * A filter to deactivate rules that catch expressions raising specific exceptions in contexts where these exceptions are expected.
 * <p>
 * This filter is willingly broad and works directly with type and method names rather than their types, to be resilient even in the face of missing semantic information.
 * </p>
 */
public class ExpectedExceptionFilter extends BaseTreeVisitorIssueFilter {

  private static final Set<String> DATE_TIME_EXCEPTION_TYPES = Set.of(
    "DateTimeException",
    "DateTimeParseException",
    "RuntimeException",
    "Exception",
    "Throwable"
  );

  private static final Set<String> ASSERT_THROWS_METHODS = Set.of(
    "assertThrows",
    "assertThrowsExactly",
    "expectThrows"
  );

  private static final Set<String> ASSERT_CODE_METHODS = Set.of(
    "assertThatCode",
    "assertThatThrownBy"
  );

  private static final Set<String> INSTANCEOF_METHODS = Set.of(
    "isInstanceOf",
    "isInstanceOfAny",
    "isExactlyInstanceOf",
    "isOfAnyClassIn"
  );

  private static final Set<String> ASSERT_EXCEPTION_METHODS = Set.of(
    "assertThatException",
    "assertThatRuntimeException",
    "thenException",
    "thenRuntimeException"
  );

  private static final Set<String> ASSERT_OF_TYPE_METHODS = Set.of(
    "assertThatExceptionOfType",
    "thenExceptionOfType"
  );

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return Set.of(DateTimeConversionsCheck.class);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (containsExpectedExceptions(tree.modifiers().annotations(), DATE_TIME_EXCEPTION_TYPES)) {
      excludeLines(tree, DateTimeConversionsCheck.class);
    }
    super.visitMethod(tree);
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    if (catchesExpectedException(tree, DATE_TIME_EXCEPTION_TYPES)) {
      excludeLines(tree.block(), DateTimeConversionsCheck.class);
    }
    super.visitTryStatement(tree);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    excludeExpectedExceptions(tree, DATE_TIME_EXCEPTION_TYPES, DateTimeConversionsCheck.class);
    super.visitMethodInvocation(tree);
  }

  /**
   * Filter a given rule for parts of a method invocation expression when it expects a given set of exception types.
   */
  private void excludeExpectedExceptions(MethodInvocationTree tree, Set<String> expectedExceptions, Class<? extends JavaCheck> filteredRule) {
    String methodName = tree.methodSymbol().name();
    Arguments arguments = tree.arguments();
    if (ASSERT_THROWS_METHODS.contains(methodName) && arguments.size() >= 2) {
      excludeFromAssertThrows(arguments, expectedExceptions, filteredRule);
    } else if ("catchThrowableOfType".equals(methodName) && arguments.size() > 1 && containsExpectedExceptions(arguments.get(1), expectedExceptions)) {
      excludeLines(arguments.get(0), filteredRule);
    } else if (ASSERT_CODE_METHODS.contains(methodName)) {
      excludeFromAssertCode(tree, arguments, expectedExceptions, filteredRule);
    } else if (ASSERT_EXCEPTION_METHODS.contains(methodName) ||
      (ASSERT_OF_TYPE_METHODS.contains(methodName) && !arguments.isEmpty() && containsExpectedExceptions(arguments.get(0), expectedExceptions))) {
      excludeIsThrownBy(tree, filteredRule);
    }
  }

  private void excludeFromAssertThrows(Arguments arguments, Set<String> expectedExceptions, Class<? extends JavaCheck> filteredRule) {
    int expectedTypeIndex = firstArgumentIsMessage(arguments) ? 1 : 0;
    int executableIndex = expectedTypeIndex + 1;
    if (arguments.size() > executableIndex && containsExpectedExceptions(arguments.get(expectedTypeIndex), expectedExceptions)) {
      excludeLines(arguments.get(executableIndex), filteredRule);
    }
  }

  private void excludeFromAssertCode(MethodInvocationTree tree, Arguments arguments, Set<String> expectedExceptions, Class<? extends JavaCheck> filteredRule) {
    subsequentMethodInvocation(tree, INSTANCEOF_METHODS).ifPresent(mit -> {
      if (!arguments.isEmpty() && mit.arguments().stream().anyMatch(expression -> containsExpectedExceptions(expression, expectedExceptions))) {
        excludeLines(arguments.get(0), filteredRule);
      }
    });
  }

  private void excludeIsThrownBy(MethodInvocationTree tree, Class<? extends JavaCheck> filteredRule) {
    subsequentMethodInvocation(tree, Set.of("isThrownBy")).ifPresent(mit -> {
      Arguments mitArguments = mit.arguments();
      if (!mitArguments.isEmpty()) {
        excludeLines(mitArguments.get(0), filteredRule);
      }
    });
  }

  /**
   * Check if a list of annotations contains a {@code Test} annotation expecting specific exception types.
   */
  private static boolean containsExpectedExceptions(List<AnnotationTree> annotations, Set<String> expectedExceptions) {
    return annotations.stream()
      .anyMatch(annotation ->
        "Test".equals(annotation.symbolType().name()) && containsExpectedExceptions(annotation.arguments(), expectedExceptions)
      );
  }

  /**
   * Check if a {@code Test} annotation has an {@code expected} or {@code expectedException} argument matching a set of expected exception types.
   */
  private static boolean containsExpectedExceptions(Arguments arguments, Set<String> expectedExceptions) {
    return arguments.stream()
      .filter(ExpectedExceptionFilter::isExpectedExceptionArgument)
      .anyMatch(argument -> containsExpectedExceptions(annotationValue(argument), expectedExceptions));
  }

  /**
   * Check that an annotation argument is an {@code expected} or {@code expectedException} attribute.
   */
  private static boolean isExpectedExceptionArgument(ExpressionTree expression) {
    String annotationAttributeName = ExpressionUtils.annotationAttributeName(expression);
    return "expected".equals(annotationAttributeName) || "expectedExceptions".equals(annotationAttributeName);
  }

  /**
   * Check if an expression contains a given exception type.
   */
  private static boolean containsExpectedExceptions(ExpressionTree expression, Set<String> expectedExceptions) {
    if (expression instanceof NewArrayTree newArray) {
      return newArray.initializers().stream()
        .anyMatch(initializer -> containsExpectedExceptions(initializer, expectedExceptions));
    }
    return classLiteralType(expression).map(type -> expectedExceptions.contains(type.name())).orElse(false);
  }

  /**
   * Get the value of an annotation attribute.
   */
  private static ExpressionTree annotationValue(ExpressionTree expression) {
    return expression.is(Tree.Kind.ASSIGNMENT) ? ((AssignmentExpressionTree) expression).expression() : expression;
  }

  /**
   * Extract the type name of a class literal expression.
   */
  private static Optional<Type> classLiteralType(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expression;
      if ("class".equals(memberSelect.identifier().name())) {
        return Optional.of(memberSelect.expression().symbolType());
      }
    }
    return Optional.empty();
  }

  /**
   * Check if a try statement catches a set of expected exception types.
   */
  private static boolean catchesExpectedException(TryStatementTree tryStatement, Set<String> expectedExceptions) {
    return tryStatement.catches().stream()
        .anyMatch(catchTree -> catchesExpectedExceptions(catchTree, expectedExceptions));
  }

  private static boolean catchesExpectedExceptions(CatchTree catchTree, Set<String> expectedExceptions) {
    return exceptionTypes(catchTree.parameter().type()).stream()
      .anyMatch(type -> expectedExceptions.contains(type.symbolType().name()));
  }

  private static List<TypeTree> exceptionTypes(TypeTree typeTree) {
    if (typeTree instanceof UnionTypeTree unionType) {
      return unionType.typeAlternatives();
    }
    return List.of(typeTree);
  }

  private static boolean firstArgumentIsMessage(Arguments arguments) {
    return arguments.size() >= 3 && arguments.get(0).symbolType().is("java.lang.String");
  }

  /**
   * Get the next chained method invocation whose identifier matches a set of expected method names.
   */
  private static Optional<MethodInvocationTree> subsequentMethodInvocation(MethodInvocationTree tree, Set<String> expectedMethodNames) {
    return consecutiveMethodInvocation(tree)
      .map(consecutiveMethod ->
        expectedMethodNames.contains(consecutiveMethod.methodSymbol().name()) ?
          consecutiveMethod : subsequentMethodInvocation(consecutiveMethod, expectedMethodNames).orElse(null));
  }

}
