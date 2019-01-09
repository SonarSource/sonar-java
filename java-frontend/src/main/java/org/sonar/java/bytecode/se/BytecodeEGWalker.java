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
package org.sonar.java.bytecode.se;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Printer;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.bytecode.cfg.BytecodeCFG;
import org.sonar.java.bytecode.cfg.BytecodeCFGMethodVisitor;
import org.sonar.java.bytecode.cfg.Instruction;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbols;
import org.sonar.java.se.ExceptionUtils;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ExplodedGraphWalker;
import org.sonar.java.se.Pair;
import org.sonar.java.se.ProgramPoint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.constraint.TypedConstraint;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.semantic.Type;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BALOAD;
import static org.objectweb.asm.Opcodes.BASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CALOAD;
import static org.objectweb.asm.Opcodes.CASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.D2F;
import static org.objectweb.asm.Opcodes.D2I;
import static org.objectweb.asm.Opcodes.D2L;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DASTORE;
import static org.objectweb.asm.Opcodes.DCMPG;
import static org.objectweb.asm.Opcodes.DCMPL;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DNEG;
import static org.objectweb.asm.Opcodes.DREM;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.DUP2_X1;
import static org.objectweb.asm.Opcodes.DUP2_X2;
import static org.objectweb.asm.Opcodes.DUP_X1;
import static org.objectweb.asm.Opcodes.DUP_X2;
import static org.objectweb.asm.Opcodes.F2D;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.F2L;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FALOAD;
import static org.objectweb.asm.Opcodes.FASTORE;
import static org.objectweb.asm.Opcodes.FCMPG;
import static org.objectweb.asm.Opcodes.FCMPL;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.FREM;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.I2B;
import static org.objectweb.asm.Opcodes.I2C;
import static org.objectweb.asm.Opcodes.I2D;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.I2S;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.IAND;
import static org.objectweb.asm.Opcodes.IASTORE;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IDIV;
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
import static org.objectweb.asm.Opcodes.IINC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INEG;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEDYNAMIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IOR;
import static org.objectweb.asm.Opcodes.IREM;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISHL;
import static org.objectweb.asm.Opcodes.ISHR;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.IUSHR;
import static org.objectweb.asm.Opcodes.IXOR;
import static org.objectweb.asm.Opcodes.L2D;
import static org.objectweb.asm.Opcodes.L2F;
import static org.objectweb.asm.Opcodes.L2I;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LALOAD;
import static org.objectweb.asm.Opcodes.LAND;
import static org.objectweb.asm.Opcodes.LASTORE;
import static org.objectweb.asm.Opcodes.LCMP;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.LDIV;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LNEG;
import static org.objectweb.asm.Opcodes.LOOKUPSWITCH;
import static org.objectweb.asm.Opcodes.LOR;
import static org.objectweb.asm.Opcodes.LREM;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.LSHL;
import static org.objectweb.asm.Opcodes.LSHR;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.LUSHR;
import static org.objectweb.asm.Opcodes.LXOR;
import static org.objectweb.asm.Opcodes.MONITORENTER;
import static org.objectweb.asm.Opcodes.MONITOREXIT;
import static org.objectweb.asm.Opcodes.MULTIANEWARRAY;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.NEWARRAY;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SALOAD;
import static org.objectweb.asm.Opcodes.SASTORE;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.SWAP;
import static org.objectweb.asm.Opcodes.TABLESWITCH;
import static org.sonar.java.bytecode.se.BytecodeEGWalker.StackValueCategoryConstraint.LONG_OR_DOUBLE;

public class BytecodeEGWalker {

  private static final Logger LOG = Loggers.get(BytecodeEGWalker.class);
  private static final int MAX_EXEC_PROGRAM_POINT = 2;
  private static final int MAX_STEPS = 16_000;

  private final BehaviorCache behaviorCache;
  private final SemanticModel semanticModel;

  @VisibleForTesting
  ExplodedGraph explodedGraph;
  private BytecodeCFG.Block exitBlock;

