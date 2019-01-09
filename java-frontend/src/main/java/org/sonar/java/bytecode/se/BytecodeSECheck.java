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

import java.util.List;
import org.objectweb.asm.Opcodes;
import org.sonar.java.bytecode.cfg.Instruction;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.DivisionByZeroCheck.ZeroConstraint;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;

import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DNEG;
import static org.objectweb.asm.Opcodes.DREM;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.FREM;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IDIV;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INEG;
import static org.objectweb.asm.Opcodes.IREM;
import static org.objectweb.asm.Opcodes.ISHL;
import static org.objectweb.asm.Opcodes.ISHR;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.IUSHR;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LDIV;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LNEG;
import static org.objectweb.asm.Opcodes.LREM;
import static org.objectweb.asm.Opcodes.LSHL;
import static org.objectweb.asm.Opcodes.LSHR;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.LUSHR;

public interface BytecodeSECheck {

  default ProgramState checkPreStatement(CheckerDispatcher dispatcher, Instruction inst) {
    return dispatcher.getState();
  }

  default ProgramState checkPostStatement(CheckerDispatcher dispatcher, Instruction inst) {
    return dispatcher.getState();
  }

  class NullnessCheck implements BytecodeSECheck {

    @Override
    public ProgramState checkPreStatement(CheckerDispatcher dispatcher, Instruction inst) {
      ProgramState state = dispatcher.getState();
      if (isInvokeOnObjectRef(inst)) {
        SymbolicValue objectRef = state.peekValue(inst.arity());
        ObjectConstraint constraint = state.getConstraint(objectRef, ObjectConstraint.class);
        if (constraint != null && constraint.isNull()) {
//       Exceptional Yield should be added : context.addExceptionalYield(currentVal, programState, JAVA_LANG_NPE, this);
          return null;
        }
        if (constraint == null) {
//       Exceptional Yield should be added : context.addExceptionalYield(currentVal, programState.addConstraint(currentVal, ObjectConstraint.NULL), JAVA_LANG_NPE, this);
          // We dereferenced the target value for the member select, so we can assume it is not null when not already known
          return state.addConstraint(objectRef, ObjectConstraint.NOT_NULL);
        }

      }
      return state;
    }

    private static boolean isInvokeOnObjectRef(Instruction inst) {
      return inst.opcode == Opcodes.INVOKEINTERFACE || inst.opcode == Opcodes.INVOKESPECIAL || inst.opcode == Opcodes.INVOKEVIRTUAL;
    }
  }

  class ZeronessCheck implements BytecodeSECheck {

    @Override
    public ProgramState checkPostStatement(CheckerDispatcher dispatcher, Instruction inst) {
      ProgramState currentState = dispatcher.getState();
      ExplodedGraph.Node node = dispatcher.getNode();
      if (node == null) {
        return currentState;
      }
      ProgramState previousState = node.programState;
      switch (inst.opcode) {
        case DADD:
        case FADD:
        case IADD:
        case LADD:
        case DSUB:
        case FSUB:
        case ISUB:
        case LSUB:
          return handlePlusMinus(previousState, currentState);
        case DMUL:
        case FMUL:
        case IMUL:
        case LMUL:
          return handleMultiply(previousState, currentState);
        case DDIV:
        case FDIV:
        case IDIV:
        case LDIV:
          return handleDivisionRemainder(previousState, currentState, true);
        case DREM:
        case FREM:
        case IREM:
        case LREM:
          return handleDivisionRemainder(previousState, currentState, false);
        case ISHL:
        case LSHL:
        case ISHR:
        case LSHR:
        case IUSHR:
        case LUSHR:
          return handleShift(previousState, currentState);
        case INEG:
        case LNEG:
        case FNEG:
        case DNEG:
          return handleNegation(previousState, currentState);
        default:
          break;
      }
      return currentState;
    }

