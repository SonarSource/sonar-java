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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2119")
public class ReuseRandomCheck extends AbstractMethodDetection {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.NEW_CLASS);
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.util.Random").constructor().addWithoutParametersMatcher().build();
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (!isInConstructorOrStaticMain(newClassTree) && isUsedOnlyLocally(newClassTree)) {
      reportIssue(newClassTree.identifier(), "Save and re-use this \"Random\".");
    }
  }

  private static boolean isInConstructorOrStaticMain(ExpressionTree tree) {
    MethodTree enclosingMethod = ExpressionUtils.getEnclosingMethod(tree);
    if (enclosingMethod != null) {
      Symbol.MethodSymbol symbol = enclosingMethod.symbol();
      String name = symbol.name();
      return MethodMatchers.CONSTRUCTOR.equals(name) || ("main".equals(name) && symbol.isStatic());
    }
    return false;
  }

  private static boolean isUsedOnlyLocally(Tree tree) {
    Tree parent = tree.parent();
    if (parent.is(Kind.ASSIGNMENT)) {
      return isLocalVariable(((AssignmentExpressionTree) parent).variable()) &&
        isUsedOnlyLocally(parent);
    } else if (parent.is(Kind.VARIABLE)) {
      return isLocalVariable(((VariableTree) parent).simpleName());
    } else if (parent.is(Kind.PARENTHESIZED_EXPRESSION)) {
      return isUsedOnlyLocally(parent);
    } else {
      return parent.is(Kind.EXPRESSION_STATEMENT, Kind.MEMBER_SELECT);
    }
  }

  private static boolean isLocalVariable(ExpressionTree expression) {
    if (expression.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) expression).symbol().isLocalVariable();
    }
    return false;
  }

}