  /**
   * Because some instructions manipulate stack differently depending on the type of the value, we need this constraint to know category of the value
   * see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-2.html#jvms-2.11.1 Table 2.11.1-B
   */
  public enum StackValueCategoryConstraint implements Constraint {
    LONG_OR_DOUBLE;

    @Override
    public String valueAsString() {
      return toString();
    }

    @Nullable
    @Override
    public Constraint copyOver(RelationalSymbolicValue.Kind kind) {
      // don't copy this constraint over any relation
      return null;
    }
  }

  static class BytecodeAnalysisException extends RuntimeException {

    public BytecodeAnalysisException(String message, Throwable cause) {
      super(message, cause);
    }
  }

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

  public BytecodeEGWalker(BehaviorCache behaviorCache, SemanticModel semanticModel){
    this.behaviorCache = behaviorCache;
    this.semanticModel = semanticModel;
    checkerDispatcher = new CheckerDispatcher(this, Lists.newArrayList(
      new BytecodeSECheck.NullnessCheck(),
      new BytecodeSECheck.ZeronessCheck()));
    constraintManager = new ConstraintManager();
    explodedGraph = new ExplodedGraph();
    workList = new LinkedList<>();
    endOfExecutionPath = new LinkedHashSet<>();
  }

  @CheckForNull
  public MethodBehavior getMethodBehavior(String signature, SquidClassLoader classLoader) {
    if (methodFromArray(signature)) {
      // should not generate any method behavior
      return null;
    }
    methodBehavior = behaviorCache.methodBehaviorForSymbol(signature);
    if (!methodBehavior.isVisited()) {
      try {
        methodBehavior.visited();
        execute(signature, classLoader);
      } catch (ExplodedGraphWalker.MaximumStepsReachedException
        | RelationalSymbolicValue.TransitiveRelationExceededException
        | BytecodeAnalysisException e) {
        LOG.debug("Dataflow analysis is incomplete for method {} : {}", signature, e.getMessage());
      } catch (Exception e) {
        throw new BytecodeAnalysisException("Failed dataflow analysis for " + signature, e);
      }
    }
    return methodBehavior;
  }

  private static boolean methodFromArray(String signature) {
    return signature.substring(0, signature.indexOf('#')).endsWith("[]");
  }

  @VisibleForTesting
  int maxSteps() {
    return MAX_STEPS;
  }

  private void execute(String signature, SquidClassLoader classLoader) {
    BytecodeCFGMethodVisitor cfgVisitor = new BytecodeCFGMethodVisitor();
    MethodLookup lookup = MethodLookup.lookup(signature, classLoader, cfgVisitor);
    if (lookup == null) {
      LOG.debug("Method body not found: {}", signature);
      return;
    }
    methodBehavior.setDeclaredExceptions(lookup.declaredExceptions);
    methodBehavior.setVarArgs(lookup.isVarArgs);
    BytecodeCFG bytecodeCFG = cfgVisitor.getCfg();
    if (bytecodeCFG == null) {
      return;
    }
    exitBlock = bytecodeCFG.exitBlock();
    steps = 0;
    for (ProgramState startingState : startingStates(signature, ProgramState.EMPTY_STATE, lookup.isStatic)) {
      enqueue(new ProgramPoint(bytecodeCFG.entry()), startingState);
    }
    while (!workList.isEmpty()) {
      steps++;
      if (steps > maxSteps()) {
        throw new ExplodedGraphWalker.MaximumStepsReachedException("Too many steps resolving "+methodBehavior.signature());
      }
      // LIFO:
      setNode(workList.removeFirst());
      if (programPosition.block.successors().isEmpty()) {
        endOfExecutionPath.add(node);
        continue;
      }

      if (programPosition.i < programPosition.block.elements().size()) {
        // process block element
        executeInstruction((Instruction) programPosition.block.elements().get(programPosition.i));
      } else {
        // process block exit, which is unconditional jump such as goto-statement or return-statement
        handleBlockExit(programPosition);
      }
    }

    handleEndOfExecutionPath();
    executeCheckEndOfExecution();
    methodBehavior.completed();
    // Cleanup:
    workList = null;
    node = null;
    programState = null;
    constraintManager = null;
  }

