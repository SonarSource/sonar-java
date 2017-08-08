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
import org.sonar.java.se.xproc.BehaviorCache;
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
    ExplodedGraphWalker.ExplodedGraphWalkerFactory egwFactory = new ExplodedGraphWalker.ExplodedGraphWalkerFactory(Collections.emptyList());
    ExplodedGraphWalker walker = egwFactory.createWalker(sev.behaviorCache, semanticModel);
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
      result.append(node(index, node, edges.isEmpty(), firstBlockId, viewer.behaviorCache));
      Stream<ExplodedGraph.Edge> edgeStream = edges.stream();
      if (!SHOW_MULTIPLE_PARENTS) {
        edgeStream = edgeStream.limit(1);
      }
      int finalIndex = index;
      edgeStream.map(e -> new EGDotEdge(nodes.indexOf(e.parent()), finalIndex, e))
        .map(EGDotEdge::edge)
        .forEach(result::append);
      index++;
    }
    return result.append("}").toString();

  }

  private static String node(int index, ExplodedGraph.Node node, boolean hasParents, int firstBlockId, BehaviorCache behaviorCache) {
    return new EGDotNode(index, node, behaviorCache, hasParents, firstBlockId).node();
  }

}
