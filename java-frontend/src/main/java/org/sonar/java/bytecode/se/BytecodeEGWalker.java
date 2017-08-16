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
package org.sonar.java.bytecode.se;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.objectweb.asm.Opcodes;
import org.sonar.java.bytecode.cfg.BytecodeCFGBuilder;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.Pair;
import org.sonar.java.se.ProgramPoint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.semantic.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class BytecodeEGWalker {

  private static final int MAX_EXEC_PROGRAM_POINT = 2;
  private static final int MAX_STEPS = 16_000;
  private final BehaviorCache behaviorCache;

  private ExplodedGraph explodedGraph;

  @VisibleForTesting
  Deque<ExplodedGraph.Node> workList;
  ExplodedGraph.Node node;
  ProgramPoint programPosition;
  ProgramState programState;
  int steps;
  private Set<ExplodedGraph.Node> endOfExecutionPath;
  private ConstraintManager constraintManager;
  MethodBehavior methodBehavior;
  private CheckerDispatcher checkerDispatcher;

  BytecodeEGWalker(BehaviorCache behaviorCache){
    this.behaviorCache = behaviorCache;
    checkerDispatcher = new CheckerDispatcher(this, Lists.newArrayList(new BytecodeSECheck.NullnessCheck()));
    constraintManager = new ConstraintManager();
    explodedGraph = new ExplodedGraph();
    workList = new LinkedList<>();
    endOfExecutionPath = new LinkedHashSet<>();
  }

  public MethodBehavior getMethodBehavior(Symbol.MethodSymbol symbol, SquidClassLoader classLoader) {
    methodBehavior = behaviorCache.methodBehaviorForSymbol(symbol);
    if(!methodBehavior.isComplete()) {
      execute(symbol, classLoader);
      methodBehavior.completed();
    }
    return methodBehavior;
  }

  private void execute(Symbol.MethodSymbol symbol, SquidClassLoader classLoader) {
    programState = ProgramState.EMPTY_STATE;
    steps = 0;
    BytecodeCFGBuilder.BytecodeCFG bytecodeCFG = BytecodeCFGBuilder.buildCFG(symbol, classLoader);
    for (ProgramState startingState : startingStates(symbol, programState)) {
      enqueue(new ProgramPoint(bytecodeCFG.entry()), startingState);
    }
    while (!workList.isEmpty()) {
      steps++;
      if (steps > MAX_STEPS) {
        throw new IllegalStateException("Too many steps");
      }
      // LIFO:
      setNode(workList.removeFirst());
      if (programPosition.block.successors().isEmpty()) {
        endOfExecutionPath.add(node);
        continue;
      }

      if (programPosition.i < programPosition.block.elements().size()) {
        // process block element
        executeInstruction((BytecodeCFGBuilder.Instruction) programPosition.block.elements().get(programPosition.i));
      } else {
        // process block exit, which is unconditional jump such as goto-statement or return-statement
        handleBlockExit(programPosition);
      }
    }

    handleEndOfExecutionPath();
    executeCheckEndOfExecution();
    // Cleanup:
    workList = null;
    node = null;
    programState = null;
    constraintManager = null;
  }

  @VisibleForTesting
  void executeInstruction(BytecodeCFGBuilder.Instruction instruction) {
    if(!checkerDispatcher.executeCheckPreStatement(instruction)) {
      return;
    }
    switch (instruction.opcode) {
      case Opcodes.ARETURN:
        programState.storeExitValue();
        break;
      case Opcodes.ACONST_NULL:
        programState = programState.stackValue(SymbolicValue.NULL_LITERAL);
        break;
      case Opcodes.ALOAD:
      case Opcodes.DLOAD:
      case Opcodes.FLOAD:
      case Opcodes.ILOAD:
      case Opcodes.LLOAD:
        SymbolicValue value = programState.getValue(instruction.operand);
        Preconditions.checkNotNull(value, "Loading a symbolic value unindexed");
        programState = programState.stackValue(value);
        break;
      case Opcodes.AALOAD:
        break;
      case Opcodes.BALOAD:
      case Opcodes.CALOAD:
      case Opcodes.DALOAD:
      case Opcodes.FALOAD:
      case Opcodes.IALOAD:
      case Opcodes.LALOAD:
      case Opcodes.SALOAD:
        break;
      case Opcodes.LDC:
      case Opcodes.NEW:
        SymbolicValue symbolicValue = constraintManager.createSymbolicValue(instruction);
        programState = programState.stackValue(symbolicValue);
        programState = programState.addConstraint(symbolicValue, ObjectConstraint.NOT_NULL);
        break;
      default:
        // do nothing
    }
    checkerDispatcher.executeCheckPostStatement(instruction);
  }

  private void handleBlockExit(ProgramPoint programPosition) {
    BytecodeCFGBuilder.Block block = (BytecodeCFGBuilder.Block) programPosition.block;
    BytecodeCFGBuilder.Instruction terminator = block.terminator();
    if (terminator != null) {
      switch (terminator.opcode) {
        case IFEQ:
        case IFNE:
        case IFLT:
        case IFGE:
        case IFGT:
        case IFLE:

        case IF_ICMPEQ:
        case IF_ICMPNE:
        case IF_ICMPLT:
        case IF_ICMPGE:
        case IF_ICMPGT:
        case IF_ICMPLE:
        case IF_ACMPEQ:
        case IF_ACMPNE:

          break;
        case IFNULL:
        case IFNONNULL:
          ProgramState.Pop pop = programState.unstackValue(1);
          List<ProgramState.SymbolicValueSymbol> symbolicValueSymbols = new ArrayList<>(pop.valuesAndSymbols);
          symbolicValueSymbols.add(new ProgramState.SymbolicValueSymbol(SymbolicValue.NULL_LITERAL, null));
          programState = pop.state.stackValue(constraintManager.createBinarySymbolicValue(terminator, symbolicValueSymbols));
          break;
      }

      ProgramPoint falsePP = new ProgramPoint(((BytecodeCFGBuilder.Block) programPosition.block).falseSuccessor());
      ProgramPoint truePP = new ProgramPoint(((BytecodeCFGBuilder.Block) programPosition.block).trueSuccessor());
      Pair<List<ProgramState>, List<ProgramState>> pair = constraintManager.assumeDual(programState);
      pair.a.stream().forEach(s -> enqueue(falsePP, s));
      pair.b.stream().forEach(s -> enqueue(truePP, s));
    } else {
      //  Table switch and lookup
      programPosition.block.successors().forEach(b -> enqueue(new ProgramPoint(b), programState));
    }
  }

  private void executeCheckEndOfExecution() {
    // TODO callback to checks at end of execution
  }

  private Iterable<ProgramState> startingStates(Symbol.MethodSymbol symbol, ProgramState currentState) {
    // TODO : deal with parameter annotations, equals methods etc.
    int arity = symbol.parameterTypes().size();
    int startIndexParam = 0;
    ProgramState state = currentState;
    if(!symbol.isStatic()) {
      // Add a sv for "this"
      SymbolicValue thisSV = constraintManager.createSymbolicValue((BytecodeCFGBuilder.Instruction) null);
      methodBehavior.addParameter(thisSV);
      state = currentState.addConstraint(thisSV, ObjectConstraint.NOT_NULL).put(0, thisSV);
      startIndexParam = 1;
      arity += 1;
    }
    for (int i = startIndexParam; i < arity; i++) {
      SymbolicValue sv = constraintManager.createSymbolicValue((BytecodeCFGBuilder.Instruction) null);
      methodBehavior.addParameter(sv);
      state = state.put(i, sv);
    }
    return Collections.singletonList(state);
  }

  private void setNode(ExplodedGraph.Node node) {
    this.node = node;
    programPosition = this.node.programPoint;
    programState = this.node.programState;
  }

  void enqueue(ProgramPoint pp, ProgramState programState) {
    int nbOfExecution = programState.numberOfTimeVisited(pp);
    if (nbOfExecution > MAX_EXEC_PROGRAM_POINT) {
      return;
    }
    ProgramState ps = programState.visitedPoint(pp, nbOfExecution + 1);
    ExplodedGraph.Node cachedNode = explodedGraph.node(pp, ps);
    cachedNode.addParent(node, null);
    if (cachedNode.isNew) {
      workList.addFirst(cachedNode);
    }
  }

  private void handleEndOfExecutionPath() {
    ExplodedGraph.Node savedNode = node;
    endOfExecutionPath.forEach(n -> {
      setNode(n);
//      if (!programState.exitingOnRuntimeException()) {
//        checkerDispatcher.executeCheckEndOfExecutionPath(constraintManager);
//      }
      if (/*!interrupted && */methodBehavior != null) {
        methodBehavior.createYield(node);
      }
    });
    setNode(savedNode);
  }
}
