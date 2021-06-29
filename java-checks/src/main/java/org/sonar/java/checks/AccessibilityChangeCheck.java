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
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3011")
public class AccessibilityChangeCheck extends AbstractMethodDetection {

  private static final String JAVA_LANG_REFLECT_FIELD = "java.lang.reflect.Field";
  private static final MethodMatchers METHOD_MATCHERS = MethodMatchers.or(
    MethodMatchers.create().ofSubTypes("java.lang.reflect.AccessibleObject").names("setAccessible").withAnyParameters().build(),
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

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (isModifyingFieldFromRecord(mit)) {
      return;
    }
    if (mit.symbol().name().equals("setAccessible")) {
      checkAccessibilityUpdate(mit);
    } else {
      reportIssue(mit, "This accessibility bypass should be removed.");
    }
  }

  private static boolean isModifyingFieldFromRecord(MethodInvocationTree mit) {
    if (!mit.symbol().owner().type().is(JAVA_LANG_REFLECT_FIELD)) {
      return false;
    }
    ExpressionTree expressionTree = mit.methodSelect();
    if (!expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      return false;
    }
    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expressionTree;
    ExpressionTree expression = memberSelect.expression();
    if (!expression.is(Tree.Kind.IDENTIFIER)) {
      return false;
    }
    Optional<MethodInvocationTree> fieldGettingInvocation = getFieldInitialization((IdentifierTree) expression);
    if (!fieldGettingInvocation.isPresent()) {
      return false;
    }
    ExpressionTree expressionTree1 = fieldGettingInvocation.get().methodSelect();
    if (!expressionTree1.is(Tree.Kind.MEMBER_SELECT)) {
      return false;
    }
    ExpressionTree classOfOrigin = unravel((MemberSelectExpressionTree) expressionTree1);

    return ((IdentifierTree) classOfOrigin).symbol().type().isSubtypeOf("java.lang.Record");
  }

  private static Optional<MethodInvocationTree> getFieldInitialization(IdentifierTree identifier) {
    Tree declaration = identifier.symbol().declaration();
    if (declaration == null || !declaration.is(Tree.Kind.VARIABLE)) {
      return Optional.empty();
    }
    VariableTree variable = (VariableTree) declaration;
    ExpressionTree initializer = variable.initializer();
    if (initializer == null || !initializer.is(Tree.Kind.METHOD_INVOCATION)) {
      return Optional.empty();
    }
    MethodInvocationTree fieldGettingInvocation = (MethodInvocationTree) initializer;
    if (!FIELD_FETCHING_METHODS.matches(fieldGettingInvocation)) {
      return Optional.empty();
    }
    return Optional.of(fieldGettingInvocation);
  }

  private static ExpressionTree unravel(MemberSelectExpressionTree memberSelect) {
    ExpressionTree expression = memberSelect.expression();
    while (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree currentMemberSelect = (MemberSelectExpressionTree) expression;
      expression = currentMemberSelect.expression();
    }
    return expression;
  }

  private void checkAccessibilityUpdate(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    ExpressionTree arg = arguments.get(0);
    if (arguments.size() > 1) {
      arg = arguments.get(1);
    }
    if (Boolean.TRUE.equals(ExpressionsHelper.getConstantValueAsBoolean(arg).value())) {
      reportIssue(mit, "This accessibility update should be removed.");
    }
  }
}
