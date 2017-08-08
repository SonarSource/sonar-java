/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.ast;

import com.sonar.sslr.api.typed.ActionParser;

import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.JavaTree;
import org.sonar.java.viewer.DotDataProvider;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.json.JsonObject;

public class ASTViewer {

  private ASTViewer() {
  }

  private static final ActionParser<Tree> PARSER = JavaParser.createParser();


  public static String toDot(String source) {
    return new TreeToDot().treeToDot(PARSER.parse(source));
  }

  private static class TreeToDot {
    int index = 0;

    private String treeToDot(Tree tree) {
      return "graph AST {" + getNode(tree) + "}";
    }

    private String getNode(Tree tree) {
      StringBuilder builder = new StringBuilder();
      String label = tree.kind() + (tree.firstToken() != null ? (" L#" + tree.firstToken().line()) : "");
      builder.append(new ASTDotNode(index, label, tree.kind()).node());
      if(tree.is(Tree.Kind.TOKEN)) {
        // add an extra node for tokens
        builder.append(new ASTDotNode(index, ((SyntaxToken) tree).text()).node());
      }
      int currentNodeIndex = index;
      if(!((JavaTree) tree).isLeaf()) {
        for (Tree child : ((JavaTree) tree).getChildren()) {
          index++;
          int childIndex = index;
          builder.append(getNode(child) + new ASTDotEdge(currentNodeIndex, childIndex).edge());
        }
      }
      return builder.toString();
    }
  }

  private static class ASTDotNode extends DotDataProvider.Node {

    private final String label;
    private final Highlighting highlighting;

    public ASTDotNode(int id, String label) {
      super(id);
      this.label = label;
      this.highlighting = Highlighting.TOKEN_KIND;
    }

    public ASTDotNode(int id, String label, Tree.Kind kind) {
      super(id);
      this.label = label;
      this.highlighting = fromTreeKind(kind);
    }

    @CheckForNull
    private static Highlighting fromTreeKind(Tree.Kind kind) {
      switch (kind) {
        case COMPILATION_UNIT:
          return Highlighting.FIRST_NODE;
        case CLASS:
        case INTERFACE:
        case ANNOTATION_TYPE:
        case ENUM:
          return Highlighting.CLASS_KIND;
        case CONSTRUCTOR:
        case METHOD:
          return Highlighting.METHOD_KIND;
        case TOKEN:
          // token are explicitly selected
        default:
          return null;
      }
    }

    @Override
    public String label() {
      return label;
    }

    @Override
    public Highlighting highlighting() {
      return highlighting;
    }

    @Override
    public JsonObject details() {
      return null;
    }

  }

  private static class ASTDotEdge extends DotDataProvider.Edge {

    public ASTDotEdge(int from, int to) {
      super(from, to);
    }

    @Override
    public String label() {
      return null;
    }

    @Override
    public Highlighting highlighting() {
      return null;
    }

    @Override
    public JsonObject details() {
      return null;
    }

  }
}
