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
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4531")
public class Struts2EndpointCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher STRUTS2_METHOD = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name("execute").withoutParameter();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    Symbol owner = methodTree.symbol().owner();
    if (owner.type().isSubtypeOf("com.opensymphony.xwork2.ActionSupport") && STRUTS2_METHOD.matches(methodTree)) {
      List<JavaFileScannerContext.Location> settersLocation = getSetters(owner);
      if (!settersLocation.isEmpty()) {
        reportIssue(methodTree.simpleName(), "Make sure that executing this ActionSupport is safe.", settersLocation, null);
      }
    }
  }

  private static List<JavaFileScannerContext.Location> getSetters(Symbol owner) {
    if (!owner.isTypeSymbol()) {
      // defensive programing : owner of methodTree should always be a type symbol
      return Collections.emptyList();
    }
    return ((Symbol.TypeSymbol) owner).memberSymbols().stream()
      .filter(Symbol::isMethodSymbol)
      .filter(s -> s.name().startsWith("set"))
      .filter(s -> ((Symbol.MethodSymbol) s).parameterTypes().size() == 1)
      .map(Symbol::declaration)
      .map(mt -> new JavaFileScannerContext.Location("", mt))
      .collect(Collectors.toList());
  }
}
