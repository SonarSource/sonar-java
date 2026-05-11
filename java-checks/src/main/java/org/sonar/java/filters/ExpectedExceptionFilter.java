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
import org.sonar.java.checks.InstantConversionsCheck;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
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

import static org.sonar.java.checks.helpers.UnitTestUtils.isTryCatchFail;

public class ExpectedExceptionFilter extends BaseTreeVisitorIssueFilter {

  private static final String ASSERTJ_ASSERTIONS = "org.assertj.core.api.Assertions";

  private static final MethodMatchers ASSERT_THROWS_MATCHER = MethodMatchers.create()
    .ofTypes("org.junit.Assert", "org.junit.jupiter.api.Assertions", "org.testng.Assert", "org.testng.AssertJUnit")
    .names("assertThrows", "assertThrowsExactly", "expectThrows")
    .withAnyParameters()
    .build();

  private static final MethodMatchers ASSERTJ_CATCH_THROWABLE_OF_TYPE = MethodMatchers.create()
    .ofTypes(ASSERTJ_ASSERTIONS)
    .names("catchThrowableOfType")
    .addParametersMatcher("org.assertj.core.api.ThrowableAssert$ThrowingCallable", "java.lang.Class")
    .build();

  private static final MethodMatchers ASSERTJ_ASSERT_CODE = MethodMatchers.create()
    .ofTypes(ASSERTJ_ASSERTIONS)
    .names("assertThatCode", "assertThatThrownBy")
    .withAnyParameters()
    .build();

  private static final MethodMatchers ASSERTJ_EXCEPTION_OF_TYPE = MethodMatchers.create()
    .ofTypes(ASSERTJ_ASSERTIONS, "org.assertj.core.api.BDDAssertions")
    .names("assertThatExceptionOfType", "thenExceptionOfType")
    .addParametersMatcher("java.lang.Class")
    .build();

  private static final MethodMatchers ASSERTJ_TYPED_EXCEPTION = MethodMatchers.create()
    .ofTypes(ASSERTJ_ASSERTIONS, "org.assertj.core.api.BDDAssertions")
    .names("assertThatException", "assertThatRuntimeException", "thenException", "thenRuntimeException")
    .withAnyParameters()
    .build();

  private static final MethodMatchers ASSERTJ_IS_THROWN_BY = MethodMatchers.create()
    .ofTypes("org.assertj.core.api.ThrowableTypeAssert")
    .names("isThrownBy")
    .addParametersMatcher("org.assertj.core.api.ThrowableAssert$ThrowingCallable")
    .build();

  private static final MethodMatchers ASSERTJ_INSTANCE_OF_PREDICATES = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.Assert")
    .names("isInstanceOf", "isInstanceOfAny")
    .withAnyParameters()
    .build();

  private static final MethodMatchers ASSERTJ_EXACT_INSTANCE_OF_PREDICATES = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.Assert")
    .names("isExactlyInstanceOf", "isOfAnyClassIn")
    .withAnyParameters()
    .build();

  private static final String DATE_TIME_EXCEPTION = "java.time.DateTimeException";

  private static final Set<String> DATE_TIME_EXCEPTION_SUPERTYPES = Set.of(
    "java.lang.RuntimeException",
    "java.lang.Exception",
    "java.lang.Throwable"
  );

  @Override
  public Set<Class<? extends JavaCheck>> filteredRules() {
    return Set.of(InstantConversionsCheck.class);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (hasExpectedDateTimeExceptionAnnotation(tree.modifiers().annotations())) {
      excludeLines(tree, InstantConversionsCheck.class);
    }
    super.visitMethod(tree);
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    if (isTryCatchFailExpectingDateTimeException(tree)) {
      excludeLines(tree.block(), InstantConversionsCheck.class);
    }
    super.visitTryStatement(tree);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    excludeExpectedDateTimeExceptionIssues(tree);
    super.visitMethodInvocation(tree);
  }

