/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
    webEngine.executeScript("loadDot('" + dot + "', " + (!SHOW_CACHE) + ")");
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
      result += index + "[label = \"" + node.programPoint + "\""
        + ",programState=\"" + node.programState + "\""
        + specialHighlight(node)
        + "] ";
      if (node.parent != null) {
        result += nodes.indexOf(node.parent) + "->" + index + "[label=\"" + node.learnedConstraints().stream().map(ExplodedGraph.Node.LearnedConstraint::toString)
          .collect(Collectors.joining(",")) + "\"] ";
      }
      if (SHOW_CACHE) {
        for (ExplodedGraph.Node cacheHit : node.cacheHits) {
          result += nodes.indexOf(cacheHit) + "->" + index + "[label=\"CACHE\", color=\"red\", fontcolor=\"red\"] ";
        }
      }
      index++;
    }
    return result + "}";

  }

  private static String specialHighlight(ExplodedGraph.Node node) {
    if (node.parent == null) {
      return ",color=\"green\",fontcolor=\"white\"";
    } else if (node.programPoint.toString().startsWith("B0.0")) {
      return ",color=\"black\",fontcolor=\"white\"";
    }
    return "";
  }
}
