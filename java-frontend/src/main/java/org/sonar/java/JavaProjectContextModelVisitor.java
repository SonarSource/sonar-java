/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaProjectContextModelVisitor extends IssuableSubscriptionVisitor {
  private static final String[] SPRING_BEAN_ANNOTATIONS = {
    "org.springframework.stereotype.Component"
  };

  private final ProjectContextModel projectContextModel;

  public JavaProjectContextModelVisitor(ProjectContextModel projectContextModel) {
    this.projectContextModel = projectContextModel;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof ClassTreeImpl classTree) {
      visitClass(classTree);
    }
  }

  private void visitClass(ClassTreeImpl classTree) {
    if (classTree.modifiers().annotations().stream()
      .anyMatch(a -> a.symbolType().is("org.springframework.stereotype.Component"))) {
      projectContextModel.springComponents.add(classTree.symbol().type().fullyQualifiedName());
    }

    SymbolMetadata classSymbolMetadata = classTree.symbol().metadata();
    if (hasAnnotation(classSymbolMetadata, SPRING_BEAN_ANNOTATIONS)) {
      Symbol.TypeSymbol symbol = classTree.symbol();
      Set<String> types = getTypes(symbol);
      for (String type : types) {
        Set<String> impls = projectContextModel.availableImpls.computeIfAbsent(type, k -> new HashSet<>());
        impls.add(symbol.type().fullyQualifiedName());
      }
    }
  }

  private static boolean hasAnnotation(SymbolMetadata classSymbolMetadata, String... annotationName) {
    return Arrays.stream(annotationName).anyMatch(classSymbolMetadata::isAnnotatedWith);
  }

  private static Set<String> getTypes(Symbol.TypeSymbol symbol) {
    var result = new HashSet<String>();
    result.add(symbol.type().fullyQualifiedName());
    for (Type iface: symbol.interfaces()) {
      result.add(iface.fullyQualifiedName());
    }
    return result;
  }
}
