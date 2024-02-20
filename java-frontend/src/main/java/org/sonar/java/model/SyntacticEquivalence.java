/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.model;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public final class SyntacticEquivalence {

  private SyntacticEquivalence() {
  }

  /**
   * @return true, if nodes are syntactically equivalent
   */
  public static boolean areEquivalent(@Nullable Tree leftNode, @Nullable Tree rightNode) {
    return areEquivalent(leftNode, rightNode, (t1, t2) -> false, true);
  }

  /**
   * @return true, if nodes are syntactically equivalent
   */
  public static boolean areEquivalent(List<? extends Tree> leftList, List<? extends Tree> rightList) {
    return areEquivalent(leftList, rightList, (t1, t2) -> false);
  }

  /**
   * @return true, if nodes are syntactically equivalent
   * Use permissiveEquivalence to force the equivalence of two nodes
   */
  public static boolean areEquivalent(List<? extends Tree> leftList, List<? extends Tree> rightList, BiPredicate<JavaTree, JavaTree> permissiveEquivalence) {
    return areEquivalent(leftList, rightList, permissiveEquivalence, true);
  }

  /**
   * Syntactic equivalence with additional semantic equivalence for methods calls.
   * Two methods calls are equivalent only if they have the same signature; if the types of the arguments are the same.
   *
   * @return true, if nodes are syntactically and semantically equivalent.
   */
  public static boolean areSemanticallyEquivalent(List<? extends Tree> leftList, List<? extends Tree> rightList) {
    return areEquivalent(leftList, rightList, SyntacticEquivalence::areNotSameMethodCalls, false);
  }

  /**
   * Syntactic equivalence with additional semantic equivalence for identifiers.
   * Two identifiers are equivalent only if they refer to the same known symbol.
   *
   * @return true, if nodes are syntactically equivalent and their variables refer to the same symbols.
   */
  public static boolean areEquivalentIncludingSameVariables(Tree left, Tree right) {
    return areEquivalent(left, right, SyntacticEquivalence::areDifferentVariables, false);
  }

  private static boolean areEquivalent(List<? extends Tree> leftList,
                                      List<? extends Tree> rightList,
                                      BiPredicate<JavaTree, JavaTree> overwriteEquivalence,
                                      boolean equivalenceValue) {
    if (leftList.size() != rightList.size()) {
      return false;
    }
    for (int i = 0; i < leftList.size(); i++) {
      Tree left = leftList.get(i);
      Tree right = rightList.get(i);
      if (!areEquivalent(left, right, overwriteEquivalence, equivalenceValue)) {
        return false;
      }
    }
    return true;
  }

  @VisibleForTesting
  static boolean areEquivalent(@Nullable Tree leftNode, @Nullable Tree rightNode, BiPredicate<JavaTree, JavaTree> overwriteEquivalence, boolean equivalenceValue) {
    return areEquivalent((JavaTree) leftNode, (JavaTree) rightNode, overwriteEquivalence, equivalenceValue);
  }

  private static boolean areEquivalent(@Nullable JavaTree leftNode, @Nullable JavaTree rightNode, BiPredicate<JavaTree, JavaTree> overWriteEquivalence, boolean equivalenceValue) {
    if (leftNode == rightNode) {
      return true;
    }
    if (leftNode == null || rightNode == null) {
      return false;
    }
    if (overWriteEquivalence.test(leftNode, rightNode)) {
      return equivalenceValue;
    }
    if (leftNode.kind() != rightNode.kind() || leftNode.is(Tree.Kind.OTHER)) {
      return false;
    } else if (leftNode.isLeaf()) {
      return areLeafsEquivalent(leftNode, rightNode);
    }
    Iterator<Tree> iteratorA = leftNode.getChildren().iterator();
    Iterator<Tree> iteratorB = rightNode.getChildren().iterator();

    while (iteratorA.hasNext() && iteratorB.hasNext()) {
      if (!areEquivalent(iteratorA.next(), iteratorB.next(), overWriteEquivalence, equivalenceValue)) {
        return false;
      }
    }

    return !iteratorA.hasNext() && !iteratorB.hasNext();
  }

  /**
   * Caller must guarantee that nodes of the same kind.
   */
  private static boolean areLeafsEquivalent(JavaTree leftNode, JavaTree rightNode) {
    if (leftNode instanceof SyntaxToken syntaxToken) {
      return Objects.equals(syntaxToken.text(), ((SyntaxToken) rightNode).text());
    } else if (leftNode.is(Tree.Kind.INFERED_TYPE)) {
      return rightNode.is(Tree.Kind.INFERED_TYPE);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private static boolean areDifferentVariables(JavaTree leftNode, JavaTree rightNode) {
    if (!leftNode.is(Tree.Kind.IDENTIFIER) || !rightNode.is(Tree.Kind.IDENTIFIER)) {
      return false;
    }

    Symbol leftSymbol = ((IdentifierTree) leftNode).symbol();
    Symbol rightSymbol = ((IdentifierTree) rightNode).symbol();

    if (leftSymbol.isUnknown() || rightSymbol.isUnknown()) return true;

    return !leftSymbol.equals(rightSymbol);
  }

  private static boolean areNotSameMethodCalls(JavaTree leftNode, JavaTree rightNode) {
    if (!leftNode.is(Tree.Kind.METHOD_INVOCATION) || !rightNode.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }

    Symbol.MethodSymbol leftSymbol = ((MethodInvocationTree) leftNode).methodSymbol();
    Symbol.MethodSymbol rightSymbol = ((MethodInvocationTree) rightNode).methodSymbol();

    if (leftSymbol.isUnknown() || rightSymbol.isUnknown()) {
      // This can happen when the symbol is unknown. If it is the case, we consider them as not the same to avoid FP.
      return true;
    }

    List<Type> leftArguments = leftSymbol.parameterTypes();
    List<Type> rightArguments = rightSymbol.parameterTypes();

    int leftArgumentsSize = leftArguments.size();
    if (leftArgumentsSize == rightArguments.size()) {
      for (int i = 0; i < leftArgumentsSize; i++) {
        if (!leftArguments.get(i).equals(rightArguments.get(i))) {
          return true;
        }
      }
    }
    return false;
  }

}