  @VisibleForTesting
  void executeInstruction(Instruction instruction) {
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
        programState = setDoubleOrLong(sv, instruction.isLongOrDoubleValue());
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
        sv = constraintManager.createSymbolicValue(instruction);
        programState = programState.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        programState = setDoubleOrLong(sv, instruction.isLongOrDoubleValue());
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
        programState = setDoubleOrLong(sv, instruction.isLongOrDoubleValue());
        break;
      case ISTORE:
      case LSTORE:
      case FSTORE:
      case DSTORE:
      case ASTORE:
        pop = popStack(1, instruction.opcode);
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
        sv = programState.peekValue();
        Preconditions.checkNotNull(sv, "POP2 on empty stack");
        pop = isDoubleOrLong(sv) ? popStack(1, instruction.opcode) : popStack(2, instruction.opcode);
        programState = pop.state;
        break;
      case DUP:
        sv = programState.peekValue();
        Preconditions.checkNotNull(sv, "DUP on empty stack");
        programState = programState.stackValue(sv);
        break;
      case DUP_X1:
        pop = popStack(2, instruction.opcode);
        programState = stackValues(pop, 0, 1, 0);
        break;
      case DUP_X2:
        sv = programState.peekValue(1);
        if (isDoubleOrLong(sv)) {
          pop = popStack(2, instruction.opcode);
          programState = stackValues(pop, 0, 1, 0);
        } else {
          pop = popStack(3, instruction.opcode);
          programState = stackValues(pop, 0, 2, 1, 0);
        }
        break;
      case DUP2:
        sv = programState.peekValue();
        Preconditions.checkNotNull(sv, "DUP2 needs at least 1 value on stack");
        if (isDoubleOrLong(sv)) {
          pop = popStack(1, instruction.opcode);
          programState = stackValues(pop, 0, 0);
        } else {
          pop = popStack(2, instruction.opcode);
          programState = stackValues(pop, 1, 0, 1, 0);
        }
        break;
      case DUP2_X1:
        sv = programState.peekValue();
        Preconditions.checkNotNull(sv, "DUP2_X1 needs at least 1 value on stack");
        if (isDoubleOrLong(sv)) {
          pop = popStack(2, instruction.opcode);
          programState = stackValues(pop, 0, 1, 0);
        } else {
          pop = popStack(3, instruction.opcode);
          programState = stackValues(pop, 1, 0, 2, 1, 0);
        }
        break;
      case DUP2_X2:
        if (isDoubleOrLong(programState.peekValue()) && isDoubleOrLong(programState.peekValue(1))) {
          pop = popStack(2, instruction.opcode);
          programState = stackValues(pop, 0, 1, 0);
        } else if (isDoubleOrLong(programState.peekValue(2))) {
          pop = popStack(3, instruction.opcode);
          programState = stackValues(pop, 1, 0, 2, 1, 0);
        } else if (isDoubleOrLong(programState.peekValue())) {
          pop = popStack(3, instruction.opcode);
          programState = stackValues(pop, 0, 2, 1, 0);
        } else {
          pop = popStack(4, instruction.opcode);
          programState = stackValues(pop, 1, 0, 3, 2, 1, 0);
        }
        break;
      case SWAP:
        pop = popStack(2, instruction.opcode);
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
      case ISHL:
      case LSHL:
      case ISHR:
      case LSHR:
      case IUSHR:
      case LUSHR:
        pop = popStack(2, instruction.opcode);
        sv = constraintManager.createSymbolicValue(instruction);
        programState = pop.state.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        programState = setDoubleOrLong(sv, instruction.isLongOrDoubleValue());
        break;
      case INEG:
      case LNEG:
      case FNEG:
      case DNEG:
        pop = popStack(1, instruction.opcode);
        sv = constraintManager.createSymbolicValue(instruction);
        programState = pop.state.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        programState = setDoubleOrLong(sv, instruction.isLongOrDoubleValue());
        break;
      case IAND:
      case LAND:
      case IOR:
      case LOR:
      case IXOR:
      case LXOR:
        pop = popStack(2, instruction.opcode);
        sv = constraintManager.createBinarySymbolicValue(instruction, pop.valuesAndSymbols);
        programState = pop.state.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        programState = setDoubleOrLong(sv, instruction.isLongOrDoubleValue());
        break;
      case IINC:
        int index = instruction.operand;
        SymbolicValue existing = programState.getValue(index);
        Preconditions.checkNotNull(existing, "Local variable " + index + " not found");
        sv = constraintManager.createSymbolicValue(instruction);
        programState = programState.put(index, sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        break;
      case I2L:
      case I2D:
      case F2L:
      case F2D:
        sv = programState.peekValue();
        Preconditions.checkNotNull(sv, "%s needs value on stack", instruction);
        programState = setDoubleOrLong(sv, true);
        break;
      case L2I:
      case L2F:
      case D2I:
      case D2F:
        sv = programState.peekValue();
        Preconditions.checkNotNull(sv, "%s needs value on stack", instruction);
        programState = setDoubleOrLong(sv, false);
        break;
      case D2L:
      case I2F:
      case L2D:
      case F2I:
      case I2B:
      case I2C:
      case I2S:
        break;
      case LCMP:
      case FCMPL:
      case FCMPG:
      case DCMPL:
      case DCMPG:
        pop = popStack(2, instruction.opcode);
        sv = constraintManager.createSymbolicValue(instruction);
        programState = pop.state.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        break;
      case IRETURN:
      case LRETURN:
      case FRETURN:
      case DRETURN:
      case ARETURN:
        programState.storeExitValue();
        programState = programState.unstackValue(1).state;
        break;
      case RETURN:
        // do nothing
        break;
      case GETSTATIC:
        // TODO SONARJAVA-2510 associated symbolic value with symbol
        sv = constraintManager.createSymbolicValue(instruction);
        programState = programState.stackValue(sv);
        programState = setDoubleOrLong(sv, instruction.isLongOrDoubleValue());
        break;
      case PUTSTATIC:
        pop = programState.unstackValue(1);
        programState = pop.state;
        break;
      case GETFIELD:
        pop = popStack(1, instruction.opcode);
        sv = constraintManager.createSymbolicValue(instruction);
        programState = pop.state.stackValue(sv);
        programState = setDoubleOrLong(sv, instruction.isLongOrDoubleValue());
        break;
      case PUTFIELD:
        pop = popStack(2, instruction.opcode);
        programState = pop.state;
        break;
      case INVOKEVIRTUAL:
      case INVOKESPECIAL:
      case INVOKESTATIC:
      case INVOKEINTERFACE:
        if (handleMethodInvocation(instruction)) {
          // when yields are available, do not execute post check on this node
          return;
        }
        break;
      case INVOKEDYNAMIC:
        pop = popStack(instruction.arity(), instruction.opcode);
        Preconditions.checkState(instruction.hasReturnValue(), "Lambda should always evaluate to target functional interface");
        SymbolicValue lambdaTargetInterface = new SymbolicValue();
        programState = pop.state.stackValue(lambdaTargetInterface).addConstraint(lambdaTargetInterface, ObjectConstraint.NOT_NULL);
        break;
      case NEW:
        sv = constraintManager.createSymbolicValue(instruction);
        programState = programState
            .stackValue(sv)
            .addConstraint(sv, ObjectConstraint.NOT_NULL)
            .addConstraint(sv, new TypedConstraint(instruction.className));
        break;
      case ARRAYLENGTH:
        pop = popStack(1, instruction.opcode);
        sv = constraintManager.createSymbolicValue(instruction);
        programState = pop.state.stackValue(sv);
        break;
      case NEWARRAY:
      case ANEWARRAY:
        pop = popStack(1, instruction.opcode);
        sv = constraintManager.createSymbolicValue(instruction);
        programState = pop.state.stackValue(sv).addConstraint(sv, ObjectConstraint.NOT_NULL);
        break;
      case ATHROW:
        if (!(programState.peekValue() instanceof SymbolicValue.ExceptionalSymbolicValue)) {
          // create exceptional SV if not already on top of the stack (e.g. throw new MyException(); )
          pop = popStack(1, instruction.opcode);
          sv = pop.values.get(0);
          TypedConstraint typedConstraint = programState.getConstraint(sv, TypedConstraint.class);
          Type type = typedConstraint != null ? typedConstraint.getType(semanticModel) : Symbols.unknownType;
          programState = pop.state.stackValue(constraintManager.createExceptionalSymbolicValue(type));
        }
        programState.storeExitValue();
        break;
      case CHECKCAST:
        Preconditions.checkState(programState.peekValue() != null, "CHECKCAST needs 1 value on stack");
        break;
      case INSTANCEOF:
        pop = popStack(1, instruction.opcode);
        SymbolicValue.InstanceOfSymbolicValue instanceOf = new SymbolicValue.InstanceOfSymbolicValue();
        instanceOf.computedFrom(pop.valuesAndSymbols);
        programState = pop.state.stackValue(instanceOf);
        break;
      case MONITORENTER:
      case MONITOREXIT:
        pop = popStack(1, instruction.opcode);
        programState = pop.state;
        break;
      case MULTIANEWARRAY:
        Instruction.MultiANewArrayInsn multiANewArrayInsn = (Instruction.MultiANewArrayInsn) instruction;
        pop = popStack(multiANewArrayInsn.dim, instruction.opcode);
        SymbolicValue arrayRef = new SymbolicValue();
        programState = pop.state.stackValue(arrayRef).addConstraint(arrayRef, ObjectConstraint.NOT_NULL);
        break;
      default:
        throw new IllegalStateException("Instruction not handled. " + instruction);
    }
    checkerDispatcher.executeCheckPostStatement(instruction);
  }

