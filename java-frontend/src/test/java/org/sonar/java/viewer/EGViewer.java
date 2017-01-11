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

import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import javafx.scene.web.WebEngine;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.cfg.CFGDebug;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ExplodedGraphWalker;
import org.sonar.java.se.MethodBehavior;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EGViewer {

  private static final ActionParser<Tree> PARSER = JavaParser.createParser(StandardCharsets.UTF_8);
  private final Viewer viewer;
  private static final boolean SHOW_CACHE = true;

  EGViewer(Viewer viewer) {
    this.viewer = viewer;
  }

  public void analyse(String source){
    ExplodedGraph eg = buildEG(source);
    viewer.textArea.setText(CFGDebug.toString(CFGViewer.buildCFG(source)));
    String dot = egToDot(eg);
    WebEngine webEngine = viewer.webView.getEngine();
    webEngine.executeScript("loadEG('" + dot + "', " + (!SHOW_CACHE) + ")");
  }

  private static ExplodedGraph buildEG(String source) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(source);
    SemanticModel.createFor(cut, Lists.newArrayList());
    MethodTree firstMethod = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
    return getEg(firstMethod);
  }

  private static ExplodedGraph getEg(MethodTree methodTree) {
    ExplodedGraphWalker walker = new ExplodedGraphWalker();
    walker.visitMethod(methodTree, new MethodBehavior(methodTree.symbol()));
    return walker.getExplodedGraph();
  }

  private static String egToDot(ExplodedGraph eg) {
    String result = "graph ExplodedGraph { ";
    List<ExplodedGraph.Node> nodes = new ArrayList<>(eg.getNodes().keySet());
    int index = 0;
    for (ExplodedGraph.Node node : nodes) {
      result += graphNode(index, node);
      if (!node.getParents().isEmpty()) {
        ExplodedGraph.Node firstParent = node.parent();
        result += parentEdge(nodes.indexOf(firstParent), index, node, firstParent);

        int nbParents = node.getParents().size();
        if (SHOW_CACHE && nbParents > 1) {
          List<ExplodedGraph.Node> cacheHits = node.getParents().subList(1, nbParents);
          for (ExplodedGraph.Node cacheHit : cacheHits) {
            result += cacheEdge(nodes.indexOf(cacheHit), index, cacheHit);
          }
        }
      }
      index++;
    }
    return result + "}";

  }

  private static String graphNode(int index, ExplodedGraph.Node node) {
    return index + "[label = \"" + node.programPoint + "\",programState=\"" + node.programState + "\"" + specialHighlight(node) + "] ";
  }

  private static String specialHighlight(ExplodedGraph.Node node) {
    if (node.getParents().isEmpty()) {
      return ",color=\"green\",fontcolor=\"white\"";
    } else if (node.programPoint.toString().startsWith("B0.0")) {
      return ",color=\"black\",fontcolor=\"white\"";
    }
    return "";
  }

  private static String parentEdge(int from, int to, ExplodedGraph.Node node, ExplodedGraph.Node firstParent) {
    return from + "->" + to
      + "[label=\""
      + node.getLearnedSymbols().stream().map(ExplodedGraph.Node.LearnedValue::toString).collect(Collectors.joining(","))
      + " "
      + node.getLearnedConstraints().stream().map(ExplodedGraph.Node.LearnedConstraint::toString).collect(Collectors.joining(","))
      + "\"] ";
  }

  private static String cacheEdge(int from, int to, ExplodedGraph.Node cacheHit) {
    return from + "->" + to + "[label=\"CACHE\", color=\"red\", fontcolor=\"red\"] ";
  }
}
