/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S2203")
public class CollectInsteadOfForeachCheck extends AbstractMethodDetection {

  private static final MethodMatcher FOREACH = MethodMatcher.create().typeDefinition("java.util.stream.Stream").name("forEach").withAnyParameters();

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(FOREACH);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArgument = mit.arguments().get(0);
    if (!firstArgument.is(Tree.Kind.METHOD_REFERENCE)) {
      return;
    }
    MethodReferenceTree mrt = (MethodReferenceTree) firstArgument;
    Tree expression = mrt.expression();
    if (isAddMethod(mrt.method()) && expression.is(Tree.Kind.IDENTIFIER)) {
      context.reportIssue(this, mrt, String.format("Use \"collect(Collectors.toList())\" instead of \"forEach(%s::add)\".", ((IdentifierTree) expression).name()));
    }
  }

  private static boolean isAddMethod(IdentifierTree methodRefIdentifier) {
    Symbol method = methodRefIdentifier.symbol();
    return method.isMethodSymbol()
      && method.owner().type().isSubtypeOf("java.util.List")
      && "add".equals(method.name());
  }

}
