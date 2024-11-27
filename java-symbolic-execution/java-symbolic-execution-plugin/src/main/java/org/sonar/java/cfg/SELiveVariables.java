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
package org.sonar.java.cfg;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonarsource.analyzer.commons.collections.SetUtils;

public class SELiveVariables {

  private final ControlFlowGraph cfg;
  private final Map<ControlFlowGraph.Block, Set<Symbol>> out = new HashMap<>();
  private final Map<ControlFlowGraph.Block, Set<Symbol>> in = new HashMap<>();

  private SELiveVariables(ControlFlowGraph cfg) {
    this.cfg = cfg;
  }

  private void analyzeControlFlowGraph(Map<ControlFlowGraph.Block, Set<Symbol>> in, Map<ControlFlowGraph.Block, Set<Symbol>> kill, Map<ControlFlowGraph.Block, Set<Symbol>> gen) {
    Deque<ControlFlowGraph.Block> workList = new LinkedList<>();
    workList.addAll(cfg.reversedBlocks());
    while (!workList.isEmpty()) {
      ControlFlowGraph.Block block = workList.removeFirst();

      Set<Symbol> blockOut = out.computeIfAbsent(block, k -> new HashSet<>());
      block.successors().stream().map(in::get).filter(Objects::nonNull).forEach(blockOut::addAll);
      block.exceptions().stream().map(in::get).filter(Objects::nonNull).forEach(blockOut::addAll);
      // in = gen and (out - kill)
      Set<Symbol> newIn = new HashSet<>(gen.get(block));
      newIn.addAll(SetUtils.difference(blockOut, kill.get(block)));

      if (newIn.equals(in.get(block))) {
        continue;
      }
      in.put(block, newIn);
      block.predecessors().forEach(workList::addLast);
    }
  }

  /**
   * Returns SELiveVariables object with information concerning local variables and parameters
   */
  public static SELiveVariables analyze(ControlFlowGraph cfg) {
    SELiveVariables liveVariables = new SELiveVariables(cfg);
    // Generate kill/gen for each block in isolation
    Map<ControlFlowGraph.Block, Set<Symbol>> kill = new HashMap<>();
    Map<ControlFlowGraph.Block, Set<Symbol>> gen = new HashMap<>();
    for (ControlFlowGraph.Block block : liveVariables.cfg.reversedBlocks()) {
      Set<Symbol> blockKill = new HashSet<>();
      Set<Symbol> blockGen = new HashSet<>();
      liveVariables.processBlockElements(block, blockKill, blockGen);
      kill.put(block, blockKill);
      gen.put(block, blockGen);
    }
    liveVariables.analyzeControlFlowGraph(liveVariables.in, kill, gen);
    // out of exit block are empty by definition.
    if (!liveVariables.out.get(liveVariables.cfg.reversedBlocks().get(0)).isEmpty()) {
      throw new IllegalStateException("Out of exit block should be empty");
    }

    // Make things immutable.
    for (Map.Entry<ControlFlowGraph.Block, Set<Symbol>> blockSetEntry : liveVariables.out.entrySet()) {
      blockSetEntry.setValue(Collections.unmodifiableSet(blockSetEntry.getValue()));
    }

    return liveVariables;
  }

  public Set<Symbol> getOut(ControlFlowGraph.Block block) {
    return out.get(block);
  }

  private static void processAssignment(AssignmentExpressionTree element, Set<Symbol> blockKill, Set<Symbol> blockGen, Set<Tree> assignmentLHS) {
    Symbol symbol = null;
    ExpressionTree lhs = element.variable();
    if (lhs.is(Kind.IDENTIFIER)) {
      symbol = ((IdentifierTree) lhs).symbol();

    }

    if (symbol != null && includeSymbol(symbol)) {
      assignmentLHS.add(lhs);
      blockGen.remove(symbol);
      blockKill.add(symbol);
    }
  }

  private void processBlockElements(ControlFlowGraph.Block block, Set<Symbol> blockKill, Set<Symbol> blockGen) {
    // process elements from bottom to top
    Set<Tree> assignmentLHS = new HashSet<>();
    for (Tree element : ListUtils.reverse(block.elements())) {
      switch (element.kind()) {
        case ASSIGNMENT:
          processAssignment((AssignmentExpressionTree) element, blockKill, blockGen, assignmentLHS);
          break;
        case IDENTIFIER:
          processIdentifier((IdentifierTree) element, blockGen, assignmentLHS);
          break;
        case VARIABLE:
          blockKill.add(((VariableTree) element).symbol());
          blockGen.remove(((VariableTree) element).symbol());
          break;
        case LAMBDA_EXPRESSION:
          blockGen.addAll(getUsedVariables(((LambdaExpressionTree) element).body(), cfg.methodSymbol()));
          break;
        case METHOD_REFERENCE:
          blockGen.addAll(getUsedVariables(((MethodReferenceTree) element).expression(), cfg.methodSymbol()));
          break;
        case NEW_CLASS:
          blockGen.addAll(getUsedVariables(((NewClassTree) element).classBody(), cfg.methodSymbol()));
          break;
        default:
          // Ignore other kind of elements, no change of gen/kill
      }
    }
  }

  private static void processIdentifier(IdentifierTree element, Set<Symbol> blockGen, Set<Tree> assignmentLHS) {
    Symbol symbol = element.symbol();
    if (!assignmentLHS.contains(element) && includeSymbol(symbol)) {
      blockGen.add(symbol);
    }
  }

  private static boolean includeSymbol(Symbol symbol) {
    return symbol.isLocalVariable();
  }

  private static Set<Symbol> getUsedVariables(@Nullable Tree syntaxNode, Symbol.MethodSymbol owner) {
    if (syntaxNode == null) {
      return Collections.emptySet();
    }
    VariableReadExtractor extractorFromClass = new VariableReadExtractor(owner, false);
    syntaxNode.accept(extractorFromClass);
    return extractorFromClass.usedVariables();
  }

}
