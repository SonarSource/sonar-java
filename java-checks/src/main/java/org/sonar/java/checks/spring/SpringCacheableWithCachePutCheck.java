/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.spring;

import org.sonar.check.Rule;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.stream.Collectors;


@Rule(key = "S7179")
public class SpringCacheableWithCachePutCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE_FORMAT = "Remove the \"@CachePut\" annotation or the \"@Cacheable\" annotation located on the same %s.";
  private static final String CLASS_MESSAGE = String.format(MESSAGE_FORMAT, "class");
  private static final String METHOD_MESSAGE = String.format(MESSAGE_FORMAT, "method");
  private static final String WRONG_CACHE_PUT_METHOD_MESSAGE = "Methods of a @Cacheable class should not be annotated with \"@CachePut\".";
  private static final String WRONG_CACHEABLE_METHOD_MESSAGE = "Methods of a @CachePut class should not be annotated with \"@Cacheable\".";
  private static final String CACHE_PUT_FQN = "org.springframework.cache.annotation.CachePut";
  private static final String CACHEABLE_FQN = "org.springframework.cache.annotation.Cacheable";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD, Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof MethodTreeImpl methodTree) {
      Symbol.MethodSymbol methodSymbol = methodTree.symbol();
      if (isSymbolAnnotatedWithCacheableAndCachePut(methodSymbol)) {
        reportIssue(methodTree.simpleName(), METHOD_MESSAGE, getSecondaryLocations(methodSymbol), null);
      } else if (isAnnotatedWithCachePut(methodSymbol) && isAnnotatedWithCacheable(methodSymbol.enclosingClass())) {
        var secondaryLocations = getSecondaryLocations(methodSymbol, methodSymbol.enclosingClass());
        reportIssue(methodTree.simpleName(), WRONG_CACHE_PUT_METHOD_MESSAGE, secondaryLocations, null);
      } else if (isAnnotatedWithCacheable(methodSymbol) && isAnnotatedWithCachePut(methodSymbol.enclosingClass())) {
        var secondaryLocations = getSecondaryLocations(methodSymbol, methodSymbol.enclosingClass());
        reportIssue(methodTree.simpleName(), WRONG_CACHEABLE_METHOD_MESSAGE, secondaryLocations, null);
      }
    } else {
      ClassTreeImpl classTree = (ClassTreeImpl) tree;
      Symbol.TypeSymbol classSymbol = classTree.symbol();
      if (isSymbolAnnotatedWithCacheableAndCachePut(classSymbol)) {
        // classTree.simpleName() can never be null, as annotations cannot target anonymous classes
        reportIssue(classTree.simpleName(), CLASS_MESSAGE, getSecondaryLocations(classSymbol), null);
      }
    }
  }

  private static boolean isAnnotatedWithCachePut(Symbol symbol) {
    return symbol.metadata().isAnnotatedWith(CACHE_PUT_FQN);
  }

  private static boolean isAnnotatedWithCacheable(Symbol symbol) {
    return symbol.metadata().isAnnotatedWith(CACHEABLE_FQN);
  }

  private static boolean isSymbolAnnotatedWithCacheableAndCachePut(Symbol symbol) {
    return isAnnotatedWithCachePut(symbol) && isAnnotatedWithCacheable(symbol);
  }

  private static List<JavaFileScannerContext.Location> getSecondaryLocations(Symbol symbol) {
    SymbolMetadata symbolMetadata = symbol.metadata();
    return symbolMetadata.annotations().stream()
      .filter(annotation ->
        annotation.symbol().type().is(CACHEABLE_FQN) || annotation.symbol().type().is(CACHE_PUT_FQN))
      .map(annotation -> new JavaFileScannerContext.Location(annotation.symbol().name(), symbolMetadata.findAnnotationTree(annotation)))
      .collect(Collectors.toList());
  }

  private static List<JavaFileScannerContext.Location> getSecondaryLocations(Symbol first, Symbol second) {
    var locations = getSecondaryLocations(first);
    locations.addAll(getSecondaryLocations(second));
    return locations;
  }

}
