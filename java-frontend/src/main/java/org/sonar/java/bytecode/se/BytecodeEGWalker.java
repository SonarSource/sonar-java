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
import org.objectweb.asm.Opcodes;
import org.sonar.java.bytecode.cfg.BytecodeCFGBuilder;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramPoint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.semantic.Symbol;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

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

  BytecodeEGWalker(BehaviorCache behaviorCache){
    this.behaviorCache = behaviorCache;
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
    explodedGraph = new ExplodedGraph();
    constraintManager = new ConstraintManager();
    programState = ProgramState.EMPTY_STATE;
    workList = new LinkedList<>();
    endOfExecutionPath = new LinkedHashSet<>();
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

  private void executeInstruction(BytecodeCFGBuilder.Instruction instruction) {
    // TODO execute check prestatement
    switch (instruction.opcode) {
      case Opcodes.ACONST_NULL:
        programState = programState.stackValue(SymbolicValue.NULL_LITERAL);
        break;
      case Opcodes.ALOAD:
      case Opcodes.DLOAD:
      case Opcodes.FLOAD:
      case Opcodes.ILOAD:
        break;
      case Opcodes.LLOAD:
      case Opcodes.AALOAD:
      case Opcodes.BALOAD:
      case Opcodes.CALOAD:
      case Opcodes.DALOAD:
      case Opcodes.FALOAD:
      case Opcodes.IALOAD:
      case Opcodes.LALOAD:
      case Opcodes.SALOAD:
        break;
      default:
        // do nothing
    }
    // TODO execute check poststatement
    enqueue(programPosition.next(), programState);
  }

  private void handleBlockExit(ProgramPoint programPosition) {
    programPosition.block.successors().forEach(b -> enqueue(new ProgramPoint(b), programState));
  }

  private void executeCheckEndOfExecution() {
    // TODO callback to checks at end of execution
  }

  private Iterable<ProgramState> startingStates(Symbol.MethodSymbol symbol, ProgramState currentState) {
    // TODO : deal with parameter annotations, equals methods etc.
    return Collections.singletonList(currentState);
  }

  private void setNode(ExplodedGraph.Node node) {
    this.node = node;
    programPosition = this.node.programPoint;
    programState = this.node.programState;
  }

  private void enqueue(ProgramPoint pp, ProgramState programState) {
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
