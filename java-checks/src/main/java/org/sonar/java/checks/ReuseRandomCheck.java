/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.util.Optional;
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
    Optional<Symbol> variable = lookupInitializedSymbol(newClassTree);
    if (!variable.isPresent()) {
      variable = lookupAssignedSymbol(newClassTree);
    }
    variable
      .filter(Symbol::isVariableSymbol)
      .map(Symbol::owner)
      .filter(Symbol::isMethodSymbol)
      .filter(ReuseRandomCheck::isNotConstructorOrStaticMain)
      .ifPresent(owner -> reportIssue(newClassTree.identifier(), "Save and re-use this \"Random\"."));
  }

  private static Optional<Symbol> lookupInitializedSymbol(ExpressionTree expression) {
    return Optional.of(expression)
      .map(Tree::parent)
      .filter(tree -> tree.is(Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .map(VariableTree::simpleName)
      .map(IdentifierTree::symbol);
  }

  private static Optional<Symbol> lookupAssignedSymbol(ExpressionTree expression) {
    return Optional.of(expression)
      .map(Tree::parent)
      .filter(tree -> tree.is(Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .map(AssignmentExpressionTree::variable)
      .filter(tree -> tree.is(Kind.IDENTIFIER))
      .map(IdentifierTree.class::cast)
      .map(IdentifierTree::symbol);
  }

  private static boolean isNotConstructorOrStaticMain(Symbol symbol) {
    return !("<init>".equals(symbol.name()) || ("main".equals(symbol.name()) && symbol.isStatic()));
  }

}