  private void excludeExpectedDateTimeExceptionIssues(MethodInvocationTree mit) {
    if (ASSERT_THROWS_MATCHER.matches(mit)) {
      excludeAssertThrowsExecutableForDateTimeException(mit);
    } else if (ASSERTJ_CATCH_THROWABLE_OF_TYPE.matches(mit) && isDateTimeExceptionClass(mit.arguments().get(1), false)) {
      excludeLines(mit.arguments().get(0), InstantConversionsCheck.class);
    } else if (ASSERTJ_ASSERT_CODE.matches(mit)) {
      MethodTreeUtils.subsequentMethodInvocation(mit, ASSERTJ_INSTANCE_OF_PREDICATES).ifPresent(subsequentMit -> {
        if (hasDateTimeExceptionType(subsequentMit.arguments(), false)) {
          excludeLines(mit.arguments().get(0), InstantConversionsCheck.class);
        }
      });
      MethodTreeUtils.subsequentMethodInvocation(mit, ASSERTJ_EXACT_INSTANCE_OF_PREDICATES).ifPresent(subsequentMit -> {
        if (hasDateTimeExceptionType(subsequentMit.arguments(), true)) {
          excludeLines(mit.arguments().get(0), InstantConversionsCheck.class);
        }
      });
    } else if (ASSERTJ_TYPED_EXCEPTION.matches(mit) || (ASSERTJ_EXCEPTION_OF_TYPE.matches(mit) && isDateTimeExceptionClass(mit.arguments().get(0), false))) {
      MethodTreeUtils.subsequentMethodInvocation(mit, ASSERTJ_IS_THROWN_BY).ifPresent(subsequentMit ->
        excludeLines(subsequentMit.arguments().get(0), InstantConversionsCheck.class)
      );
    }
  }

  private void excludeAssertThrowsExecutableForDateTimeException(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    if (arguments.size() < 2) {
      return;
    }
    int expectedTypeIndex = firstArgumentIsMessage(arguments) ? 1 : 0;
    int executableIndex = expectedTypeIndex + 1;
    if (arguments.size() > executableIndex && isDateTimeExceptionClass(arguments.get(expectedTypeIndex), "assertThrowsExactly".equals(mit.methodSymbol().name()))) {
      excludeLines(arguments.get(executableIndex), InstantConversionsCheck.class);
    }
  }

  private static boolean firstArgumentIsMessage(Arguments arguments) {
    return arguments.size() >= 3 && arguments.get(0).symbolType().is("java.lang.String");
  }

  private static boolean isTryCatchFailExpectingDateTimeException(TryStatementTree tryStatement) {
    return isTryCatchFail(tryStatement.block()) && tryStatement.catches().stream().anyMatch(ExpectedExceptionFilter::isDateTimeExceptionCatch);
  }

  private static boolean isDateTimeException(Type type, boolean exact) {
    return type.isSubtypeOf(DATE_TIME_EXCEPTION) || (!exact && DATE_TIME_EXCEPTION_SUPERTYPES.stream().anyMatch(type::is));
  }

  private static boolean isDateTimeExceptionClass(ExpressionTree expression, boolean exact) {
    if (expression.is(Tree.Kind.NEW_ARRAY)) {
      return ((NewArrayTree) expression).initializers().stream()
        .anyMatch(initializer -> isDateTimeExceptionClass(initializer, exact));
    }
    return classLiteralType(expression)
      .map(type -> isDateTimeException(type, exact))
      .orElse(false);
  }

  private static Optional<Type> classLiteralType(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expression;
      if ("class".equals(memberSelect.identifier().name())) {
        return Optional.of(memberSelect.expression().symbolType());
      }
    }
    return Optional.empty();
  }

  private static boolean isDateTimeExceptionCatch(CatchTree catchTree) {
    return exceptionTypes(catchTree.parameter().type()).stream()
      .anyMatch(type -> isDateTimeException(type.symbolType(), false));
  }

  private static List<TypeTree> exceptionTypes(TypeTree typeTree) {
    if (typeTree.is(Tree.Kind.UNION_TYPE)) {
      return ((UnionTypeTree) typeTree).typeAlternatives();
    }
    return List.of(typeTree);
  }

  private static boolean hasExpectedDateTimeExceptionAnnotation(List<AnnotationTree> annotations) {
    return annotations.stream().anyMatch(annotation -> {
      Type annotationType = annotation.annotationType().symbolType();
      if (annotationType.is("org.junit.Test")) {
        return hasExpectedDateTimeExceptionArgument(annotation, "expected");
      } else if (annotationType.is("org.testng.annotations.Test")) {
        return hasExpectedDateTimeExceptionArgument(annotation, "expectedExceptions");
      }
      return false;
    });
  }

  private static boolean hasExpectedDateTimeExceptionArgument(AnnotationTree annotation, String attributeName) {
    return annotation.arguments().stream()
      .filter(argument -> attributeName.equals(ExpressionUtils.annotationAttributeName(argument)))
      .anyMatch(argument -> isDateTimeExceptionClass(annotationValue(argument), false));
  }

  private static ExpressionTree annotationValue(ExpressionTree expression) {
    return expression.is(Tree.Kind.ASSIGNMENT) ? ((AssignmentExpressionTree) expression).expression() : expression;
  }

  private static boolean hasDateTimeExceptionType(List<ExpressionTree> expressions, boolean exact) {
    return expressions.stream().anyMatch(expression -> isDateTimeExceptionClass(expression, exact));
  }

}
