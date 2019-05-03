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

import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S1609")
public class SAMAnnotatedCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final MethodMatcherCollection OBJECT_METHODS = MethodMatcherCollection.create(
    methodMatcherWithName("equals", "java.lang.Object"),
    methodMatcherWithName("getClass"),
    methodMatcherWithName("hashcode"),
    methodMatcherWithName("notify"),
    methodMatcherWithName("notifyAll"),
    methodMatcherWithName("toString"),
    methodMatcherWithName("wait"),
    methodMatcherWithName("wait", "long"),
    methodMatcherWithName("wait", "long", "int"));

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    if (hasOneAbstractMethod(classTree.symbol()) && !isAnnotated(classTree)) {
      IdentifierTree simpleName = classTree.simpleName();
      reportIssue(
        simpleName,
        "Annotate the \"" + simpleName.name() + "\" interface with the @FunctionalInterface annotation" + context.getJavaVersion().java8CompatibilityMessage());
    }
  }

  private static boolean isAnnotated(ClassTree tree) {
    return tree.symbol().metadata().isAnnotatedWith("java.lang.FunctionalInterface");
  }

  private static boolean hasOneAbstractMethod(Symbol.TypeSymbol symbol) {
    return numberOfAbstractMethod(symbol) == 1 && noAbstractMethodInParentInterfaces(symbol.interfaces());
  }

  private static boolean noAbstractMethodInParentInterfaces(List<Type> interfaces) {
    return interfaces.stream()
      .map(Type::symbol)
      .noneMatch(symbol -> numberOfAbstractMethod(symbol) > 0 || !noAbstractMethodInParentInterfaces(symbol.interfaces()));
  }

  private static long numberOfAbstractMethod(Symbol symbol) {
    if (!symbol.isTypeSymbol()) {
      // unknown interface
      return Integer.MAX_VALUE;
    }

    Symbol.TypeSymbol interfaceSymbol = (Symbol.TypeSymbol) symbol;
    return interfaceSymbol.memberSymbols().stream()
      .filter(Symbol::isMethodSymbol)
      .filter(member -> {
        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) member;
        return isNotObjectMethod(methodSymbol) && methodSymbol.isAbstract();
      }).count();
  }


  private static boolean isNotObjectMethod(Symbol.MethodSymbol method) {
    MethodTree declaration = method.declaration();
    return declaration == null || !OBJECT_METHODS.anyMatch(declaration);
  }

  private static MethodMatcher methodMatcherWithName(String name, String... parameters) {
    return MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name(name).parameters(parameters);
  }
}
