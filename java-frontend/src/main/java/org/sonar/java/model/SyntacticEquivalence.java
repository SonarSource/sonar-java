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
package org.sonar.java.model;

import org.sonar.java.annotations.VisibleForTesting;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public final class SyntacticEquivalence {

  private SyntacticEquivalence() {
  }

  /**
   * @return true, if nodes are syntactically equivalent
   */
  public static boolean areEquivalent(List<? extends Tree> leftList, List<? extends Tree> rightList) {
    return areEquivalent(leftList, rightList, (t1, t2) -> false, true);
  }

  /**
   * @return true, if nodes are syntactically equivalent
   * Use "overwriteEquivalence" to force the equivalence or not of two nodes. When it returns true, the method will return "equivalenceValue".
   */
  public static boolean areEquivalent(List<? extends Tree> leftList,
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

  /**
   * @return true, if nodes are syntactically equivalent
   */
  public static boolean areEquivalent(@Nullable Tree leftNode, @Nullable Tree rightNode) {
    return areEquivalent(leftNode, rightNode, (t1, t2) -> false, true);
  }

  /**
   * @return true, if nodes are syntactically equivalent
   * Use "overwriteEquivalence" to force the equivalence or not of two nodes. When it returns true, the method will return "equivalenceValue".
   */
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
    if (leftNode instanceof SyntaxToken) {
      return Objects.equals(((SyntaxToken) leftNode).text(), ((SyntaxToken) rightNode).text());
    } else if (leftNode.is(Tree.Kind.INFERED_TYPE)) {
      return rightNode.is(Tree.Kind.INFERED_TYPE);
    } else {
      throw new IllegalArgumentException();
    }
  }

}
