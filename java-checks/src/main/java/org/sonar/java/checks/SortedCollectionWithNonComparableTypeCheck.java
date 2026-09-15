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
import org.sonar.check.Rule;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9402")
public class SortedCollectionWithNonComparableTypeCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Provide a comparator because this element or key type does not implement \"Comparable\".";
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
    int orderedTypeIndex = orderedTypeIndex(collectionType);
    if (orderedTypeIndex < 0 || !usesNaturalOrdering(newClassTree) || !collectionType.isParameterized()) {
      return;
    }

    List<Type> typeArguments = collectionType.typeArguments();
    if (orderedTypeIndex >= typeArguments.size()) {
      return;
    }

    Type orderedType = typeArguments.get(orderedTypeIndex);
    if (!orderedType.isUnknown() &&
      !orderedType.isTypeVar() &&
      !orderedType.isSubtypeOf(COMPARABLE) &&
      !JUtils.hasUnknownTypeInHierarchy(orderedType.symbol())) {
      reportIssue(newClassTree, MESSAGE);
    }
  }

  private static int orderedTypeIndex(Type type) {
    if (type.is("java.util.TreeSet") ||
      type.is("java.util.PriorityQueue") ||
      type.is("java.util.concurrent.ConcurrentSkipListSet")) {
      return 0;
    }
    if (type.is("java.util.TreeMap") || type.is("java.util.concurrent.ConcurrentSkipListMap")) {
      return 0;
    }
    return -1;
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
}
