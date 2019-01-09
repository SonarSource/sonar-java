/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4635")
public class StringOffsetMethodsCheck extends AbstractMethodDetection {

  private static final TypeCriteria JAVA_LANG_STRING = TypeCriteria.is("java.lang.String");
  private static final TypeCriteria INT = TypeCriteria.is("int");
  private static final MethodMatcher SUBSTRING = MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("substring").parameters(INT);

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("indexOf").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("indexOf").parameters(INT),
      MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("lastIndexOf").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("lastIndexOf").parameters(INT),
      MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("startsWith").parameters(JAVA_LANG_STRING)
      );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    // defensive programming : methodSelect can only be a MemberSelect (methods are instance method of java.lang.String).
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION) && SUBSTRING.matches(((MethodInvocationTree) expression).symbol())) {
        reportIssue(ExpressionUtils.methodName((MethodInvocationTree) expression), mit,
          String.format("Replace \"%s\" with the overload that accepts an offset parameter.", mit.symbol().name()));
      }
    }
  }
}
