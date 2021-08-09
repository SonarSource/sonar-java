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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.collections.MapBuilder;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5329")
public class CollectionConstructorReferenceCheck extends IssuableSubscriptionVisitor {

  private static final String INITIAL_CAPACITY = "initialCapacity";
  private static final String EXPECTED_MAX_SIZE = "expectedMaxSize";

  private static final Map<String, String> COLLECTION_CONSTRUCTOR_WITH_INT_ARGUMENT = MapBuilder.<String, String>newMap()
    .put("java.util.ArrayList", INITIAL_CAPACITY)
    .put("java.util.HashMap", INITIAL_CAPACITY)
    .put("java.util.HashSet", INITIAL_CAPACITY)
    .put("java.util.Hashtable", INITIAL_CAPACITY)
    .put("java.util.IdentityHashMap", EXPECTED_MAX_SIZE)
    .put("java.util.LinkedHashMap", INITIAL_CAPACITY)
    .put("java.util.LinkedHashSet", INITIAL_CAPACITY)
    .put("java.util.PriorityQueue", INITIAL_CAPACITY)
    .put("java.util.Vector", INITIAL_CAPACITY)
    .put("java.util.WeakHashMap", INITIAL_CAPACITY)
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_REFERENCE);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodReferenceTree methodReference = (MethodReferenceTree) tree;
    if (!isConstructorWithSingleIntArgument(methodReference) ||
      !"java.util.function.Function".equals(methodReference.symbolType().fullyQualifiedName())) {
      return;
    }
    methodOwnerType(methodReference).ifPresent(constructorType -> {
      String intArgumentName = COLLECTION_CONSTRUCTOR_WITH_INT_ARGUMENT.get(constructorType.fullyQualifiedName());
      if (intArgumentName != null) {
        reportIssue(methodReference, String.format(
          "Replace this method reference by a lambda to explicitly show the usage of %1$s(int %2$s) or %1$s().",
          constructorType.name(),
          intArgumentName));
      }
    });
  }

  private static boolean isConstructorWithSingleIntArgument(MethodReferenceTree methodReference) {
    Symbol symbol = methodReference.method().symbol();
    if (!symbol.isMethodSymbol() || !"<init>".equals(symbol.name())) {
      return false;
    }
    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
    List<Type> parameters = methodSymbol.parameterTypes();
    return parameters.size() == 1 && "int".equals(parameters.get(0).fullyQualifiedName());
  }

  private static Optional<Type> methodOwnerType(MethodReferenceTree methodReference) {
    return Optional.of(methodReference.expression())
      .filter(ExpressionTree.class::isInstance)
      .map(ExpressionTree.class::cast)
      .flatMap(ExpressionUtils::extractIdentifierSymbol)
      .filter(Symbol::isTypeSymbol)
      .map(Symbol::type);
  }

}
