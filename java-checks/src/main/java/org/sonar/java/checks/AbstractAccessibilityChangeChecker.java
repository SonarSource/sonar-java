/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.List;
import java.util.Optional;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

abstract class AbstractAccessibilityChangeChecker extends AbstractMethodDetection {
  protected static final String JAVA_LANG_REFLECT_FIELD = "java.lang.reflect.Field";
  protected static final MethodMatchers SET_ACCESSIBLE_MATCHER = MethodMatchers.create().ofSubTypes("java.lang.reflect.AccessibleObject")
    .names("setAccessible")
    .addParametersMatcher("boolean")
    .addParametersMatcher("java.lang.reflect.AccessibleObject[]", "boolean")
    .build();
  protected static final MethodMatchers SET_MATCHERS = MethodMatchers.or(
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("set").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setBoolean").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setByte").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setChar").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setDouble").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setFloat").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setInt").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setLong").withAnyParameters().build(),
    MethodMatchers.create().ofTypes(JAVA_LANG_REFLECT_FIELD).names("setShort").withAnyParameters().build()
  );

  protected static final MethodMatchers GET_CLASS_MATCHER = MethodMatchers.create()
    .ofAnyType()
    .names("getClass")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers FIELD_FETCHING_METHODS = MethodMatchers.or(
    MethodMatchers.create().ofTypes("java.lang.Class")
      .names("getField", "getDeclaredField")
      .addParametersMatcher("java.lang.String")
      .build(),
    MethodMatchers.create().ofTypes("java.lang.Class")
      .names("getFields", "getDeclaredFields")
      .addWithoutParametersMatcher()
      .build()
  );

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      SET_MATCHERS,
      SET_ACCESSIBLE_MATCHER
    );
  }

  protected static boolean setsToPubliclyAccessible(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    ExpressionTree arg = arguments.get(0);
    if (arguments.size() > 1) {
      arg = arguments.get(1);
    }
    return Boolean.TRUE.equals(ExpressionsHelper.getConstantValueAsBoolean(arg).value());
  }

  protected static boolean isModifyingFieldFromRecord(MethodInvocationTree mit) {
    ExpressionTree fieldModificationExpression = mit.methodSelect();
    if (!fieldModificationExpression.is(Tree.Kind.MEMBER_SELECT)) {
      return false;
    }
    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) fieldModificationExpression;
    ExpressionTree expression = memberSelect.expression();
    if (expression.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
      ArrayAccessExpressionTree arrayAccess = (ArrayAccessExpressionTree) expression;
      expression = arrayAccess.expression();
    }
    MethodInvocationTree fieldInitializer = null;
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      fieldInitializer = (MethodInvocationTree) expression;
    } else if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) expression;
      Optional<MethodInvocationTree> fieldGettingInvocation = getInitializingMethodInvocation(identifier);
      if (!fieldGettingInvocation.isPresent()) {
        return false;
      }
      fieldInitializer = fieldGettingInvocation.get();
    } else {
      return false;
    }
    if (!FIELD_FETCHING_METHODS.matches(fieldInitializer)) {
      return false;
    }
    ExpressionTree object = ((MemberSelectExpressionTree) fieldInitializer.methodSelect()).expression();
    List<Type> classTypeArguments = object.symbolType().typeArguments();
    if (classTypeArguments.isEmpty()) {
      return false;
    }
    return classTypeArguments.get(0).isSubtypeOf("java.lang.Record");
  }

  private static Optional<MethodInvocationTree> getInitializingMethodInvocation(IdentifierTree identifier) {
    Tree declaration = identifier.symbol().declaration();
    if (declaration == null || !declaration.is(Tree.Kind.VARIABLE)) {
      return Optional.empty();
    }
    VariableTree variable = (VariableTree) declaration;
    ExpressionTree initializer = variable.initializer();
    if (initializer == null || !initializer.is(Tree.Kind.METHOD_INVOCATION)) {
      return Optional.empty();
    }
    return Optional.of((MethodInvocationTree) initializer);
  }
}