  private static ProgramState stackValues(ProgramState.Pop pop, int... values) {
    ProgramState ps = pop.state;
    for (int value : values) {
      ps = ps.stackValue(pop.values.get(value));
    }
    return ps;
  }

  private ProgramState setDoubleOrLong(SymbolicValue sv, boolean value) {
    return setDoubleOrLong(programState, sv, value);
  }

  private static ProgramState setDoubleOrLong(ProgramState programState, SymbolicValue sv, boolean value) {
    if (value) {
      return programState.addConstraint(sv, LONG_OR_DOUBLE);
    } else {
      return programState.removeConstraintsOnDomain(sv, StackValueCategoryConstraint.class);
    }
  }

  private boolean isDoubleOrLong(SymbolicValue sv) {
    return programState.getConstraint(sv, StackValueCategoryConstraint.class) == LONG_OR_DOUBLE;
  }

  private ProgramState.Pop popStack(int nbOfValues, int opcode) {
    ProgramState.Pop pop = programState.unstackValue(nbOfValues);
    Preconditions.checkState(pop.values.size() == nbOfValues, "%s needs %s values on stack", Printer.OPCODES[opcode], nbOfValues);
    return pop;
  }

  private boolean handleMethodInvocation(Instruction instruction) {
    boolean isStatic = instruction.opcode == Opcodes.INVOKESTATIC;
    int arity = isStatic ? instruction.arity() : (instruction.arity() + 1);
    ProgramState.Pop pop = programState.unstackValue(arity);
    Preconditions.checkState(pop.values.size() == arity, "Arguments mismatch for INVOKE");
    // TODO use constraintManager.createMethodSymbolicValue to create relational SV for equals
    programState = pop.state;
    SymbolicValue returnSV = instruction.hasReturnValue() ? constraintManager.createSymbolicValue(instruction) : null;
    String signature = instruction.fieldOrMethod.completeSignature();
    MethodBehavior methodInvokedBehavior = behaviorCache.get(signature);
    enqueueUncheckedExceptions();
    // FIXME : empty yields here should not happen, for now act as if behavior was not resolved.
    if (methodInvokedBehavior != null && methodInvokedBehavior.isComplete() && !methodInvokedBehavior.yields().isEmpty()) {
      List<SymbolicValue> stack = Lists.reverse(pop.values);
      if (!isStatic) {
        // remove "thisSV" from stack before trying to apply any yield, as it should not match with arguments
        stack = stack.subList(1, stack.size());
      }
      List<SymbolicValue> arguments = stack;

      methodInvokedBehavior
        .happyPathYields()
        .forEach(yield -> yield.statesAfterInvocation(arguments, Collections.emptyList(), programState, () -> returnSV).forEach(ps -> {
          checkerDispatcher.methodYield = yield;
          if (ps.peekValue() != null) {
            ps = setDoubleOrLong(ps, ps.peekValue(), instruction.isLongOrDoubleValue());
          }
          checkerDispatcher.addTransition(ps);
          checkerDispatcher.methodYield = null;
        }));
      methodInvokedBehavior
        .exceptionalPathYields()
        .forEach(yield -> {
          Type exceptionType = yield.exceptionType(semanticModel);
          yield.statesAfterInvocation(
            arguments, Collections.emptyList(), programState, () -> constraintManager.createExceptionalSymbolicValue(exceptionType)).forEach(ps -> {
              ps.storeExitValue();
              enqueueExceptionHandlers(exceptionType, ps);
          });
        });
      return true;
    }
    if (methodInvokedBehavior != null) {
      methodInvokedBehavior.getDeclaredExceptions().forEach(exception -> {
        Type exceptionType = semanticModel.getClassType(exception);
        ProgramState ps = programState.stackValue(constraintManager.createExceptionalSymbolicValue(exceptionType));
        enqueueExceptionHandlers(exceptionType, ps);
      });
    }
    if (instruction.hasReturnValue()) {
      programState = programState.stackValue(returnSV);
      programState = setDoubleOrLong(returnSV, instruction.isLongOrDoubleValue());
    }
    return false;
  }

