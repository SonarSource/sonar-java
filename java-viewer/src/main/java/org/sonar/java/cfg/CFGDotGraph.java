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

import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.viewer.DotGraph;

import javax.annotation.CheckForNull;
import javax.json.JsonObject;

import java.util.List;

public class CFGDotGraph extends DotGraph {

  private final CFG cfg;

  public CFGDotGraph(CFG cfg) {
    this.cfg = cfg;
  }

  @Override
  public void build() {
    List<CFG.Block> blocks = cfg.blocks();

    // nodes
    int firstBlockId = blocks.size() - 1;
    blocks.stream()
      .map(CFG.Block::id)
      .map(id -> new CFGDotNode(id, id == firstBlockId))
      .forEach(this::addNode);

    // edges
    for (CFG.Block block : blocks) {
      block.successors().stream()
        .map(successor -> new CFGDotEdge(block, successor))
        .forEach(this::addEdge);
      block.exceptions().stream()
        .map(exception -> new CFGDotEdge(block, exception, "EXCEPTION", DotGraph.Highlighting.EXCEPTION_EDGE))
        .forEach(this::addEdge);
    }
  }

  private static class CFGDotNode extends DotGraph.Node {

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

  private static class CFGDotEdge extends DotGraph.Edge {

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

  @Override
  public String name() {
    return "CFG";
  }

}