    private static ProgramState handlePlusMinus(ProgramState previousState, ProgramState currentState) {
      List<SymbolicValue> operands = previousState.peekValues(2);
      SymbolicValue result = currentState.peekValue();
      SymbolicValue op1 = operands.get(0);
      SymbolicValue op2 = operands.get(1);
      boolean op1Zero = isZero(currentState, op1);
      boolean op2Zero = isZero(currentState, op2);
      if (op2Zero) {
        return currentState.unstackValue(1).state.stackValue(op1);
      } else if (op1Zero) {
        return currentState.unstackValue(1).state.stackValue(op2);
      }
      // we know nothing about zero-ness
      return currentState.removeConstraintsOnDomain(result, BooleanConstraint.class);
    }

    private static ProgramState handleMultiply(ProgramState previousState, ProgramState currentState) {
      List<SymbolicValue> operands = previousState.peekValues(2);
      SymbolicValue result = currentState.peekValue();
      SymbolicValue op1 = operands.get(0);
      SymbolicValue op2 = operands.get(1);
      boolean op1Zero = isZero(currentState, op1);
      if (op1Zero || isZero(currentState, op2)) {
        // Reuse zero
        return currentState.unstackValue(1).state.stackValue(op1Zero ? op1 : op2);
      }
      if (isNonZero(currentState, op1) && isNonZero(currentState, op2)) {
        return currentState.removeConstraintsOnDomain(result, BooleanConstraint.class).addConstraint(result, ZeroConstraint.NON_ZERO);
      }
      return currentState.removeConstraintsOnDomain(result, BooleanConstraint.class);
    }

    private static ProgramState handleDivisionRemainder(ProgramState previousState, ProgramState currentState, boolean isDivision) {
      List<SymbolicValue> operands = previousState.peekValues(2);
      SymbolicValue result = currentState.peekValue();
      SymbolicValue op1 = operands.get(0);
      SymbolicValue op2 = operands.get(1);
      if (isZero(currentState, op2)) {
        // Division by zero
        // TODO exceptional yield ?
        return null;
      } else if (isZero(currentState, op1)) {
        // Reuse zero
        return currentState.unstackValue(1).state.stackValue(op1);
      }
      if (isNonZero(currentState, op1) && isDivision) {
        return currentState.removeConstraintsOnDomain(result, BooleanConstraint.class).addConstraint(result, ZeroConstraint.NON_ZERO);
      }
      return currentState.removeConstraintsOnDomain(result, BooleanConstraint.class);
    }

    private static ProgramState handleNegation(ProgramState previousState, ProgramState currentState) {
      List<SymbolicValue> operands = previousState.peekValues(1);
      SymbolicValue result = currentState.peekValue();
      SymbolicValue op1 = operands.get(0);
      if (isZero(currentState, op1)) {
        // Reuse zero
        return currentState.unstackValue(1).state.stackValue(op1);
      }
      if (isNonZero(currentState, op1)) {
        return currentState.removeConstraintsOnDomain(result, BooleanConstraint.class).addConstraint(result, ZeroConstraint.NON_ZERO);
      }
      return currentState.removeConstraintsOnDomain(result, BooleanConstraint.class);
    }

    private static ProgramState handleShift(ProgramState previousState, ProgramState currentState) {
      List<SymbolicValue> operands = previousState.peekValues(2);
      SymbolicValue result = currentState.peekValue();
      SymbolicValue op1 = operands.get(0);
      SymbolicValue op2 = operands.get(0);
      if (isZero(currentState, op1) || isZero(currentState, op2)) {
        // shifting by zero or shifting zero: Reuse fist operand
        return currentState.unstackValue(1).state.stackValue(op1);
      }
      return currentState.removeConstraintsOnDomain(result, BooleanConstraint.class).addConstraint(result, ZeroConstraint.NON_ZERO);
    }

    private static boolean isZero(ProgramState state, SymbolicValue sv) {
      return state.getConstraint(sv, ZeroConstraint.class) == ZeroConstraint.ZERO;
    }

    private static boolean isNonZero(ProgramState state, SymbolicValue sv) {
      return state.getConstraint(sv, ZeroConstraint.class) == ZeroConstraint.NON_ZERO;
    }
  }
}
