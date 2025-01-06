/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.function.Predicate;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;

public class CFGUtils {

  private CFGUtils(){
    // utility class
  }

  public static boolean isMethodExitBlock(ControlFlowGraph.Block block) {
    return block.successors().isEmpty();
  }

  public static final Predicate<ControlFlowGraph.Block> IS_CATCH_BLOCK = ControlFlowGraph.Block::isCatchBlock;

}
