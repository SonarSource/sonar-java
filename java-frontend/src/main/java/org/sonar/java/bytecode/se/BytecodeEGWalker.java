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
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
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
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ACMPEQ;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.JSR;

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
    ProgramState.Pop pop;
    switch (instruction.opcode) {
      case ICONST_0:
        SymbolicValue svZero = constraintManager.createSymbolicValue(instruction);
        programState = programState.stackValue(svZero).addConstraint(svZero, DivisionByZeroCheck.ZeroConstraint.ZERO).addConstraint(svZero, BooleanConstraint.FALSE);
        break;
      case ICONST_M1:
      case ICONST_1:
      case ICONST_2:
      case ICONST_3:
      case ICONST_4:
      case ICONST_5:
        SymbolicValue svNonZero = constraintManager.createSymbolicValue(instruction);
        programState = programState.stackValue(svNonZero).addConstraint(svNonZero, DivisionByZeroCheck.ZeroConstraint.NON_ZERO);
        if (instruction.opcode == ICONST_1) {
          programState = programState.addConstraint(svNonZero, BooleanConstraint.TRUE);
        }
        break;
      case Opcodes.ARETURN:
        programState.storeExitValue();
        break;
      case Opcodes.ATHROW:
        pop = programState.unstackValue(1);
        programState = pop.state.stackValue(constraintManager.createExceptionalSymbolicValue(null));
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
      case Opcodes.NEW: {
        SymbolicValue symbolicValue = constraintManager.createSymbolicValue(instruction);
        programState = programState.stackValue(symbolicValue);
        programState = programState.addConstraint(symbolicValue, ObjectConstraint.NOT_NULL);
        break;
      }
      case Opcodes.DUP: {
        SymbolicValue symbolicValue = programState.peekValue();
        Preconditions.checkNotNull(symbolicValue, "DUP on empty stack");
        programState = programState.stackValue(symbolicValue);
        break;
      }
      case Opcodes.INVOKESPECIAL:
      case Opcodes.INVOKESTATIC:
      case Opcodes.INVOKEVIRTUAL:
      case Opcodes.INVOKEINTERFACE:
        org.objectweb.asm.Type methodType = org.objectweb.asm.Type.getMethodType(instruction.fieldOrMethod.desc);
        boolean isStatic = instruction.opcode == Opcodes.INVOKESTATIC;
        int arity = isStatic ? methodType.getArgumentTypes().length : (methodType.getArgumentTypes().length + 1);
        pop = programState.unstackValue(arity);
        Preconditions.checkState(pop.values.size() == arity, "Arguments mismatch for INVOKE");
        // TODO resolve method and retrieve behavior
        if (methodType.getReturnType() == org.objectweb.asm.Type.VOID_TYPE) {
          programState = pop.state;
        } else {
          // TODO use constraintManager.createMethodSymbolicValue to create relational SV for equals
          SymbolicValue returnSV = constraintManager.createSymbolicValue(instruction);
          programState = pop.state.stackValue(returnSV);
        }
        break;
      default:
        // do nothing
    }
    checkerDispatcher.executeCheckPostStatement(instruction);
  }

  private void handleBlockExit(ProgramPoint programPosition) {
    BytecodeCFGBuilder.Block block = (BytecodeCFGBuilder.Block) programPosition.block;
    BytecodeCFGBuilder.Instruction terminator = block.terminator();
    ProgramState.Pop pop;
    ProgramState ps;
    List<ProgramState.SymbolicValueSymbol> symbolicValueSymbols;
    if (terminator != null) {
      switch (terminator.opcode) {
        case GOTO:
        case JSR:
          programPosition.block.successors().forEach(b -> enqueue(new ProgramPoint(b), programState));
          return;
        case IFEQ:
        case IFNE:
        case IFLT:
        case IFGE:
        case IFGT:
        case IFLE:
          pop = programState.unstackValue(1);
          symbolicValueSymbols = new ArrayList<>(pop.valuesAndSymbols);
          SymbolicValue svZero = new SymbolicValue();
          symbolicValueSymbols.add(new ProgramState.SymbolicValueSymbol(svZero, null));
          List<ProgramState> programStates = svZero.setConstraint(pop.state, DivisionByZeroCheck.ZeroConstraint.ZERO).stream()
            .flatMap(s -> svZero.setConstraint(s, BooleanConstraint.FALSE).stream()).collect(Collectors.toList());
          Preconditions.checkState(programStates.size() == 1);
          ps = programStates.get(0);
          break;
        case IF_ICMPEQ:
        case IF_ICMPNE:
        case IF_ICMPLT:
        case IF_ICMPGE:
        case IF_ICMPGT:
        case IF_ICMPLE:
        case IF_ACMPEQ:
        case IF_ACMPNE:
          pop = programState.unstackValue(2);
          symbolicValueSymbols = pop.valuesAndSymbols;
          ps = pop.state;
          break;
        case IFNULL:
        case IFNONNULL:
          pop = programState.unstackValue(1);
          symbolicValueSymbols = new ArrayList<>(pop.valuesAndSymbols);
          symbolicValueSymbols.add(new ProgramState.SymbolicValueSymbol(SymbolicValue.NULL_LITERAL, null));
          ps = pop.state;
          break;
        default:
          throw new IllegalStateException("Unexpected terminator " + terminator.opcode);
      }
      programState = ps.stackValue(constraintManager.createBinarySymbolicValue(terminator, symbolicValueSymbols));
      Pair<List<ProgramState>, List<ProgramState>> pair = constraintManager.assumeDual(programState);
      ProgramPoint falsePP = new ProgramPoint(((BytecodeCFGBuilder.Block) programPosition.block).falseSuccessor());
      ProgramPoint truePP = new ProgramPoint(((BytecodeCFGBuilder.Block) programPosition.block).trueSuccessor());
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
