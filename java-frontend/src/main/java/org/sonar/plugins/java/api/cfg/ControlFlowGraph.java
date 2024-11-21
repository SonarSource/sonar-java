/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.plugins.java.api.cfg;

import org.sonar.java.annotations.Beta;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.Tree;

@Beta
public interface ControlFlowGraph {

  Block entryBlock();

  List<? extends Block> blocks();

  Block exitBlock();

  boolean hasCompleteSemantic();

  List<? extends Block> reversedBlocks();

  Symbol.MethodSymbol methodSymbol();

  interface Block {
    int id();

    List<Tree> elements();

    @CheckForNull
    Tree terminator();

    Set<? extends Block> successors();

    Set<? extends Block> predecessors();

    Set<? extends Block> exceptions();

    boolean isCatchBlock();

    boolean isDefaultBlock();

    boolean isFinallyBlock();

    @CheckForNull
    CaseGroupTree caseGroup();

    Block trueBlock();

    Block falseBlock();

    Block exitBlock();
  }

}
