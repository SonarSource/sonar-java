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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9402")
public class SortedCollectionWithNonComparableTypeCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Provide a comparator because this %s type does not implement \"Comparable\".";
  private static final String COMPARABLE = "java.lang.Comparable";
  private static final String COMPARATOR = "java.util.Comparator";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (context.getSemanticModel() == null) {
      return;
    }
    NewClassTree newClassTree = (NewClassTree) tree;
    Type collectionType = newClassTree.symbolType();
    if (newClassTree.methodSymbol().isUnknown() ||
      !isSupportedSortedCollection(collectionType) ||
      !usesNaturalOrdering(newClassTree) ||
      !collectionType.isParameterized() ||
      collectionType.typeArguments().isEmpty() ||
      isComparisonFailureHandled(newClassTree)) {
      return;
    }

    Type orderedType = contextualOrderedType(newClassTree, collectionType);
    if (!orderedType.isUnknown() &&
      !orderedType.symbol().isUnknown() &&
      !orderedType.isTypeVar() &&
      orderedType.isSubtypeOf("java.lang.Object") &&
      !hasCompatibleNaturalOrdering(orderedType) &&
      !JUtils.hasUnknownTypeInHierarchy(orderedType.symbol())) {
      reportIssue(newClassTree, MESSAGE.formatted(isMap(collectionType) ? "key" : "element"));
    }
  }

  private static boolean isSupportedSortedCollection(Type type) {
    return type.is("java.util.TreeSet") ||
      type.is("java.util.PriorityQueue") ||
      type.is("java.util.TreeMap") ||
      type.is("java.util.concurrent.ConcurrentSkipListSet") ||
      type.is("java.util.concurrent.ConcurrentSkipListMap");
  }

  private static boolean isMap(Type type) {
    return type.is("java.util.TreeMap") || type.is("java.util.concurrent.ConcurrentSkipListMap");
  }

  private static boolean usesNaturalOrdering(NewClassTree tree) {
    return tree.methodSymbol().parameterTypes().stream().noneMatch(SortedCollectionWithNonComparableTypeCheck::providesOrdering);
  }

  private static boolean providesOrdering(Type parameterType) {
    return parameterType.isSubtypeOf(COMPARATOR) ||
      parameterType.isSubtypeOf("java.util.SortedSet") ||
      parameterType.isSubtypeOf("java.util.SortedMap") ||
      parameterType.isSubtypeOf("java.util.PriorityQueue");
  }

  private static boolean hasCompatibleNaturalOrdering(Type orderedType) {
    return Stream.concat(Stream.of(orderedType), orderedType.symbol().superTypes().stream())
      .filter(superType -> superType.is(COMPARABLE))
      .anyMatch(comparableType -> comparableType.typeArguments().isEmpty() || orderedType.isSubtypeOf(comparableType.typeArguments().get(0)));
  }

  private static Type contextualOrderedType(NewClassTree tree, Type collectionType) {
    return targetOrderedType(tree, isMap(collectionType))
      .filter(type -> !type.isUnknown() && !type.symbol().isUnknown() && hasCompatibleNaturalOrdering(type))
      .orElseGet(() -> collectionType.typeArguments().get(0));
  }

  private static Optional<Type> targetOrderedType(NewClassTree tree, boolean map) {
    Tree expression = tree;
    Tree parent = tree.parent();
    while (parent instanceof ParenthesizedTree) {
      expression = parent;
      parent = parent.parent();
    }
    if (parent instanceof VariableTree variableTree && variableTree.initializer() == expression) {
      return declaredOrderedType(variableTree.type(), map);
    }
    return Optional.empty();
  }

  private static Optional<Type> declaredOrderedType(TypeTree typeTree, boolean map) {
    if (typeTree instanceof ParameterizedTypeTree parameterizedType && !parameterizedType.typeArguments().isEmpty()) {
      Type rawType = parameterizedType.type().symbolType();
      if (map ? isMapType(rawType) : isCollectionType(rawType)) {
        return Optional.of(parameterizedType.typeArguments().get(0).symbolType());
      }
    }
    return Optional.empty();
  }

  private static boolean isMapType(Type type) {
    return type.is("java.util.Map") || type.isSubtypeOf("java.util.Map");
  }

  private static boolean isCollectionType(Type type) {
    return type.is("java.util.Collection") || type.isSubtypeOf("java.util.Collection");
  }

  private static boolean isComparisonFailureHandled(NewClassTree tree) {
    if (tree.arguments().isEmpty() || tree.methodSymbol().parameterTypes().stream().noneMatch(SortedCollectionWithNonComparableTypeCheck::isCollectionOrMap)) {
      return false;
    }
    Tree child = tree;
    Tree ancestor = tree.parent();
    while (ancestor != null && !ancestor.is(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION) && !(ancestor instanceof ClassTree)) {
      if (ancestor.is(Tree.Kind.TRY_STATEMENT)) {
        TryStatementTree tryStatement = (TryStatementTree) ancestor;
        if (child == tryStatement.block() && tryStatement.catches().stream().anyMatch(SortedCollectionWithNonComparableTypeCheck::catchesClassCastException)) {
          return true;
        }
      }
      child = ancestor;
      ancestor = ancestor.parent();
    }
    return false;
  }

  private static boolean isCollectionOrMap(Type type) {
    return type.isSubtypeOf("java.util.Collection") || type.isSubtypeOf("java.util.Map");
  }

  private static boolean catchesClassCastException(CatchTree catchTree) {
    TypeTree caughtType = catchTree.parameter().type();
    if (caughtType.is(Tree.Kind.UNION_TYPE)) {
      return ((UnionTypeTree) caughtType).typeAlternatives().stream()
        .anyMatch(SortedCollectionWithNonComparableTypeCheck::isClassCastException);
    }
    return isClassCastException(caughtType);
  }

  private static boolean isClassCastException(TypeTree typeTree) {
    return typeTree.symbolType().is("java.lang.ClassCastException");
  }
}
