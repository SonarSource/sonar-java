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
package org.sonar.java.cfg;

import com.sonar.sslr.api.typed.ActionParser;

import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.viewer.DotDataProvider;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.json.JsonObject;

import java.util.Collections;
import java.util.List;

public class CFGViewer {

  private CFGViewer() {
  }

  private static final String NEW_LINE = "\n";
  private static final ActionParser<Tree> PARSER = JavaParser.createParser();

  public static CFG buildCFG(String source) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(source);
    SemanticModel.createFor(cut, new SquidClassLoader(Collections.emptyList()));
    return CFG.build(getFirstMethod(cut));
  }

  private static MethodTree getFirstMethod(CompilationUnitTree cut) {
    ClassTree classTree = (ClassTree) cut.types().get(0);
    return (MethodTree) classTree.members().stream()
      .filter(m -> m.is(Tree.Kind.METHOD))
      .findFirst()
      .orElse(null);
  }

  /**
   * Convert the CFG to DOT format (graph description language).
   * See language specification: http://www.graphviz.org/content/dot-language
   */
  public static String toDot(CFG cfg) {
    StringBuilder sb = new StringBuilder();
    List<CFG.Block> blocks = cfg.blocks();

    // nodes
    int firstBlockId = blocks.size() - 1;
    blocks.stream()
      .map(CFG.Block::id)
      .map(id -> new CFGDotNode(id, id == firstBlockId))
      .map(DotDataProvider.Node::node)
      .forEach(sb::append);

    sb.append(NEW_LINE);

    // edges
    for (CFG.Block block : blocks) {
      block.successors().stream()
        .map(successor -> new CFGDotEdge(block, successor))
        .map(DotDataProvider.Edge::edge)
        .forEach(sb::append);
      block.exceptions().stream()
        .map(exception -> new CFGDotEdge(block, exception, "EXCEPTION", DotDataProvider.Highlighting.EXCEPTION_EDGE))
        .map(DotDataProvider.Edge::edge)
        .forEach(sb::append);
    }

    return escapeNewLines("graph cfg {" + NEW_LINE + sb.toString() + "}");
  }

  private static String escapeNewLines(String dotGraph) {
    return dotGraph.replaceAll("\\n", "\\\\n");
  }

  private static class CFGDotNode extends DotDataProvider.Node {

    private final int blockId;
    private final boolean isFirstBlock;
    private final boolean isExitBlock;

    public CFGDotNode(int blockId, boolean isFirstBlock) {
      super(blockId);
      this.blockId = blockId;
      this.isFirstBlock = isFirstBlock;
      this.isExitBlock = blockId == 0;
    }

    @Override
    public String label() {
      String label = "B" + blockId;
      if (isExitBlock) {
        label += " (EXIT)";
      } else if (isFirstBlock) {
        label += " (START)";
      }
      return label;
    }

    @Override
    public Highlighting highlighting() {
      if (isExitBlock) {
        return Highlighting.EXIT_NODE;
      }
      if (isFirstBlock) {
        return Highlighting.FIRST_NODE;
      }
      return null;
    }

    @Override
    public JsonObject details() {
      return null;
    }
  }

  private static class CFGDotEdge extends DotDataProvider.Edge {

    private final String label;
    private final Highlighting highlighting;

    public CFGDotEdge(CFG.Block from, CFG.Block to) {
      super(from.id(), to.id());
      this.label = label(from, to);
      this.highlighting = null;
    }

    public CFGDotEdge(CFG.Block from, CFG.Block to, String label, Highlighting highlighting) {
      super(from.id(), to.id());
      this.label = label;
      this.highlighting = highlighting;
    }

    @Override
    public String label() {
      return label;
    }

    @CheckForNull
    private static String label(Block block, Block successor) {
      if (successor == block.trueBlock()) {
        return "TRUE";
      } else if (successor == block.falseBlock()) {
        return "FALSE";
      } else if (successor == block.exitBlock()) {
        return "EXIT";
      }
      return null;
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
}
