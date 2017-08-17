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

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.sonar.java.bytecode.cfg.BytecodeCFGBuilder;
import org.sonar.java.bytecode.cfg.BytecodeCFGBuilder.Instruction;
import org.sonar.java.se.ProgramPoint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BytecodeEGWalkerExecuteTest {

  @Test
  public void test_nop() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.NOP));
    assertThat(programState).isEqualTo(ProgramState.EMPTY_STATE);
  }

  @Test
  public void test_ldc() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.LDC));
    assertStack(programState, ObjectConstraint.NOT_NULL);
  }

  @Test
  public void test_aconst_null() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.ACONST_NULL));
    assertStack(programState, ObjectConstraint.NULL);
  }

  @Test
  public void test_areturn() throws Exception {
    SymbolicValue returnValue = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.ARETURN), ProgramState.EMPTY_STATE.stackValue(returnValue));
    assertThat(programState.peekValue()).isEqualTo(returnValue);
  }

  @Test
  public void test_iconst() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.ICONST_0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, BooleanConstraint.FALSE}});

    programState = execute(new Instruction(Opcodes.ICONST_1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, BooleanConstraint.TRUE}});

    int[] opCodesConst = new int[] {Opcodes.ICONST_M1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5};
    for (int opcode : opCodesConst) {
      programState = execute(new Instruction(opcode));
      assertStack(programState, DivisionByZeroCheck.ZeroConstraint.NON_ZERO);
    }
  }

  @Test
  public void test_load() throws Exception {
    int[] loadRefOpcodes = new int[] {Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD};
    for (int loadRefOpcode : loadRefOpcodes) {
      SymbolicValue loadRef = new SymbolicValue();
      ProgramState programState = execute(new Instruction(loadRefOpcode, 0), ProgramState.EMPTY_STATE.put(0, loadRef));
      assertThat(programState.peekValue()).isEqualTo(loadRef);
      // no SV indexed should failed
      assertThatThrownBy(() -> execute(new Instruction(loadRefOpcode, 0), ProgramState.EMPTY_STATE)).hasMessage("Loading a symbolic value unindexed");
    }
  }

  @Test
  public void test_new() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.NEW));
    assertStack(programState, ObjectConstraint.NOT_NULL);
  }

  @Test
  public void test_dup() throws Exception {
    SymbolicValue sv = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP), ProgramState.EMPTY_STATE.stackValue(sv));
    ProgramState.Pop pop = programState.unstackValue(2);
    assertThat(pop.values).containsOnly(sv);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP)))
      .hasMessage("DUP on empty stack");
  }

  @Test
  public void test_invoke_instance_method() throws Exception {
    int[] opcodes = new int[] {Opcodes.INVOKESPECIAL, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE};
    for (int opcode: opcodes) {
      SymbolicValue thisSv = new SymbolicValue();
      ProgramState stateWithThis = ProgramState.EMPTY_STATE.stackValue(thisSv);
      ProgramState programState = execute(invokeMethod(opcode, "()V"), stateWithThis);
      assertEmptyStack(programState);
      assertThat(programState.getConstraints(thisSv).get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);

      programState = execute(invokeMethod(opcode, "()Z"), stateWithThis);
      assertStack(programState, new Constraint[] {null});

      SymbolicValue arg = new SymbolicValue();
      programState = execute(invokeMethod(opcode, "(I)I"), stateWithThis.stackValue(arg));
      assertStack(programState, new Constraint[] {null});
      assertThat(programState.peekValue()).isNotEqualTo(arg);

      programState = execute(invokeMethod(opcode, "(II)V"), stateWithThis.stackValue(arg).stackValue(arg));
      assertEmptyStack(programState);
      assertThatThrownBy(() -> execute(invokeMethod(opcode, "(II)V"), stateWithThis))
        .hasMessage("Arguments mismatch for INVOKE");
    }
  }

  @Test
  public void test_invoke_static() throws Exception {
    ProgramState programState = execute(invokeStatic("()V"));
    assertEmptyStack(programState);

    programState = execute(invokeStatic("()Z"));
    assertStack(programState, new Constraint[]{null});

    SymbolicValue arg = new SymbolicValue();
    programState = execute(invokeStatic("(I)I"), ProgramState.EMPTY_STATE.stackValue(arg));
    assertStack(programState, new Constraint[]{null});
    assertThat(programState.peekValue()).isNotEqualTo(arg);

    programState = execute(invokeStatic("(II)V"), ProgramState.EMPTY_STATE.stackValue(arg).stackValue(arg));
    assertEmptyStack(programState);

    assertThatThrownBy(() -> execute(invokeStatic("(I)V")))
      .hasMessage("Arguments mismatch for INVOKE");
  }

  @Test
  public void test_athrow() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.ATHROW), ProgramState.EMPTY_STATE.stackValue(new SymbolicValue()));
    SymbolicValue exception = programState.peekValue();
    assertThat(exception).isInstanceOf(SymbolicValue.ExceptionalSymbolicValue.class);
    assertThat(((SymbolicValue.ExceptionalSymbolicValue) exception).exceptionType()).isNull();
    assertThat(programState.exitValue()).isEqualTo(exception);
  }

  private BytecodeCFGBuilder.Instruction invokeMethod(int opcode, String desc) {
    return new Instruction(opcode, new Instruction.FieldOrMethod("owner", "name", desc, false));
  }

  private BytecodeCFGBuilder.Instruction invokeStatic(String desc) {
    return new Instruction(Opcodes.INVOKESTATIC, new Instruction.FieldOrMethod("owner", "name", desc, false));
  }

  private void assertStack(ProgramState ps, Constraint... constraints) {
    Constraint[][] cs = new Constraint[constraints.length][1];
    int i = 0;
    for (Constraint constraint : constraints) {
      cs[i] = new Constraint[] {constraint};
      i++;
    }
    assertStack(ps, cs);
  }

  private void assertStack(ProgramState ps, Constraint[]... constraints) {
    ProgramState.Pop pop = ps.unstackValue(constraints.length);
    assertEmptyStack(pop.state);
    assertThat(pop.valuesAndSymbols).hasSize(constraints.length);
    List<SymbolicValue> symbolicValues = pop.values;
    int idx = 0;
    for (SymbolicValue sv : symbolicValues) {
      ConstraintsByDomain constraintsByDomain = ps.getConstraints(sv);
      for (Constraint expectedConstraint : constraints[idx]) {
        if (expectedConstraint != null) {
          Class<? extends Constraint> expectedConstraintDomain = expectedConstraint.getClass();
          Constraint constraint = constraintsByDomain.get(expectedConstraintDomain);
          assertThat(constraint).isEqualTo(expectedConstraint);
          constraintsByDomain = constraintsByDomain.remove(expectedConstraintDomain);
        } else {
          assertThat(constraintsByDomain).isNull();
        }
      }
      if(constraintsByDomain != null) {
        assertThat(constraintsByDomain.isEmpty()).isTrue();
      }
      idx++;
    }
  }

  private void assertEmptyStack(ProgramState programState) {
    assertThat(programState.peekValue()).isNull();
  }

  private ProgramState execute(Instruction instruction) {
    return execute(instruction, ProgramState.EMPTY_STATE);
  }

  private ProgramState execute(Instruction instruction, ProgramState startingState) {
    BytecodeEGWalker walker = new BytecodeEGWalker(new BehaviorCache(null));
    ProgramPoint programPoint = mock(ProgramPoint.class);
    when(programPoint.next()).thenReturn(programPoint);
    walker.programPosition = programPoint;
    walker.programState = startingState;
    walker.executeInstruction(instruction);
    return walker.programState;
  }
}
