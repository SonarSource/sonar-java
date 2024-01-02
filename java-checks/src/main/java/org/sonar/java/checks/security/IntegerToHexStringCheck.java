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
package org.sonar.java.checks.security;

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4425")
public class IntegerToHexStringCheck extends AbstractMethodDetection {

  private static final MethodMatchers APPEND_MATCHER = MethodMatchers.create()
    .ofSubTypes("java.lang.AbstractStringBuilder")
    .names("append")
    .addParametersMatcher("java.lang.String")
    .build();

  private static final MethodMatchers PRINT_MATCHER = MethodMatchers.create()
    .ofSubTypes("java.io.PrintStream")
    .names("print")
    .addParametersMatcher("java.lang.String")
    .build();

  private static final MethodMatchers JOINER_MATCHER = MethodMatchers.create()
    .ofSubTypes("java.util.StringJoiner")
    .names("add")
    .addParametersMatcher("java.lang.CharSequence")
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.lang.Integer")
      .names("toHexString")
      .addParametersMatcher("int")
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree method) {
    if (isArgumentAppended(method) && typeIsByte(method.arguments().get(0))) {
      reportIssue(method.methodSelect(), "Use String.format( \"%02X\", ...) instead.");
    }
  }

  private static boolean isArgumentAppended(MethodInvocationTree method) {
    return Optional.of(method)
      .map(Tree::parent)
      .filter(tree -> tree.is(Tree.Kind.ARGUMENTS))
      .map(Tree::parent)
      .filter(tree -> tree.is(Tree.Kind.METHOD_INVOCATION))
      .map(MethodInvocationTree.class::cast)
      .filter(parentMethod -> APPEND_MATCHER.matches(parentMethod) ||
        PRINT_MATCHER.matches(parentMethod) ||
        JOINER_MATCHER.matches(parentMethod))
      .isPresent();
  }

  private static boolean typeIsByte(ExpressionTree expression) {
    return expression.symbolType().isSubtypeOf("byte") ||
      ExpressionUtils.isSecuringByte(expression);
  }
}
