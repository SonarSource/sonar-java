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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LiveVariables {

  private final CFG cfg;
  private final Map<CFG.Block, Set<Symbol>> out = new HashMap<>();
  private final Map<CFG.Block, Set<Symbol>> in = new HashMap<>();
  private final boolean includeFields;

  private LiveVariables(CFG cfg, boolean includeFields) {
    this.cfg = cfg;
    this.includeFields = includeFields;
  }

  public Set<Symbol> getOut(CFG.Block block) {
    return out.get(block);
  }

  public Set<Symbol> getIn(CFG.Block block) {
    return in.get(block);
  }

  /**
   * Returns LiveVariables object with information concerning local variables and parameters
   */
  public static LiveVariables analyze(CFG cfg) {
    return analyze(cfg, false);
  }

  /**
   * Returns LiveVariables object with information concerning local variables, parameters and fields
   */
  public static LiveVariables analyzeWithFields(CFG cfg) {
    return analyze(cfg, true);
  }

  private static LiveVariables analyze(CFG cfg, boolean includeFields) {
    LiveVariables liveVariables = new LiveVariables(cfg, includeFields);
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
    liveVariables.analyzeCFG(liveVariables.in, kill, gen);
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

      Set<Symbol> blockOut = out.computeIfAbsent(block, k -> new HashSet<>());
      block.successors().stream().map(in::get).filter(Objects::nonNull).forEach(blockOut::addAll);
      block.exceptions().stream().map(in::get).filter(Objects::nonNull).forEach(blockOut::addAll);
      // in = gen and (out - kill)
      Set<Symbol> newIn = new HashSet<>(gen.get(block));
      newIn.addAll(Sets.difference(blockOut, kill.get(block)));

      if (newIn.equals(in.get(block))) {
        continue;
      }
      in.put(block, newIn);
      block.predecessors().forEach(workList::addLast);
    }
  }

  private void processBlockElements(CFG.Block block, Set<Symbol> blockKill, Set<Symbol> blockGen) {
    // process elements from bottom to top
    Set<Tree> assignmentLHS = new HashSet<>();
    for (Tree element : Lists.reverse(block.elements())) {
      switch (element.kind()) {
        case ASSIGNMENT:
          processAssignment((AssignmentExpressionTree) element, blockKill, blockGen, assignmentLHS);
          break;
        case IDENTIFIER:
          processIdentifier((IdentifierTree) element, blockGen, assignmentLHS);
          break;
        case MEMBER_SELECT:
          processMemberSelect((MemberSelectExpressionTree) element, assignmentLHS, blockGen);
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

  private void processIdentifier(IdentifierTree element, Set<Symbol> blockGen, Set<Tree> assignmentLHS) {
    Symbol symbol = element.symbol();
    if (!assignmentLHS.contains(element) && includeSymbol(symbol)) {
      blockGen.add(symbol);
    }
  }

  private void processMemberSelect(MemberSelectExpressionTree element, Set<Tree> assignmentLHS, Set<Symbol> blockGen) {
    Symbol symbol;
    if (!assignmentLHS.contains(element) && includeFields) {
      symbol = getField(element);
      if (symbol != null) {
        blockGen.add(symbol);
      }
    }
  }

  private void processAssignment(AssignmentExpressionTree element, Set<Symbol> blockKill, Set<Symbol> blockGen, Set<Tree> assignmentLHS) {
    Symbol symbol = null;
    ExpressionTree lhs = element.variable();
    if (lhs.is(Kind.IDENTIFIER)) {
      symbol = ((IdentifierTree) lhs).symbol();

    } else if (includeFields && lhs.is(Kind.MEMBER_SELECT)) {
      symbol = getField((MemberSelectExpressionTree) lhs);
    }

    if (symbol != null && includeSymbol(symbol)) {
      assignmentLHS.add(lhs);
      blockGen.remove(symbol);
      blockKill.add(symbol);
    }
  }

  private boolean includeSymbol(Symbol symbol) {
    return isLocalVariable(symbol) || (includeFields && isField(symbol));
  }

  private static boolean isLocalVariable(Symbol symbol) {
    return symbol.owner().isMethodSymbol();
  }

  private static boolean isField(Symbol symbol) {
    return symbol.owner().isTypeSymbol() && !"this".equals(symbol.name()) && symbol.isVariableSymbol();
  }

  @CheckForNull
  private static Symbol getField(MemberSelectExpressionTree memberSelect) {
    Symbol symbol = memberSelect.identifier().symbol();

    if (memberSelect.expression().is(Kind.IDENTIFIER)) {
      String objectName = ((IdentifierTree) memberSelect.expression()).name();

      if (symbol.isStatic() || "this".equals(objectName)) {
        return symbol;
      }

    } else if (symbol.isStatic()) {
      return symbol;
    }

    return null;
  }

  private Set<Symbol> getUsedVariables(@Nullable Tree syntaxNode, Symbol.MethodSymbol owner) {
    if(syntaxNode == null) {
      return Collections.emptySet();
    }
    VariableReadExtractor extractorFromClass = new VariableReadExtractor(owner, includeFields);
    syntaxNode.accept(extractorFromClass);
    return extractorFromClass.usedVariables();
  }

}
