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
package org.sonar.java.viewer;

import com.sonar.sslr.api.typed.ActionParser;
import javafx.scene.web.WebEngine;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.nio.charset.StandardCharsets;

public class TreeViewer {

  private static final ActionParser<Tree> PARSER = JavaParser.createParser(StandardCharsets.UTF_8);
  private final Viewer viewer;

  public TreeViewer(Viewer viewer) {
    this.viewer = viewer;
  }

  public void analyse(String source){
    String dot = new TreeToDot().treeToDot(PARSER.parse(source));
    WebEngine webEngine = viewer.webView.getEngine();
    webEngine.executeScript("loadSyntaxTree('" + dot + "')");
  }

  private static class TreeToDot {
    int index = 0;

    private String treeToDot(Tree tree) {
      return "graph AST {" + getNode(tree) + "}";
    }

    private String getNode(Tree tree) {
      String result = index + "[label = \"" + tree.kind() + "#" + (tree.firstToken() != null ? tree.firstToken().line() : "") + "\"] ";
      if(tree.is(Tree.Kind.TOKEN)) {
        result = index + "[label = \"" + ((SyntaxToken) tree).text()+"\"] ";
      }
      int currentNodeIndex = index;
      if(!((JavaTree) tree).isLeaf()) {
        for (Tree child : ((JavaTree) tree).getChildren()) {
          index++;
          int childIndex = index;
          result += getNode(child) + currentNodeIndex+"->"+childIndex+" ";
        }
      }
      return result;
    }
  }
}
