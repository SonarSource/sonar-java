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

import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.viewer.DotGraph;
import org.sonar.java.viewer.Viewer;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EGDotGraph extends DotGraph {

  private static final boolean SHOW_MULTIPLE_PARENTS = true;

  private ExplodedGraph explodedGraph;
  private BehaviorCache behaviorCache;
  private final CompilationUnitTree cut;
  private final MethodTree methodToAnalyze;
  private final SemanticModel semanticModel;
  private final int cfgFirstBlockId;

  public EGDotGraph(Viewer.Base base) {
    this(base.cut, base.firstMethod, base.semanticModel, base.cfgFirstMethod.blocks().get(0).id());
  }

  private EGDotGraph(CompilationUnitTree cut, MethodTree method, SemanticModel semanticModel, int cfgFirstBlockId) {
    this.cut = cut;
    this.methodToAnalyze = method;
    this.semanticModel = semanticModel;
    this.cfgFirstBlockId = cfgFirstBlockId;
    computeEG();
  }

  private void computeEG() {
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
    ExplodedGraphWalker.ExplodedGraphWalkerFactory egwFactory = new ExplodedGraphWalker.ExplodedGraphWalkerFactory(Collections.emptyList());
    ExplodedGraphWalker walker = egwFactory.createWalker(sev.behaviorCache, semanticModel);
    walker.visitMethod(methodToAnalyze, new MethodBehavior(methodToAnalyze.symbol()));

    this.explodedGraph = walker.getExplodedGraph();
    this.behaviorCache = sev.behaviorCache;
  }

  @Override
  public String name() {
    return "ExplodedGraph";
  }

  @Override
  public void build() {
    List<ExplodedGraph.Node> egNodes = new ArrayList<>(explodedGraph.nodes().keySet());
    int index = 0;
    for (ExplodedGraph.Node node : egNodes) {
      Collection<ExplodedGraph.Edge> egEdges = node.edges();
      addNode(new EGDotNode(index, node, behaviorCache, egEdges.isEmpty(), cfgFirstBlockId));
      Stream<ExplodedGraph.Edge> edgeStream = egEdges.stream();
      if (!SHOW_MULTIPLE_PARENTS) {
        edgeStream = edgeStream.limit(1);
      }
      int finalIndex = index;
      edgeStream.map(e -> new EGDotEdge(egNodes.indexOf(e.parent()), finalIndex, e)).forEach(this::addEdge);
      index++;
    }
  }
}
