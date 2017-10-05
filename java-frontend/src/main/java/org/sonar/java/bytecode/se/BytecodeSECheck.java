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

import org.objectweb.asm.Opcodes;
import org.sonar.java.bytecode.cfg.Instruction;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;

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

  }
}
