/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Optional;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
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
  protected static final MethodMatchers METHOD_MATCHERS = MethodMatchers.or(
    SET_ACCESSIBLE_MATCHER,
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
    return METHOD_MATCHERS;
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
    ExpressionTree initializerExpression = fieldInitializer.methodSelect();
    Optional<IdentifierTree> classIdentifier = getClassIdentifier((MemberSelectExpressionTree) initializerExpression);
    if (!classIdentifier.isPresent()) {
      return false;
    }
    return classIdentifier.get().symbol().type().isSubtypeOf("java.lang.Record");
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

  private static Optional<IdentifierTree> getClassIdentifier(MemberSelectExpressionTree memberSelect) {
    ExpressionTree expression = memberSelect.expression();
    while (true) {
      if (expression.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree currentMemberSelect = (MemberSelectExpressionTree) expression;
        expression = currentMemberSelect.expression();
      } else if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree currentMethodInvocation = (MethodInvocationTree) expression;
        expression = currentMethodInvocation.methodSelect();
      } else {
        break;
      }
    }
    if (expression.symbolType().isUnknown() || !expression.is(Tree.Kind.IDENTIFIER)) {
      return Optional.empty();
    }
    return Optional.of((IdentifierTree) expression);
  }
}
