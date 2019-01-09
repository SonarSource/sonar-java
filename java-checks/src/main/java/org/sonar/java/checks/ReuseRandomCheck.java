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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2119")
public class ReuseRandomCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(MethodMatcher.create().typeDefinition("java.util.Random").name("<init>").withoutParameter());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (assignedToLocalVariablesNotInConstructorOrStaticMain(newClassTree)) {
      reportIssue(newClassTree.identifier(), "Save and re-use this \"Random\".");
    }
  }

  private static boolean assignedToLocalVariablesNotInConstructorOrStaticMain(Tree tree) {
    Tree parent = tree.parent();
    if (parent.is(Kind.ASSIGNMENT)) {
      return isLocalVariableNotInConstructorOrStaticMain(((AssignmentExpressionTree) parent).variable()) &&
        assignedToLocalVariablesNotInConstructorOrStaticMain(parent);
    } else if (parent.is(Kind.VARIABLE)) {
      return isLocalVariableNotInConstructorOrStaticMain(((VariableTree) parent).simpleName());
    } else if (parent.is(Kind.PARENTHESIZED_EXPRESSION)) {
      return assignedToLocalVariablesNotInConstructorOrStaticMain(parent);
    } else {
      return parent.is(Kind.EXPRESSION_STATEMENT);
    }
  }

  private static boolean isLocalVariableNotInConstructorOrStaticMain(ExpressionTree expression) {
    if (expression.is(Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol().owner();
      return symbol.isMethodSymbol() &&
        !("<init>".equals(symbol.name()) || ("main".equals(symbol.name()) && symbol.isStatic()));
    }
    return false;
  }

}
