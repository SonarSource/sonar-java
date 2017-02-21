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

import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFGDebug;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ExplodedGraphWalker;
import org.sonar.java.se.LearnedAssociation;
import org.sonar.java.se.LearnedConstraint;
import org.sonar.java.se.ProgramStateDataProvider;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javafx.scene.web.WebEngine;

public class EGViewer {

  private static final ActionParser<Tree> PARSER = JavaParser.createParser(StandardCharsets.UTF_8);
  private final Viewer viewer;
  private static final boolean SHOW_MULTIPLE_PARENTS = true;

  EGViewer(Viewer viewer) {
    this.viewer = viewer;
  }

  public void analyse(String source){
    ExplodedGraph eg = buildEG(source);
    CFG cfg = CFGViewer.buildCFG(source);
    viewer.textArea.setText(CFGDebug.toString(cfg));
    int firstBlockId = cfg.blocks().get(0).id();
    String dot = egToDot(eg, firstBlockId);
    WebEngine webEngine = viewer.webView.getEngine();
    webEngine.executeScript("loadEG('" + dot + "', " + (!SHOW_MULTIPLE_PARENTS) + ")");
  }

  private static ExplodedGraph buildEG(String source) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(source);
    SemanticModel semanticModel = SemanticModel.createFor(cut, Lists.newArrayList());
    MethodTree firstMethod = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
    return getEg(cut, semanticModel, firstMethod);
  }

  private static ExplodedGraph getEg(CompilationUnitTree cut, SemanticModel semanticModel, MethodTree methodTree) {
    JavaFileScannerContext mockContext = mock(JavaFileScannerContext.class);
    when(mockContext.getTree()).thenReturn(cut);
    when(mockContext.getSemanticModel()).thenReturn(semanticModel);
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Lists.newArrayList()) {
      @Override
      public MethodBehavior execute(MethodTree methodTree) {
        this.context = mockContext;
        return super.execute(methodTree);
      }
    };
    ExplodedGraphWalker walker = new ExplodedGraphWalker(sev.behaviorCache, semanticModel);
    walker.visitMethod(methodTree, new MethodBehavior(methodTree.symbol()));
    return walker.getExplodedGraph();
  }

  private static String egToDot(ExplodedGraph eg, int firstBlockId) {
    String result = "graph ExplodedGraph { ";
    List<ExplodedGraph.Node> nodes = new ArrayList<>(eg.nodes().keySet());
    int index = 0;
    for (ExplodedGraph.Node node : nodes) {
      List<ExplodedGraph.Node> parents = Lists.newArrayList(node.parents());
      result += graphNode(index, node, parents, firstBlockId);
      if (!parents.isEmpty()) {
        ExplodedGraph.Node firstParent = node.parent();
        result += parentEdge(nodes.indexOf(firstParent), index, node, firstParent);

        int nbParents = parents.size();
        if (SHOW_MULTIPLE_PARENTS && nbParents > 1) {
          List<ExplodedGraph.Node> others = parents.subList(1, nbParents);
          for (ExplodedGraph.Node other : others) {
            result += parentEdge(nodes.indexOf(other), index, node, other);
          }
        }
      }
      index++;
    }
    return result + "}";

  }

  private static String graphNode(int index, ExplodedGraph.Node node, List<ExplodedGraph.Node> parents, int firstBlockId) {
    ProgramStateDataProvider psProvider = new ProgramStateDataProvider(node.programState);
    return new StringBuilder()
      .append(index)
      .append("[")
      .append("label=\"" + node.programPoint + "\"")
      .append(",psStack=\"" + psProvider.stack() + "\"")
      .append(",psConstraints=\"" + psProvider.constraints() + "\"")
      .append(",psValues=\"" + psProvider.values() + "\"")
      .append(",psLastEvaluatedSymbol=\"" + psProvider.lastEvaluatedSymbol() + "\"")
      .append(specialHighlight(node, parents, firstBlockId))
      .append("]")
      .toString();
  }

  private static String specialHighlight(ExplodedGraph.Node node, List<ExplodedGraph.Node> parents, int firstBlockId) {
    if (parents.isEmpty()) {
      if (isFirstBlock(node, firstBlockId)) {
        return ",color=\"green\",fontcolor=\"white\"";
      }
      // lost nodes - should never happen - worth investigation if appears in viewer
      return ",color=\"red\",fontcolor=\"white\"";
    } else if (node.programPoint.toString().startsWith("B0.0")) {
      return ",color=\"black\",fontcolor=\"white\"";
    }
    return "";
  }

  private static boolean isFirstBlock(ExplodedGraph.Node node, int firstBlockId) {
    return node.programPoint.toString().startsWith("B" + firstBlockId + "." + "0");
  }

  private static String parentEdge(int from, int to, ExplodedGraph.Node node, ExplodedGraph.Node parent) {
    String yield = yield(node, parent);
    return from + "->" + to
      + "[label=\""
      + node.learnedAssociations().map(LearnedAssociation::toString).collect(Collectors.joining(","))
      + "\\n"
      + node.learnedConstraints().map(LearnedConstraint::toString).collect(Collectors.joining(","))
      + "\""
      + (yield.isEmpty() ? handleException(node) : yield)
      + "] ";
  }

  private static String handleException(ExplodedGraph.Node node) {
    if (node.programState.peekValue() instanceof SymbolicValue.ExceptionalSymbolicValue) {
      return ",color=\"red\",fontcolor=\"red\"";
    }
    return "";
  }

  private static String yield(ExplodedGraph.Node node, ExplodedGraph.Node parent) {
    MethodYield selectedMethodYield = node.selectedMethodYield(parent);
    if (selectedMethodYield != null) {
      return String.format(",color=\"purple\",fontcolor=\"purple\",selectedMethodYield=\"%s\"", selectedMethodYield);
    }
    return "";
  }
}
