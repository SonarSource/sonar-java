/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public abstract class AbstractCallToDeprecatedCodeChecker extends IssuableSubscriptionVisitor {

  private int nestedDeprecationLevel = 0;

  @Override
  public final void leaveFile(JavaFileScannerContext context) {
    nestedDeprecationLevel = 0;
  }

  @Override
  public final List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.IDENTIFIER, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public final void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (nestedDeprecationLevel == 0) {
      if (tree.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) tree;
        if (isSimpleNameOfVariableTreeOrVariableIsDeprecated(identifierTree)) {
          return;
        }
        tryGetDeprecatedSymbol(identifierTree).ifPresent(deprecatedSymbol -> checkDeprecatedIdentifier(identifierTree, deprecatedSymbol));
      } else if (tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
        MethodTree methodTree = (MethodTree) tree;
        tryGetDeprecatedSymbol(methodTree).ifPresent(deprecatedSymbol -> checkOverridingMethod(methodTree, deprecatedSymbol));
      }
    }
    if (isDeprecatedMethod(tree) || isDeprecatedClassTree(tree)) {
      nestedDeprecationLevel++;
    }
  }

  @Override
  public final void leaveNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (isDeprecatedMethod(tree) || isDeprecatedClassTree(tree)) {
      nestedDeprecationLevel--;
    }
  }

  private static Optional<Symbol> tryGetDeprecatedSymbol(IdentifierTree identifierTree) {
    Symbol symbol = identifierTree.symbol();
    if (symbol.isDeprecated()) {
      return Optional.of(symbol);
    }
    if (isConstructor(symbol) && symbol.owner().isDeprecated()) {
      return Optional.of(symbol.owner());
    }
    if (isDeprecatedEnumConstant(symbol)) {
      return Optional.of(symbol.type().symbol());
    }
    return Optional.empty();
  }

  public static boolean isConstructor(Symbol symbol) {
    return symbol.isMethodSymbol() && "<init>".equals(symbol.name());
  }

  private static boolean isDeprecatedEnumConstant(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.isEnum() && symbol.type().symbol().isDeprecated();
  }

  abstract void checkDeprecatedIdentifier(IdentifierTree identifierTree, Symbol deprecatedSymbol);

  private static boolean isSimpleNameOfVariableTreeOrVariableIsDeprecated(IdentifierTree identifierTree) {
    Tree parent = identifierTree.parent();
    return parent.is(Tree.Kind.VARIABLE) && (identifierTree.equals(((VariableTree) parent).simpleName()) || ((VariableTree) parent).symbol().isDeprecated());
  }

  private static Optional<Symbol.MethodSymbol> tryGetDeprecatedSymbol(MethodTree methodTree) {
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    if (methodSymbol.isDeprecated()) {
      return Optional.empty();
    }
    return tryGetOverridingDeprecatedConcreteMethod(methodSymbol);
  }

  abstract void checkOverridingMethod(MethodTree methodTree, Symbol.MethodSymbol deprecatedSymbol);

  private static Optional<Symbol.MethodSymbol> tryGetOverridingDeprecatedConcreteMethod(Symbol.MethodSymbol symbol) {
    Symbol.MethodSymbol overriddenMethod = symbol.overriddenSymbol();
    while(overriddenMethod != null && !overriddenMethod.isUnknown()) {
      if (overriddenMethod.isAbstract()) {
        return Optional.empty();
      }
      if (overriddenMethod.isDeprecated()) {
        return Optional.of(overriddenMethod);
      }
      overriddenMethod = overriddenMethod.overriddenSymbol();
    }
    return Optional.empty();
  }

  private static boolean isDeprecatedMethod(Tree tree) {
    return tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR) && ((MethodTree) tree).symbol().isDeprecated();
  }

  private static boolean isDeprecatedClassTree(Tree tree) {
    return tree.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE) && ((ClassTree) tree).symbol().isDeprecated();
  }

  boolean isFlaggedForRemoval(Symbol deprecatedSymbol) {
    List<AnnotationValue> valuesForAnnotation = deprecatedSymbol.metadata().valuesForAnnotation("java.lang.Deprecated");
    if (valuesForAnnotation == null) {
      return false;
    }
    return valuesForAnnotation.stream()
      .filter(annotationValue -> "forRemoval".equals(annotationValue.name()))
      .anyMatch(annotationValue -> Boolean.TRUE.equals(annotationValue.value()));
  }
}
