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
package org.sonar.java.model;

import java.util.Collections;
import java.util.Iterator;
import org.sonar.plugins.java.api.tree.Tree;

public final class JavaTreeFinalizer {

  private JavaTreeFinalizer() {
  }

  public static void setParents(Tree node) {
    Iterator<Tree> childrenIterator = iteratorFor(node);
    while (childrenIterator.hasNext()) {
      Tree child = childrenIterator.next();
      ((JavaTree) child).setParent(node);
      setParents(child);
    }
  }

  private static Iterator<Tree> iteratorFor(Tree node) {
    if (node.kind() == Tree.Kind.INFERED_TYPE || node.kind() == Tree.Kind.TOKEN) {
      return Collections.emptyIterator();
    }
    return ((JavaTree) node).getChildren().iterator();
  }

}