  private void enqueueUncheckedExceptions() {
    programPosition.block.successors()
        .stream()
        .map(BytecodeCFG.Block.class::cast)
        .filter(this::isUncheckedExceptionCatchBlock)
        .forEach(b -> enqueue(new ProgramPoint(b), stateWithException(programState, b)));
  }

  private boolean isUncheckedExceptionCatchBlock(BytecodeCFG.Block b) {
    return b.isCatchBlock() && ExceptionUtils.isUncheckedException(b.getExceptionType(semanticModel));
  }

  private ProgramState stateWithException(ProgramState programState, BytecodeCFG.Block b) {
    Type exceptionType = b.getExceptionType(semanticModel);
    SymbolicValue.ExceptionalSymbolicValue sv = new SymbolicValue.ExceptionalSymbolicValue(exceptionType);
    return programState.stackValue(sv);
  }

  private void enqueueExceptionHandlers(Type exceptionType, ProgramState ps) {
    List<BytecodeCFG.Block> blocksCatchingException = programPosition.block.successors().stream()
        .map(b -> (BytecodeCFG.Block) b)
        .filter(BytecodeCFG.Block::isCatchBlock)
        .filter(b -> isExceptionHandledByBlock(exceptionType, b))
        .collect(Collectors.toList());
    if (!blocksCatchingException.isEmpty()) {
      blocksCatchingException.forEach(b -> enqueue(new ProgramPoint(b), ps));
      if (isCatchExhaustive(exceptionType, blocksCatchingException)) {
        return;
      }
    }
    // exception was not handled or was handled only partially, enqueue exit block with exceptional SV
    Preconditions.checkState(ps.peekValue() instanceof SymbolicValue.ExceptionalSymbolicValue,
        "Exception shall be on top of the stack");
    ps.storeExitValue();
    enqueue(new ProgramPoint(exitBlock), ps);
  }

