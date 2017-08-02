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
package org.sonar.java.se;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;

import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EGViewer {

  private EGViewer() {
  }

  private static final ActionParser<Tree> PARSER = JavaParser.createParser();
  private static final boolean SHOW_MULTIPLE_PARENTS = true;

  public static String toDot(String source, int cfgFirstBlockId) {
    ExplodedGraph eg = buildEG(source);
    return egToDot(eg, cfgFirstBlockId);
  }

  private static ExplodedGraph buildEG(String source) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(source);
    SemanticModel semanticModel = SemanticModel.createFor(cut, new SquidClassLoader(Collections.emptyList()));
    MethodTree firstMethod = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
    return getEg(cut, semanticModel, firstMethod);
  }

  private static ExplodedGraph getEg(CompilationUnitTree cut, SemanticModel semanticModel, MethodTree methodTree) {
    JavaFileScannerContext mockContext = mock(JavaFileScannerContext.class);
    when(mockContext.getTree()).thenReturn(cut);
    when(mockContext.getSemanticModel()).thenReturn(semanticModel);
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Lists.newArrayList()) {
      @Override
      public void execute(MethodTree methodTree) {
        this.context = mockContext;
        super.execute(methodTree);
      }
    };
    ExplodedGraphWalker walker = new ExplodedGraphWalker(sev.behaviorCache, semanticModel);
    walker.visitMethod(methodTree, new MethodBehavior(methodTree.symbol()));
    return walker.getExplodedGraph();
  }

  private static String egToDot(ExplodedGraph eg, int firstBlockId) {
    StringBuilder result = new StringBuilder("graph ExplodedGraph { ");
    List<ExplodedGraph.Node> nodes = new ArrayList<>(eg.nodes().keySet());
    int index = 0;
    for (ExplodedGraph.Node node : nodes) {
      Collection<ExplodedGraph.Edge> edges = node.edges();
      result.append(graphNode(index, node, edges.isEmpty(), firstBlockId));
      Stream<ExplodedGraph.Edge> edgeStream = edges.stream();
      if (!SHOW_MULTIPLE_PARENTS) {
        edgeStream = edgeStream.limit(1);
      }
      int finalIndex = index;
      edgeStream.map(e -> parentEdge(nodes.indexOf(e.parent()), finalIndex, e)).forEach(result::append);
      index++;
    }
    return result.append("}").toString();

  }

  private static String graphNode(int index, ExplodedGraph.Node node, boolean hasParents, int firstBlockId) {
    ProgramStateDataProvider psDataProvider = new ProgramStateDataProvider(node);
    return new StringBuilder()
      .append(index)
      .append("[")
      .append("label=\"" + psDataProvider.programPoint() + "\"")
      .append(",psStack=\"" + psDataProvider.stack() + "\"")
      .append(",psConstraints=\"" + psDataProvider.constraints() + "\"")
      .append(",psValues=\"" + psDataProvider.values() + "\"")
      .append(specialHighlight(node, hasParents, firstBlockId, psDataProvider))
      .append("]")
      .toString();
  }

  private static String specialHighlight(ExplodedGraph.Node node, boolean hasParents, int firstBlockId, ProgramStateDataProvider psDataProvider) {
    if (hasParents) {
      if (isFirstBlock(node, firstBlockId)) {
        return specialHighlight("firstNode");
      }
      // lost nodes - should never happen - worth investigation if appears in viewer
      return specialHighlight("lostNode");
    } else if (psDataProvider.programPoint().startsWith("B0.0")) {
      return specialHighlight("exitNode");
    }
    return "";
  }

  private static String specialHighlight(String name) {
    return ",specialHighlight=\"" + name + "\"";
  }

  private static boolean isFirstBlock(ExplodedGraph.Node node, int firstBlockId) {
    return node.programPoint.toString().startsWith("B" + firstBlockId + "." + "0");
  }

  private static String parentEdge(int from, int to, ExplodedGraph.Edge edge) {
    String yield = yield(edge);
    return from + "->" + to
      + "[label=\""
      + edge.learnedAssociations().stream().map(LearnedAssociation::toString).collect(Collectors.joining(","))
      + "\\n"
      + edge.learnedConstraints().stream().map(LearnedConstraint::toString).collect(Collectors.joining(","))
      + "\""
      + (yield.isEmpty() ? handleException(edge.child()) : yield)
      + "] ";
  }

  private static String handleException(ExplodedGraph.Node node) {
    if (node.programState.peekValue() instanceof SymbolicValue.ExceptionalSymbolicValue) {
      return specialHighlight("exceptionEdge");
    }
    return "";
  }

  private static String yield(ExplodedGraph.Edge edge) {
    return edge.yields().stream()
      .map(y -> String.format("%s,selectedMethodYield=\"%s\"", specialHighlight("yieldEdge"), y))
      .collect(Collectors.joining());
  }
}
