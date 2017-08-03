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
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.java.viewer.DotHelper;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EGViewer {

  private ExplodedGraph explodedGraph;
  private BehaviorCache behaviorCache;

  private EGViewer(String source) {
    buildEG(source);
  }

  private static final ActionParser<Tree> PARSER = JavaParser.createParser();
  private static final boolean SHOW_MULTIPLE_PARENTS = true;

  public static String toDot(String source, int cfgFirstBlockId) {
    EGViewer viewer = new EGViewer(source);
    return egToDot(viewer, cfgFirstBlockId);
  }

  private void buildEG(String source) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(source);
    SemanticModel semanticModel = SemanticModel.createFor(cut, new SquidClassLoader(Collections.emptyList()));
    computeEG(cut, semanticModel, getFirstMethod(cut));
  }

  private static MethodTree getFirstMethod(CompilationUnitTree cut) {
    ClassTree classTree = (ClassTree) cut.types().get(0);
    return (MethodTree) classTree.members().stream()
      .filter(m -> m.is(Tree.Kind.METHOD))
      .findFirst()
      .orElse(null);
  }

  private void computeEG(CompilationUnitTree cut, SemanticModel semanticModel, MethodTree methodTree) {
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

    this.explodedGraph = walker.getExplodedGraph();
    this.behaviorCache = sev.behaviorCache;
  }

  private static String egToDot(EGViewer viewer, int firstBlockId) {
    StringBuilder result = new StringBuilder("graph ExplodedGraph { ");
    List<ExplodedGraph.Node> nodes = new ArrayList<>(viewer.explodedGraph.nodes().keySet());
    int index = 0;
    for (ExplodedGraph.Node node : nodes) {
      Collection<ExplodedGraph.Edge> edges = node.edges();
      result.append(graphNode(index, node, edges.isEmpty(), firstBlockId, viewer.behaviorCache));
      Stream<ExplodedGraph.Edge> edgeStream = edges.stream();
      if (!SHOW_MULTIPLE_PARENTS) {
        edgeStream = edgeStream.limit(1);
      }
      int finalIndex = index;
      edgeStream.map(e -> edge(nodes.indexOf(e.parent()), finalIndex, e)).forEach(result::append);
      index++;
    }
    return result.append("}").toString();

  }

  private static String graphNode(int index, ExplodedGraph.Node node, boolean hasParents, int firstBlockId, BehaviorCache behaviorCache) {
    ProgramStateDataProvider psDataProvider = new ProgramStateDataProvider(node, behaviorCache);
    Map<String, String> extraFields = new HashMap<>();
    extraFields.put("psStack", psDataProvider.stack());
    extraFields.put("psConstraints", psDataProvider.constraints());
    extraFields.put("psValues", psDataProvider.values());
    String yields = psDataProvider.yields();
    if (yields != null) {
      extraFields.put("methodYields", yields);
      extraFields.put("methodName", psDataProvider.methodName());
    }
    return DotHelper.node(
      index,
      psDataProvider.programPoint(),
      specialNodeHighlight(node, hasParents, firstBlockId, psDataProvider),
      extraFields);
  }

  @CheckForNull
  private static DotHelper.Highlighting specialNodeHighlight(ExplodedGraph.Node node, boolean hasParents, int firstBlockId, ProgramStateDataProvider psDataProvider) {
    if (hasParents) {
      if (isFirstBlock(node, firstBlockId)) {
        return DotHelper.Highlighting.FIRST_NODE;
      }
      // lost nodes - should never happen - worth investigation if appears in viewer
      return DotHelper.Highlighting.LOST_NODE;
    } else if (psDataProvider.programPoint().startsWith("B0.0")) {
      return DotHelper.Highlighting.EXIT_NODE;
    }
    return null;
  }

  private static boolean isFirstBlock(ExplodedGraph.Node node, int firstBlockId) {
    return node.programPoint.toString().startsWith("B" + firstBlockId + "." + "0");
  }

  private static String edge(int from, int to, ExplodedGraph.Edge edge) {
    String label = learnedAssociations(edge) + "\\n" + learnedConstraints(edge);
    Map<String, String> yield = yield(edge);
    if (!yield.isEmpty()) {
      return DotHelper.edge(from, to, label, DotHelper.Highlighting.YIELD_EDGE, yield);
    }
    return DotHelper.edge(from, to, label, specialEdgeHighlight(edge.child()));
  }

  private static String learnedConstraints(ExplodedGraph.Edge edge) {
    return edge.learnedConstraints().stream().map(LearnedConstraint::toString).collect(Collectors.joining(","));
  }

  private static String learnedAssociations(ExplodedGraph.Edge edge) {
    return edge.learnedAssociations().stream().map(LearnedAssociation::toString).collect(Collectors.joining(","));
  }

  @CheckForNull
  private static DotHelper.Highlighting specialEdgeHighlight(ExplodedGraph.Node node) {
    if (node.programState.peekValue() instanceof SymbolicValue.ExceptionalSymbolicValue) {
      return DotHelper.Highlighting.EXCEPTION_EDGE;
    }
    return null;
  }

  private static Map<String, String> yield(ExplodedGraph.Edge edge) {
    Set<MethodYield> yields = edge.yields();
    if (yields.isEmpty()) {
      return Collections.emptyMap();
    }
    String yieldsAsText = yields.stream().map(ProgramStateDataProvider::yield).collect(Collectors.joining());
    Map<String, String> result = new HashMap<>();
    result.put("selectedMethodYield", yieldsAsText);
    return result;
  }
}
