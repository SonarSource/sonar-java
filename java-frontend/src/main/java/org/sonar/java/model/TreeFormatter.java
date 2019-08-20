/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class TreeFormatter {

  boolean showTokens = true;

  /**
   * Only for testing.
   */
  @Deprecated
  public static void compare(CompilationUnitTree newTree, CompilationUnitTree oldTree) {
    String actual = new TreeFormatter().toString(newTree);
    String expected = new TreeFormatter().toString(oldTree);
    if (!expected.equals(actual)) {
      try {
        throw (AssertionError) Class.forName("org.junit.ComparisonFailure")
          .getConstructor(String.class, String.class, String.class)
          .newInstance("", expected, actual);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  String toString(Tree node) {
    StringBuilder out = new StringBuilder();
    append(out, 0, node);
    return out.toString();
  }

  private void append(StringBuilder out, int indent, Tree node) {
    for (int i = 0; i < indent; i++) {
      out.append(' ');
    }
    out.append(node.kind());

    if (node.is(Tree.Kind.TRIVIA)) {
      SyntaxTrivia trivia = (SyntaxTrivia) node;
      out.append(' ').append(trivia.startLine()).append(':').append(trivia.column());
      out.append(' ').append(trivia.comment());

    } else if (node.is(Tree.Kind.TOKEN)) {
      SyntaxToken token = (SyntaxToken) node;
      out.append(' ').append(token.line()).append(':').append(token.column());
      out.append(' ').append(token.text());

    } else if (node.is(Tree.Kind.IDENTIFIER)) {
      out.append(" name=").append(((IdentifierTree) node).name());
    }

    out.append('\n');
    indent += 2;

    Iterator<? extends Tree> i = iteratorFor(node);
    while (i.hasNext()) {
      Tree child = i.next();
      if (child.is(Tree.Kind.TOKEN) && !showTokens) {
        continue;
      }
      append(out, indent, child);
    }
  }

  private static Iterator<? extends Tree> iteratorFor(Tree node) {
    if (node.kind() == Tree.Kind.TOKEN) {
      return ((SyntaxToken) node).trivias().iterator();
    }
    if (node.kind() == Tree.Kind.INFERED_TYPE || node.kind() == Tree.Kind.TRIVIA) {
      // getChildren throws exception in this case
      return Collections.emptyIterator();
    }
    final Iterator<Tree> iterator = ((JavaTree) node).getChildren().iterator();
    return Iterators.filter(
      iterator,
      child -> child != null
        && /* not empty list: */ !(child.is(Tree.Kind.LIST) && ((List) child).isEmpty())
    );
  }

}
