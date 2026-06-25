/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8910")
public class MapperWithoutDaoFactoryCheck extends IssuableSubscriptionVisitor {

  private static final String MAPPER_ANNOTATION = "com.datastax.oss.quarkus.runtime.api.mapper.Mapper";
  private static final String DAO_FACTORY_ANNOTATION = "com.datastax.oss.quarkus.runtime.api.mapper.DaoFactory";
  private static final String MESSAGE = "Add at least one \"@DaoFactory\" method to this \"@Mapper\" interface.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    if (!hasAnnotation(classTree.modifiers().annotations(), MAPPER_ANNOTATION)) {
      return;
    }

    if (!hasDaoFactoryMethod(classTree.symbol(), new HashSet<>())) {
      reportIssue(classTree.simpleName(), MESSAGE);
    }
  }

  private static boolean hasDaoFactoryMethod(Symbol.TypeSymbol typeSymbol, Set<Symbol.TypeSymbol> visited) {
    if (!visited.add(typeSymbol)) {
      return false;
    }

    if (typeSymbol.memberSymbols().stream()
      .filter(Symbol::isMethodSymbol)
      .map(Symbol.MethodSymbol.class::cast)
      .anyMatch(MapperWithoutDaoFactoryCheck::hasDaoFactoryAnnotation)) {
      return true;
    }

    for (Type superType : typeSymbol.superTypes()) {
      Symbol.TypeSymbol superTypeSymbol = superType.symbol();
      if (superTypeSymbol.isUnknown()) {
        // If we encounter an unresolved supertype, assume it might provide the required @DaoFactory method
        // to avoid false positives in projects with incomplete classpaths
        return true;
      }
      if (hasDaoFactoryMethod(superTypeSymbol, visited)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasDaoFactoryAnnotation(Symbol.MethodSymbol methodSymbol) {
    MethodTree declaration = methodSymbol.declaration();
    return declaration != null
      ? hasAnnotation(declaration.modifiers().annotations(), DAO_FACTORY_ANNOTATION)
      : methodSymbol.metadata().isAnnotatedWith(DAO_FACTORY_ANNOTATION);
  }

  private static boolean hasAnnotation(List<AnnotationTree> annotations, String fullyQualifiedName) {
    return annotations.stream().anyMatch(annotation -> annotation.annotationType().symbolType().is(fullyQualifiedName));
  }

}
