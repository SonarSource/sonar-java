/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class
    LiveVariables {

  private final CFG cfg;
  private final Map<CFG.Block, Set<Symbol>> out = new HashMap<>();

  private LiveVariables(CFG cfg) {
    this.cfg = cfg;
  }

  public Set<Symbol> getOut(CFG.Block block) {
    return out.get(block);
  }

  public static LiveVariables analyze(CFG cfg) {
    LiveVariables liveVariables = new LiveVariables(cfg);
    final Map<CFG.Block, Set<Symbol>> in = new HashMap<>();
    // Generate kill/gen for each block in isolation
    Map<CFG.Block, Set<Symbol>> kill = new HashMap<>();
    Map<CFG.Block, Set<Symbol>> gen = new HashMap<>();
    for (CFG.Block block : liveVariables.cfg.reversedBlocks()) {
      Set<Symbol> blockKill = new HashSet<>();
      Set<Symbol> blockGen = new HashSet<>();
      liveVariables.processBlockElements(block, blockKill, blockGen);
      kill.put(block, blockKill);
      gen.put(block, blockGen);
    }
    liveVariables.analyzeCFG(in, kill, gen);
    // out of exit block are empty by definition.
    if (!liveVariables.out.get(liveVariables.cfg.reversedBlocks().get(0)).isEmpty()) {
      throw new IllegalStateException("Out of exit block should be empty");
    }

    // Make things immutable.
    for (Map.Entry<CFG.Block, Set<Symbol>> blockSetEntry : liveVariables.out.entrySet()) {
      blockSetEntry.setValue(ImmutableSet.copyOf(blockSetEntry.getValue()));
    }

    return liveVariables;
  }

  private void analyzeCFG(Map<CFG.Block, Set<Symbol>> in, Map<CFG.Block, Set<Symbol>> kill, Map<CFG.Block, Set<Symbol>> gen) {
    Deque<CFG.Block> workList = new LinkedList<>();
    workList.addAll(cfg.reversedBlocks());
    while (!workList.isEmpty()) {
      CFG.Block block = workList.removeFirst();

      Set<Symbol> blockOut = out.get(block);
      if (blockOut == null) {
        blockOut = new HashSet<>();
        out.put(block, blockOut);
      }
      for (CFG.Block successor : block.successors()) {
        Set<Symbol> inOfSuccessor = in.get(successor);
        if (inOfSuccessor != null) {
          blockOut.addAll(inOfSuccessor);
        }
      }
      // in = gen and (out - kill)
      Set<Symbol> newIn = new HashSet<>(gen.get(block));
      newIn.addAll(Sets.difference(blockOut, kill.get(block)));

      if (newIn.equals(in.get(block))) {
        continue;
      }
      in.put(block, newIn);
      for (CFG.Block predecessor : block.predecessors()) {
        workList.addLast(predecessor);
      }
    }
  }

  private void processBlockElements(CFG.Block block, Set<Symbol> blockKill, Set<Symbol> blockGen) {
    // process elements from bottom to top
    Set<Tree> assignmentLHS = new HashSet<>();
    for (Tree element : Lists.reverse(block.elements())) {
      Symbol symbol;
      switch (element.kind()) {
        case ASSIGNMENT:
          ExpressionTree lhs = ((AssignmentExpressionTree) element).variable();
          if (lhs.is(Tree.Kind.IDENTIFIER)) {
            symbol = ((IdentifierTree) lhs).symbol();
            if (isLocalVariable(symbol)) {
              assignmentLHS.add(lhs);
              blockGen.remove(symbol);
              blockKill.add(symbol);
            }
          }
          break;
        case IDENTIFIER:
          symbol = ((IdentifierTree) element).symbol();
          if (!assignmentLHS.contains(element) && isLocalVariable(symbol)) {
            blockGen.add(symbol);
          }
          break;
        case VARIABLE:
          blockKill.add(((VariableTree) element).symbol());
          blockGen.remove(((VariableTree) element).symbol());
          break;
        case LAMBDA_EXPRESSION:
          blockGen.addAll(getUsedVariables(((LambdaExpressionTree) element).body(), cfg.methodSymbol()));
          break;
        case NEW_CLASS:
          blockGen.addAll(getUsedVariables(((NewClassTree) element).classBody(), cfg.methodSymbol()));
          break;
        default:
          // Ignore other kind of elements, no change of gen/kill
      }
    }
  }

  private static boolean isLocalVariable(Symbol symbol) {
    return symbol.owner().isMethodSymbol();
  }

  private static List<Symbol> getUsedVariables(@Nullable Tree syntaxNode, Symbol.MethodSymbol owner) {
    if(syntaxNode == null) {
      return Collections.emptyList();
    }
    LocalVariableReadExtractor extractorFromClass = new LocalVariableReadExtractor(owner);
    syntaxNode.accept(extractorFromClass);
    return extractorFromClass.usedVariables();
  }

}
