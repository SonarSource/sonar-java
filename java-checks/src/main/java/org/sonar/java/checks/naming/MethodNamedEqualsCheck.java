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
package org.sonar.java.checks.naming;

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1201")
public class MethodNamedEqualsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    if (equalsWithSingleParam(methodTree) && !hasProperEquals(methodTree)) {
      reportIssue(methodTree.simpleName(), "Either override Object.equals(Object), or rename the method to prevent any confusion.");
    }
  }

  private static boolean equalsWithSingleParam(MethodTree methodTree) {
    return "equals".equalsIgnoreCase(methodTree.simpleName().name()) && methodTree.parameters().size() == 1;
  }

  private static boolean hasProperEquals(MethodTree methodTree) {
    Symbol.MethodSymbol symbol = methodTree.symbol();
    Symbol.TypeSymbol enclosingClass = symbol.enclosingClass();
    return enclosingClass != null && enclosingClass.memberSymbols().stream().anyMatch(MethodNamedEqualsCheck::isEqualsMethod);
  }

  private static boolean isEqualsMethod(Symbol symbol) {
    if (!symbol.isMethodSymbol()) {
      return false;
    }
    if (!"equals".equals(symbol.name())) {
      return false;
    }
    List<Type> parameters = ((Symbol.MethodSymbol) symbol).parameterTypes();
    return parameters.size() == 1 && parameters.get(0).is("java.lang.Object");
  }

}
