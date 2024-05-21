package org.sonar.java.model;

import java.util.function.Predicate;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;

public class CFGUtils {

  public static final Predicate<ControlFlowGraph.Block> IS_CATCH_BLOCK = ControlFlowGraph.Block::isCatchBlock;

  public static boolean isMethodExitBlock(ControlFlowGraph.Block block) {
    return block.successors().isEmpty();
  }



}