  private boolean isCatchExhaustive(Type exceptionType, List<BytecodeCFG.Block> blocksCatchingException) {
    return blocksCatchingException.stream()
        .filter(BytecodeCFG.Block::isCatchBlock)
        .anyMatch(b -> b.isUncaughtException() || exceptionType.isSubtypeOf(b.getExceptionType(semanticModel)));
  }

  private boolean isExceptionHandledByBlock(Type exceptionType, BytecodeCFG.Block b) {
    Type blockException = b.getExceptionType(semanticModel);
    return b.isUncaughtException()
      ||/*required as long as there is no real type tracking*/ exceptionType == null
      || exceptionType.isSubtypeOf(blockException)
      || blockException.isSubtypeOf(exceptionType);
  }

  @VisibleForTesting
  void handleBlockExit(ProgramPoint programPosition) {
    BytecodeCFG.Block block = (BytecodeCFG.Block) programPosition.block;
    Instruction terminator = block.terminator();
    if (terminator == null) {
      enqueueHappyPath(programPosition);
      return;
    }
    switch (terminator.opcode) {
      case GOTO:
        enqueueHappyPath(programPosition);
        break;
      case TABLESWITCH:
      case LOOKUPSWITCH:
        programState = programState.unstackValue(1).state;
        enqueueHappyPath(programPosition);
        break;
      default:
        handleBranching(terminator);
    }
  }

