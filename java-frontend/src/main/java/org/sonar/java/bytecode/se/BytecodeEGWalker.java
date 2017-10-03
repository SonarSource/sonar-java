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
import org.objectweb.asm.Type;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
  private BytecodeCFGBuilder.Block exitBlock;

  public BytecodeEGWalker(BehaviorCache behaviorCache){
    this.behaviorCache = behaviorCache;
    checkerDispatcher = new CheckerDispatcher(this, Lists.newArrayList(new BytecodeSECheck.NullnessCheck()));
    constraintManager = new ConstraintManager();
    explodedGraph = new ExplodedGraph();
    workList = new LinkedList<>();
    endOfExecutionPath = new LinkedHashSet<>();
  }

  public MethodBehavior getMethodBehavior(String signature, SquidClassLoader classLoader) {
    methodBehavior = behaviorCache.methodBehaviorForSymbol(signature);
    if(!methodBehavior.isComplete()) {
      execute(signature, classLoader);
      methodBehavior.completed();
    }
    return methodBehavior;
  }

  private void execute(String signature, SquidClassLoader classLoader) {
    programState = ProgramState.EMPTY_STATE;
    steps = 0;
    BytecodeCFGBuilder.BytecodeCFG bytecodeCFG = BytecodeCFGBuilder.buildCFG(signature, classLoader);
    exitBlock = bytecodeCFG.exitBlock();
    methodBehavior.setStaticMethod(bytecodeCFG.isStaticMethod());
    methodBehavior.setVarArgs(bytecodeCFG.isVarArgs());
    for (ProgramState startingState : startingStates(signature, programState)) {
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
    SymbolicValue sv;
    switch (instruction.opcode) {
      case NOP:
        break;
      case ACONST_NULL:
        programState = programState.stackValue(SymbolicValue.NULL_LITERAL);
        break;
      case ICONST_M1:
      case ICONST_0:
      case ICONST_1:
      case ICONST_2:
      case ICONST_3:
      case ICONST_4:
      case ICONST_5:
      case LCONST_0:
      case LCONST_1:
      case FCONST_0:
      case FCONST_1:
      case FCONST_2:
      case DCONST_0:
      case DCONST_1:
        sv = constraintManager.createSymbolicValue(instruction);
        programState = programState.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        if (instruction.opcode == ICONST_1 || instruction.opcode == LCONST_1 || instruction.opcode == FCONST_1 || instruction.opcode == DCONST_1) {
          programState = programState.addConstraint(sv, BooleanConstraint.TRUE);
        }
        if (instruction.opcode == ICONST_0 || instruction.opcode == LCONST_0 || instruction.opcode == FCONST_0  || instruction.opcode == DCONST_0  ) {
          programState = programState.addConstraint(sv, BooleanConstraint.FALSE).addConstraint(sv, DivisionByZeroCheck.ZeroConstraint.ZERO);
        } else {
          programState = programState.addConstraint(sv, DivisionByZeroCheck.ZeroConstraint.NON_ZERO);
        }
        break;
      case BIPUSH:
      case SIPUSH:
        sv = constraintManager.createSymbolicValue(instruction);
        programState = programState.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL).addConstraint(sv,DivisionByZeroCheck.ZeroConstraint.NON_ZERO);
        if(instruction.operand == 0) {
          programState = programState.addConstraint(sv, BooleanConstraint.FALSE).addConstraint(sv, DivisionByZeroCheck.ZeroConstraint.ZERO);
        } else if (instruction.operand == 1) {
          programState = programState.addConstraint(sv, BooleanConstraint.TRUE);
        }
        break;
      case LDC:
        createNonNullValue(instruction);
        break;
      case ARETURN:
      case IRETURN:
        programState.storeExitValue();
        break;
      case ATHROW:
        pop = programState.unstackValue(1);
        programState = pop.state.stackValue(constraintManager.createExceptionalSymbolicValue(null));
        programState.storeExitValue();
        break;
      case ILOAD:
      case LLOAD:
      case FLOAD:
      case DLOAD:
      case ALOAD:
        SymbolicValue value = programState.getValue(instruction.operand);
        Preconditions.checkNotNull(value, "Loading a symbolic value unindexed");
        programState = programState.stackValue(value);
        break;
      case IALOAD:
      case LALOAD:
      case FALOAD:
      case DALOAD:
      case AALOAD:
      case BALOAD:
      case CALOAD:
      case SALOAD:
        sv = constraintManager.createSymbolicValue(instruction);
        programState = programState.unstackValue(2).state.stackValue(sv);
        if (instruction.opcode != AALOAD) {
          programState = programState.addConstraint(sv, ObjectConstraint.NOT_NULL);
        }
        break;
      case ISTORE:
      case LSTORE:
      case FSTORE:
      case DSTORE:
      case ASTORE:
        pop = programState.unstackValue(1);
        programState = pop.state.put(instruction.operand, pop.values.get(0));
        break;
      case IASTORE:
      case LASTORE:
      case FASTORE:
      case DASTORE:
      case AASTORE:
      case BASTORE:
      case CASTORE:
      case SASTORE:
        programState = programState.unstackValue(3).state;
        break;
      case POP:
        programState = programState.unstackValue(1).state;
        break;
      case POP2:
        programState = programState.unstackValue(2).state;
        break;
      case DUP:
        sv = programState.peekValue();
        Preconditions.checkNotNull(sv, "DUP on empty stack");
        programState = programState.stackValue(sv);
        break;
      case DUP_X1:
        pop = programState.unstackValue(2);
        Preconditions.checkState(pop.values.size() == 2, "DUP_X1 needs 2 values on stack");
        programState = pop.state.stackValue(pop.values.get(0)).stackValue(pop.values.get(1)).stackValue(pop.values.get(0));
        break;
      case DUP_X2:
        pop = programState.unstackValue(3);
        Preconditions.checkState(pop.values.size() == 3, "DUP_X2 needs 3 values on stack");
        programState = pop.state.stackValue(pop.values.get(0)).stackValue(pop.values.get(2)).stackValue(pop.values.get(1)).stackValue(pop.values.get(0));
        break;
      case DUP2:
        pop = programState.unstackValue(2);
        Preconditions.checkState(pop.values.size() == 2, "DUP2 needs 2 values on stack");
        programState = pop.state.stackValue(pop.values.get(1)).stackValue(pop.values.get(0)).stackValue(pop.values.get(1)).stackValue(pop.values.get(0));
        break;
      case DUP2_X1:
        pop = programState.unstackValue(3);
        Preconditions.checkState(pop.values.size() == 3, "DUP2_X1 needs 3 values on stack");
        programState = pop.state
          .stackValue(pop.values.get(1))
          .stackValue(pop.values.get(0))
          .stackValue(pop.values.get(2))
          .stackValue(pop.values.get(1))
          .stackValue(pop.values.get(0));
        break;
      case DUP2_X2:
        pop = programState.unstackValue(4);
        Preconditions.checkState(pop.values.size() == 4, "DUP2_X2 needs 4 values on stack");
        programState = pop.state
          .stackValue(pop.values.get(1))
          .stackValue(pop.values.get(0))
          .stackValue(pop.values.get(3))
          .stackValue(pop.values.get(2))
          .stackValue(pop.values.get(1))
          .stackValue(pop.values.get(0));
        break;
      case SWAP:
        pop = programState.unstackValue(2);
        Preconditions.checkState(pop.values.size() == 2, "SWAP needs 2 values on stack");
        programState = pop.state.stackValue(pop.values.get(0)).stackValue(pop.values.get(1));
        break;
      case IADD:
      case LADD:
      case FADD:
      case DADD:
      case ISUB:
      case LSUB:
      case FSUB:
      case DSUB:
      case IMUL:
      case LMUL:
      case FMUL:
      case DMUL:
      case IDIV:
      case LDIV:
      case FDIV:
      case DDIV:
      case IREM:
      case LREM:
      case FREM:
      case DREM:
        handleArithmetic(instruction);
        break;
      case INEG:
      case LNEG:
      case FNEG:
      case DNEG:
        sv = constraintManager.createSymbolicValue(instruction);
        pop = programState.unstackValue(1);
        Preconditions.checkState(pop.values.size() == 1, "NEG needs value on stack");
        programState = pop.state.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        break;
      case ISHL:
      case LSHL:
      case ISHR:
      case LSHR:
      case IUSHR:
      case LUSHR:
        handleArithmetic(instruction);
        break;
      case IAND:
      case LAND:
      case IOR:
      case LOR:
      case IXOR:
      case LXOR:
        pop = programState.unstackValue(2);
        Preconditions.checkState(pop.values.size() == 2, "Bitwise instruction needs 2 values on stack");
        sv = constraintManager.createBinarySymbolicValue(instruction, pop.valuesAndSymbols);
        programState = pop.state.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        break;
      case IINC:
        int index = instruction.operand;
        SymbolicValue existing = programState.getValue(index);
        Preconditions.checkNotNull(existing, "Local variable " + index + " not found");
        sv = constraintManager.createSymbolicValue(instruction);
        programState = programState.put(index, sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        break;
      case I2L:
      case I2F:
      case I2D:
      case L2I:
      case L2F:
      case L2D:
      case F2I:
      case F2L:
      case F2D:
      case D2I:
      case D2L:
      case D2F:
      case I2B:
      case I2C:
      case I2S:
        // do nothing
        break;
      case LCMP:
      case FCMPL:
      case FCMPG:
      case DCMPL:
      case DCMPG:
        sv = constraintManager.createSymbolicValue(instruction);
        pop = programState.unstackValue(2);
        Preconditions.checkState(pop.values.size() == 2, "CMP needs 2 values on stack");
        programState = pop.state.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        break;
      case NEW:
        createNonNullValue(instruction);
        break;
      case INVOKESPECIAL:
      case INVOKESTATIC:
      case INVOKEVIRTUAL:
      case INVOKEINTERFACE:
        boolean isStatic = instruction.opcode == Opcodes.INVOKESTATIC;
        int arity = isStatic ? instruction.arity() : (instruction.arity() + 1);
        pop = programState.unstackValue(arity);
        Preconditions.checkState(pop.values.size() == arity, "Arguments mismatch for INVOKE");
        // TODO use constraintManager.createMethodSymbolicValue to create relational SV for equals
        SymbolicValue returnSV = constraintManager.createSymbolicValue(instruction);
        if (isStatic) {
          // follow only static invocations for now.
          String signature = instruction.fieldOrMethod.completeSignature();
          MethodBehavior methodInvokedBehavior = behaviorCache.get(signature);
          if (methodInvokedBehavior != null && methodInvokedBehavior.isComplete()) {
            methodInvokedBehavior
              .happyPathYields()
              .forEach(yield ->
                yield.statesAfterInvocation(Lists.reverse(pop.values), Collections.emptyList(), pop.state, () -> returnSV).forEach(ps -> {
                  checkerDispatcher.methodYield = yield;
                  checkerDispatcher.addTransition(ps);
                  checkerDispatcher.methodYield = null;
                }));
            methodInvokedBehavior
              .exceptionalPathYields()
              .forEach(yield ->
                yield.statesAfterInvocation(
                  Lists.reverse(pop.values), Collections.emptyList(), pop.state, () -> constraintManager.createExceptionalSymbolicValue(yield.exceptionType())).forEach(ps -> {
                  ps.storeExitValue();
                  enqueue(new ProgramPoint(exitBlock), ps);
                }));
            return;
          }
        }
        if (instruction.hasReturnValue()) {
          programState = pop.state;
        } else {
          programState = pop.state.stackValue(returnSV);
        }
        break;
      default:
        // do nothing
    }
    checkerDispatcher.executeCheckPostStatement(instruction);
  }

  private void createNonNullValue(BytecodeCFGBuilder.Instruction instruction) {
    SymbolicValue sv = constraintManager.createSymbolicValue(instruction);
    programState = programState.stackValue(sv);
    programState = programState.addConstraint(sv, ObjectConstraint.NOT_NULL);
  }

  private void handleArithmetic(BytecodeCFGBuilder.Instruction instruction) {
    SymbolicValue sv = constraintManager.createSymbolicValue(instruction);
    ProgramState.Pop pop = programState.unstackValue(2);
    Preconditions.checkState(pop.values.size() == 2, "Arithmetic instruction needs 2 values on stack");
    programState = pop.state.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
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
      // TODO : filter some node of the EG depending of the exceptionType in the successor.
      programPosition.block.successors().forEach(b -> enqueue(new ProgramPoint(b), programState));
    }
  }

  private void executeCheckEndOfExecution() {
    // TODO callback to checks at end of execution
  }

  @VisibleForTesting
  Iterable<ProgramState> startingStates(String signature, ProgramState currentState) {
    // TODO : deal with parameter annotations, equals methods etc.
    int parameterIdx = 0;
    ProgramState state = currentState;
    if(!methodBehavior.isStaticMethod()) {
      // Add a sv for "this"
      SymbolicValue thisSV = constraintManager.createSymbolicValue((BytecodeCFGBuilder.Instruction) null);
      methodBehavior.addParameter(thisSV);
      state = currentState.addConstraint(thisSV, ObjectConstraint.NOT_NULL).put(0, thisSV);
      parameterIdx = 1;
    }
    Type[] argumentTypes = Type.getArgumentTypes(signature.substring(signature.indexOf('(')));
    for (Type argumentType: argumentTypes) {
      SymbolicValue sv = constraintManager.createSymbolicValue((BytecodeCFGBuilder.Instruction) null);
      methodBehavior.addParameter(sv);
      state = state.put(parameterIdx, sv);
      parameterIdx += argumentType.getSize();
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
    if (cachedNode.isNew()) {
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
        methodBehavior.createYield(node, false);
      }
    });
    setNode(savedNode);
  }
}
