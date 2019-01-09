/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CFGLoop {

  private final CFG.Block startingBlock;
  private final Set<CFG.Block> blocks = new HashSet<>();
  private final Set<CFG.Block> successors = new HashSet<>();

  private CFGLoop(CFG.Block block) {
    startingBlock = block;
  }

  private void initialize(CFG.Block block, Map<Tree, CFGLoop> container) {
    Block loopFirstBlock = block.trueBlock();
    if (loopFirstBlock == null) {
      // Special case where no condition is given in FOR loop: only one successor specified
      loopFirstBlock = block.successors().iterator().next();
    }
    collectBlocks(loopFirstBlock, container);
    successors.addAll(block.successors());
    successors.remove(block.falseBlock());
    collectWaysOut(container);
  }

  @VisibleForTesting
  CFG.Block startingBlock() {
    return startingBlock;
  }

  @VisibleForTesting
  Collection<CFG.Block> blocks() {
    return new ArrayList<>(blocks);
  }

  @VisibleForTesting
  Collection<CFG.Block> successors() {
    return new ArrayList<>(successors);
  }

  public boolean hasNoWayOut() {
    return successors.isEmpty();
  }

  private void collectBlocks(CFG.Block block, Map<Tree, CFGLoop> container) {
    collectBlocks(block, container, new HashSet<CFG.Block>());
  }

  private boolean collectBlocks(CFG.Block block, Map<Tree, CFGLoop> container, Set<CFG.Block> visitedBlocks) {
    if (blocks.contains(block)) {
      return true;
    }
    if (block.id() == startingBlock.id() || !visitedBlocks.add(block)) {
      return false;
    }
    boolean answer = returnsToStart(block, container, visitedBlocks);
    if (answer || isBreak(block)) {
      blocks.add(block);
    }
    return answer;
  }

  private boolean returnsToStart(CFG.Block block, Map<Tree, CFGLoop> container, Set<CFG.Block> visitedBlocks) {
    Set<Block> localSuccessors = localSuccessors(block, container);
    if (localSuccessors == null) {
      return true;
    }
    boolean answer = false;
    for (CFG.Block successor : localSuccessors) {
      if (startingBlock.id() == successor.id()) {
        answer = true;
      } else {
        answer |= collectBlocks(successor, container, visitedBlocks);
      }
    }
    return answer;
  }

  @CheckForNull
  private static Set<Block> localSuccessors(CFG.Block block, Map<Tree, CFGLoop> container) {
    if (isStarting(block)) {
      CFGLoop loop = container.get(block.terminator());
      if (loop == null) {
        loop = create(block, container);
      }
      Set<Block> loopSuccessors = new HashSet<>(loop.successors);
      if (block.trueBlock() == null) {
        // Special case where no condition is given in FOR loop: only one successor specified
        return null;
      } else {
        loopSuccessors.add(block.falseBlock());
      }
      return loopSuccessors;
    }
    return block.successors();
  }

  private static boolean isBreak(CFG.Block block) {
    Tree terminator = block.terminator();
    return terminator != null && terminator.is(Tree.Kind.BREAK_STATEMENT);
  }

  private void collectWaysOut(Map<Tree, CFGLoop> container) {
    for (CFG.Block block : blocks) {
      if (isStarting(block)) {
        CFGLoop innerLoop = container.get(block.terminator());
        successors.addAll(innerLoop.successors());
      } else {
        successors.addAll(block.successors());
      }
    }
    successors.removeAll(blocks);
    successors.remove(startingBlock);
  }

  private static boolean isStarting(CFG.Block block) {
    Tree terminator = block.terminator();
    return terminator != null && terminator.is(Tree.Kind.FOR_STATEMENT, Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT);
  }

  public static Map<Tree, CFGLoop> getCFGLoops(CFG cfg) {
    Map<Tree, CFGLoop> cfgLoops = new HashMap<>();
    for (CFG.Block block : cfg.blocks()) {
      if (CFGLoop.isStarting(block)) {
        Tree terminator = block.terminator();
        if (!cfgLoops.containsKey(terminator)) {
          create(block, cfgLoops);
        }
      }
    }
    return cfgLoops;
  }

  private static CFGLoop create(CFG.Block block, Map<Tree, CFGLoop> container) {
    CFGLoop loop = new CFGLoop(block);
    container.put(block.terminator(), loop);
    loop.initialize(block, container);
    return loop;
  }
}