  private void handleBranching(Instruction terminator) {
    programState = branchingState(terminator, programState);
    Pair<List<ProgramState>, List<ProgramState>> pair = constraintManager.assumeDual(programState);
    ProgramPoint falsePP = new ProgramPoint(((BytecodeCFG.Block) programPosition.block).falseSuccessor());
    ProgramPoint truePP = new ProgramPoint(((BytecodeCFG.Block) programPosition.block).trueSuccessor());
    pair.a.forEach(s -> enqueue(falsePP, s));
    pair.b.forEach(s -> enqueue(truePP, s));
  }

  @VisibleForTesting
  ProgramState branchingState(Instruction terminator, ProgramState programState) {
    ProgramState.Pop pop;
    ProgramState ps;
    List<ProgramState.SymbolicValueSymbol> symbolicValueSymbols;
    switch (terminator.opcode) {
      case IFEQ:
      case IFNE:
      case IFLT:
      case IFGE:
      case IFGT:
      case IFLE:
        pop = programState.unstackValue(1);
        SymbolicValue svZero = new SymbolicValue();
        symbolicValueSymbols = ImmutableList.of(
            new ProgramState.SymbolicValueSymbol(svZero, null),
            pop.valuesAndSymbols.get(0));
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
        symbolicValueSymbols = ImmutableList.of(
            new ProgramState.SymbolicValueSymbol(SymbolicValue.NULL_LITERAL, null),
            pop.valuesAndSymbols.get(0));
        ps = pop.state;
        break;
      default:
        throw new IllegalStateException("Unexpected terminator " + terminator);
    }
    return ps.stackValue(constraintManager.createBinarySymbolicValue(terminator, symbolicValueSymbols));
  }

  private void enqueueHappyPath(ProgramPoint programPosition) {
    programPosition.block.successors().stream()
      .map(b-> (BytecodeCFG.Block)b)
      .filter(b -> !b.isCatchBlock())
      .forEach(b -> enqueue(new ProgramPoint(b), programState));
  }

  private void executeCheckEndOfExecution() {
    // TODO callback to checks at end of execution
  }

  @VisibleForTesting
  Iterable<ProgramState> startingStates(String signature, ProgramState currentState, boolean isStaticMethod) {
    // TODO : deal with parameter annotations, equals methods etc.
    int parameterIdx = 0;
    ProgramState state = currentState;
    if(!isStaticMethod) {
      // Add a sv for "this"
      SymbolicValue thisSV = constraintManager.createSymbolicValue((Instruction) null);
      state = currentState.addConstraint(thisSV, ObjectConstraint.NOT_NULL)
        .addConstraint(thisSV, new TypedConstraint(signature.substring(0, signature.indexOf('#')))).put(0, thisSV);
      parameterIdx = 1;
    }
    org.objectweb.asm.Type[] argumentTypes = org.objectweb.asm.Type.getArgumentTypes(signature.substring(signature.indexOf('(')));
    for (org.objectweb.asm.Type argumentType: argumentTypes) {
      SymbolicValue sv = constraintManager.createSymbolicValue((Instruction) null);
      methodBehavior.addParameter(sv);
      state = state.put(parameterIdx, sv);
      state = setDoubleOrLong(state, sv, argumentType.getSize() == 2);
      parameterIdx += argumentType.getSize();
    }
    return Collections.singletonList(state);
  }

  @VisibleForTesting
  void setNode(ExplodedGraph.Node node) {
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
