/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.se;

import com.google.common.collect.Maps;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExplodedGraph {

  private Map<Node, Node> nodes = Maps.newHashMap();

  /**
   * Returns node associated with given (programPoint,programState) pair. If no node for this pair exists, it is created.
   */
  Node getNode(ProgramPoint programPoint, @Nullable ProgramState programState) {
    Node result = new Node(programPoint, programState);
    Node cached = nodes.get(result);
    if (cached != null) {
      cached.isNew = false;
      return cached;
    }
    result.isNew = true;
    nodes.put(result, result);
    return result;
  }

  public static interface ProgramPoint {

    Block block();

    ProgramPoint next();

    Tree terminator();

    boolean atEnd();

    Tree currentElement();

    boolean hasBlock();

    Collection<Block> successors();

  }

  public static class FinallyProgramPoint implements ProgramPoint {

    private int hashcode;
    private final List<CFG.Block> blocks;
    private final int blockStep;
    private final int step;

    public FinallyProgramPoint(List<CFG.Block> blocks) {
      this(blocks, 0, 0);
    }

    private FinallyProgramPoint(List<CFG.Block> blocks, int blockStep, int step) {
      this.blocks = new ArrayList<>(blocks);
      this.step = step;
      this.blockStep = adjustBlockIndex(blockStep);
    }

    private int adjustBlockIndex(int blockStep) {
      int n = blockStep;
      while (blocks.get(n).elements().isEmpty()) {
        n += 1;
        if (n == blocks.size()) {
          break;
        }
      }
      return n;
    }

    @Override
    public int hashCode() {
      if (hashcode == 0) {
        long hash = step;
        for (Block block : blocks) {
          hash = block.id() * 31 + hash >> 16;
        }
        hashcode = (int) hash;
      }
      return hashcode;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof FinallyProgramPoint) {
        FinallyProgramPoint other = (FinallyProgramPoint) obj;
        if (hasBlock() && other.hasBlock()) {
          return this.blocks.get(blockStep).id() == other.blocks.get(other.blockStep).id()
            && this.step == other.step;
        }
        return blockStep == other.blockStep;
      }
      return false;
    }

    @Override
    public Block block() {
      return blocks.get(blockStep);
    }

    @Override
    public ProgramPoint next() {
      int nextBlockStep = blockStep;
      int nextStep = step + 1;
      if (nextStep == block().elements().size()) {
        nextBlockStep += 1;
        nextStep = 0;
      }
      return new FinallyProgramPoint(blocks, nextBlockStep, nextStep);
    }

    @Override
    public Tree terminator() {
      return block().terminator();
    }

    @Override
    public boolean atEnd() {
      return blockStep == blocks.size();
    }

    @Override
    public Tree currentElement() {
      return block().elements().get(step);
    }

    @Override
    public boolean hasBlock() {
      return blockStep < blocks.size();
    }

    @Override
    public Collection<Block> successors() {
      return blocks.get(blocks.size() - 1).successors();
    }

    @Override
    public String toString() {
      return hasBlock() ? "F" + block().id() + "." + step : "F (empty)";
    }
  }

  public static class BlockProgramPoint implements ProgramPoint {

    private int hashcode;
    private final CFG.Block block;
    private final int step;

    public BlockProgramPoint(CFG.Block block) {
      this(block, 0);
    }

    private BlockProgramPoint(CFG.Block block, int step) {
      this.block = block;
      this.step = step;
    }

    @Override
    public ProgramPoint next() {
      return new BlockProgramPoint(block, step + 1);
    }

    @Override
    public int hashCode() {
      if(hashcode == 0) {
        hashcode = block.id() * 31 + step;
      }
      return hashcode;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof BlockProgramPoint) {
        BlockProgramPoint other = (BlockProgramPoint) obj;
        return this.block.id() == other.block.id()
          && this.step == other.step;
      }
      return false;
    }

    @Override
    public CFG.Block block() {
      return block;
    }

    @Override
    public Tree terminator() {
      return block.terminator();
    }

    @Override
    public boolean hasBlock() {
      return step < block.elements().size();
    }

    @Override
    public Tree currentElement() {
      return block.elements().get(step);
    }

    @Override
    public boolean atEnd() {
      return step == block.elements().size();
    }

    @Override
    public Collection<Block> successors() {
      return block.successors();
    }

    @Override
    public String toString() {
      return "B" + block.id() + "." + step;
    }
  }

  public static class Node {
    boolean isNew;

    /**
     * Execution location. Currently only pre-statement, but tomorrow we might add post-statement.
     */
    final ProgramPoint programPoint;
    @Nullable
    final ProgramState programState;

    Node(ProgramPoint programPoint, @Nullable ProgramState programState) {
      this.programPoint = programPoint;
      this.programState = programState;
    }

    @Override
    public int hashCode() {
      return programPoint.hashCode() * 31 + (programState == null ? 0 : programState.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Node) {
        Node other = (Node) obj;
        return this.programPoint.equals(other.programPoint)
          && Objects.equals(this.programState, other.programState);
      }
      return false;
    }

    @Override
    public String toString() {
      return programPoint + ": " + programState;
    }
  }
}
