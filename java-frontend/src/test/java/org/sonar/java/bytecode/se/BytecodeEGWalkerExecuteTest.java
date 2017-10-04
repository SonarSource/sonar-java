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
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.sonar.java.bytecode.cfg.BytecodeCFGBuilder;
import org.sonar.java.bytecode.cfg.BytecodeCFGBuilder.Instruction;
import org.sonar.java.bytecode.cfg.Instructions;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.ProgramPoint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.BinarySymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.objectweb.asm.Opcodes.*;

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
  public void test_xReturn() throws Exception {
    SymbolicValue returnValue = new SymbolicValue();
    int[] opcodes = {Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN};
    for (int opcode : opcodes) {
      ProgramState programState = execute(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(returnValue));
      assertThat(programState.peekValue()).isNull();
      assertThat(programState.exitValue()).isEqualTo(returnValue);
    }
  }

  @Test
  public void test_return() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.RETURN), ProgramState.EMPTY_STATE);
    assertThat(programState.peekValue()).isNull();
    assertThat(programState.exitValue()).isNull();
  }

  @Test
  public void test_iconst() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.ICONST_0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, BooleanConstraint.FALSE, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.ICONST_1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, BooleanConstraint.TRUE, ObjectConstraint.NOT_NULL}});

    int[] opCodesConst = new int[] {Opcodes.ICONST_M1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5};
    for (int opcode : opCodesConst) {
      programState = execute(new Instruction(opcode));
      assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL}});
    }
  }

  @Test
  public void test_lconst() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.LCONST_0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, BooleanConstraint.FALSE, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.LCONST_1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, BooleanConstraint.TRUE, ObjectConstraint.NOT_NULL}});
  }

  @Test
  public void test_fconst() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.FCONST_0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, BooleanConstraint.FALSE, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.FCONST_1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, BooleanConstraint.TRUE, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.FCONST_2));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL}});
  }

  @Test
  public void test_bipush() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.BIPUSH, 42));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.BIPUSH, 1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL, BooleanConstraint.TRUE}});

    programState = execute(new Instruction(Opcodes.BIPUSH, 0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, ObjectConstraint.NOT_NULL, BooleanConstraint.FALSE}});
  }

  @Test
  public void test_sipush() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.SIPUSH, 42));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.SIPUSH, 1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL, BooleanConstraint.TRUE}});

    programState = execute(new Instruction(Opcodes.SIPUSH, 0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, ObjectConstraint.NOT_NULL, BooleanConstraint.FALSE}});
  }

  @Test
  public void test_array_load() throws Exception {
    int[] loadRefOpcodes = new int[] {Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD, Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD};
    SymbolicValue array = new SymbolicValue();
    SymbolicValue index = new SymbolicValue();
    ProgramState initState = ProgramState.EMPTY_STATE.stackValue(array).stackValue(index);
    for (int opcode : loadRefOpcodes) {
      ProgramState programState = execute(new Instruction(opcode), initState);
      if (opcode != Opcodes.AALOAD) {
        assertStack(programState, ObjectConstraint.NOT_NULL);
      } else {
        ProgramState.Pop result = programState.unstackValue(1);
        assertThat(result.values).hasSize(1);
        assertThat(result.state).isEqualTo(ProgramState.EMPTY_STATE);
        assertThat(result.values.get(0)).isNotEqualTo(array);
        assertThat(result.values.get(0)).isNotEqualTo(index);
      }
    }
  }

  @Test
  public void test_array_store() throws  Exception {
    int[] storeArrayOpcodes = new int[] {Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE};
    SymbolicValue array = new SymbolicValue();
    SymbolicValue index = new SymbolicValue();
    SymbolicValue value = new SymbolicValue();
    ProgramState initState = ProgramState.EMPTY_STATE.stackValue(array).stackValue(index).stackValue(value);
    for (int opcode : storeArrayOpcodes) {
      ProgramState ps = execute(new Instruction(opcode), initState);
      assertEmptyStack(ps);
    }
  }

  @Test
  public void test_dconst() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.DCONST_0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, BooleanConstraint.FALSE, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.DCONST_1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, BooleanConstraint.TRUE, ObjectConstraint.NOT_NULL}});
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
  public void test_pop() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.POP), ProgramState.EMPTY_STATE.stackValue(sv1).stackValue(sv2));
    assertThat(programState.peekValue()).isEqualTo(sv1);
    programState = execute(new Instruction(Opcodes.POP));
    assertEmptyStack(programState);
  }

  @Test
  public void test_pop2() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.POP2), ProgramState.EMPTY_STATE.stackValue(sv1).stackValue(sv2).stackValue(sv3));
    assertThat(programState.peekValue()).isEqualTo(sv1);
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
  public void test_dup_x1() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP_X1), ProgramState.EMPTY_STATE.stackValue(sv3).stackValue(sv2).stackValue(sv1));
    ProgramState.Pop pop = programState.unstackValue(4);
    assertThat(pop.values).containsExactly(sv1, sv2, sv1, sv3);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP_X1), ProgramState.EMPTY_STATE.stackValue(sv1)))
      .hasMessage("DUP_X1 needs 2 values on stack");
  }

  @Test
  public void test_dup_x2() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP_X2), ProgramState.EMPTY_STATE.stackValue(sv3).stackValue(sv2).stackValue(sv1));
    ProgramState.Pop pop = programState.unstackValue(4);
    assertThat(pop.values).containsExactly(sv1, sv2, sv3, sv1);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP_X2), ProgramState.EMPTY_STATE.stackValue(sv1)))
      .hasMessage("DUP_X2 needs 3 values on stack");
  }

  @Test
  public void test_dup2() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP2), ProgramState.EMPTY_STATE.stackValue(sv2).stackValue(sv1));
    ProgramState.Pop pop = programState.unstackValue(4);
    assertThat(pop.values).containsOnly(sv1, sv2, sv1, sv2);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP2)))
      .hasMessage("DUP2 needs 2 values on stack");
  }

  @Test
  public void test_dup2_x1() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP2_X1), ProgramState.EMPTY_STATE.stackValue(sv3).stackValue(sv2).stackValue(sv1));
    ProgramState.Pop pop = programState.unstackValue(5);
    assertThat(pop.values).containsExactly(sv1, sv2, sv3, sv1, sv2);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP2_X1), ProgramState.EMPTY_STATE.stackValue(sv1)))
      .hasMessage("DUP2_X1 needs 3 values on stack");
  }

  @Test
  public void test_swap() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.SWAP), ProgramState.EMPTY_STATE.stackValue(sv1).stackValue(sv2));
    ProgramState.Pop pop = programState.unstackValue(2);
    assertThat(pop.values).containsExactly(sv1, sv2);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.SWAP), ProgramState.EMPTY_STATE.stackValue(sv1)))
      .hasMessage("SWAP needs 2 values on stack");
  }

  @Test
  public void test_dup2_x2() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    SymbolicValue sv4 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP2_X2), ProgramState.EMPTY_STATE.stackValue(sv4).stackValue(sv3).stackValue(sv2).stackValue(sv1));
    ProgramState.Pop pop = programState.unstackValue(6);
    assertThat(pop.values).containsExactly(sv1, sv2, sv3, sv4, sv1, sv2);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP2_X2), ProgramState.EMPTY_STATE.stackValue(sv1)))
      .hasMessage("DUP2_X2 needs 4 values on stack");
  }

  @Test
  public void test_add_sub_mul_div_rem() throws Exception {
    int[] opcodes = new int[] {
      Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD,
      Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB,
      Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL,
      Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV,
      Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM,
    };
    assertConsume2produceNotNull(opcodes);

    assertThrowWhenInvalidStack(opcodes, "Arithmetic instruction needs 2 values on stack");
  }

  @Test
  public void test_neg() throws Exception {
    SymbolicValue sv = new SymbolicValue();

    int[] negOpcodes = new int[] { Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG };
    ProgramState initState = ProgramState.EMPTY_STATE.stackValue(sv);
    for (int negOpcode : negOpcodes) {
      ProgramState programState = execute(new Instruction(negOpcode), initState);
      assertStack(programState, new Constraint[][]{{ ObjectConstraint.NOT_NULL }});
      assertThat(programState.peekValue()).isNotEqualTo(sv);
    }

    for (int opcode : negOpcodes) {
      assertThatThrownBy(() -> execute(new Instruction(opcode), ProgramState.EMPTY_STATE))
        .hasMessage("NEG needs value on stack");
    }
  }

  @Test
  public void test_shift() throws Exception {
    int[] shiftOpcodes = new int[] {Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR, Opcodes.LUSHR};
    assertConsume2produceNotNull(shiftOpcodes);

    assertThrowWhenInvalidStack(shiftOpcodes, "Arithmetic instruction needs 2 values on stack");
  }

  @Test
  public void test_and() throws Exception {
    int[] opcodes = new int[] {Opcodes.IAND, Opcodes.LAND};
    assertBinarySymbolicValue(opcodes, SymbolicValue.AndSymbolicValue.class);

    assertThrowWhenInvalidStack(opcodes, "Bitwise instruction needs 2 values on stack");
  }

  private void assertThrowWhenInvalidStack(int[] opcodes, String message) {
    for (int opcode : opcodes) {
      assertThatThrownBy(() -> execute(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(new SymbolicValue())))
        .hasMessage(message);
    }
  }

  private void assertBinarySymbolicValue(int[] opcodes, Class<? extends BinarySymbolicValue> binarySvClass) {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    ProgramState initState = ProgramState.EMPTY_STATE.stackValue(sv2).stackValue(sv1);
    for (int opcode : opcodes) {
      ProgramState programState = execute(new Instruction(opcode), initState);
      ProgramState.Pop pop = programState.unstackValue(1);
      assertStack(programState, new Constraint[][] {{ObjectConstraint.NOT_NULL}});
      SymbolicValue result = pop.values.get(0);
      assertThat(result).isNotEqualTo(sv1);
      assertThat(result).isNotEqualTo(sv2);
      assertThat(result).isInstanceOf(binarySvClass);
      BinarySymbolicValue andSv = (BinarySymbolicValue) result;
      assertThat(andSv.getRightOp()).isEqualTo(sv1);
      assertThat(andSv.getLeftOp()).isEqualTo(sv2);
    }
  }

  @Test
  public void test_or() throws Exception {
    int[] opcodes = new int[] {Opcodes.IOR, Opcodes.LOR};
    assertBinarySymbolicValue(opcodes, SymbolicValue.OrSymbolicValue.class);

    assertThrowWhenInvalidStack(opcodes, "Bitwise instruction needs 2 values on stack");
  }

  @Test
  public void test_xor() throws Exception {
    int[] opcodes = new int[] {Opcodes.IXOR, Opcodes.LXOR};
    assertBinarySymbolicValue(opcodes, SymbolicValue.XorSymbolicValue.class);

    assertThrowWhenInvalidStack(opcodes, "Bitwise instruction needs 2 values on stack");
  }

  @Test
  public void test_iinc() throws Exception {
    SymbolicValue sv = new SymbolicValue();
    ProgramState programState = ProgramState.EMPTY_STATE.put(2, sv);
    programState = execute(new Instruction(Opcodes.IINC, 2), programState);
    SymbolicValue result = programState.getValue(2);
    assertThat(result).isNotEqualTo(sv);
    assertThat(programState.getConstraint(result, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    assertEmptyStack(programState);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.IINC, 1), ProgramState.EMPTY_STATE))
      .hasMessage("Local variable 1 not found");
  }

  @Test
  public void test_cmp() throws Exception {
    int[] opcodes = new int[] {Opcodes.LCMP, Opcodes.FCMPG, Opcodes.FCMPL, Opcodes.DCMPG, Opcodes.FCMPL};
    assertConsume2produceNotNull(opcodes);

    assertThrowWhenInvalidStack(opcodes, "CMP needs 2 values on stack");
  }

  private void assertConsume2produceNotNull(int... opcodes) {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    ProgramState initState = ProgramState.EMPTY_STATE.stackValue(sv2).stackValue(sv1);
    for (int opcode : opcodes) {
      ProgramState programState = execute(new Instruction(opcode), initState);
      ProgramState.Pop pop = programState.unstackValue(1);
      assertStack(programState, new Constraint[][] {{ObjectConstraint.NOT_NULL}});
      SymbolicValue result = pop.values.get(0);
      assertThat(result).isNotEqualTo(sv1);
      assertThat(result).isNotEqualTo(sv2);
    }
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
        .isInstanceOf(IllegalStateException.class);
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

  @Test
  public void test_nullness_check() throws Exception {
    SymbolicValue thisSv = new SymbolicValue();
    ProgramState startingState = ProgramState.EMPTY_STATE.stackValue(thisSv);
    ProgramState programState = execute(invokeMethod(Opcodes.INVOKESPECIAL, "()V"), startingState);
    assertThat(hasConstraint(thisSv, programState, ObjectConstraint.NOT_NULL)).isTrue();

    programState = execute(invokeMethod(Opcodes.INVOKESTATIC, "()V"), startingState);
    assertThat(programState).isEqualTo(startingState);

    programState = execute(invokeMethod(Opcodes.INVOKESPECIAL, "(I)V"), startingState.stackValue(new SymbolicValue()));
    assertThat(hasConstraint(thisSv, programState, ObjectConstraint.NOT_NULL)).isTrue();
  }

  @Test
  public void test_store() throws Exception {
    int[] storeOpcodes = new int[] {Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE};
    SymbolicValue sv = new SymbolicValue();
    ProgramState startState = ProgramState.EMPTY_STATE.stackValue(sv);
    for (int opcode : storeOpcodes) {
      ProgramState programState = execute(new Instruction(opcode, 67), startState);
      assertThat(programState.getValue(67)).isEqualTo(sv);
    }
  }

  @Test
  public void test_tableswitch() throws Exception {
    Instructions instr = new Instructions();
    instr.visitVarInsn(ILOAD, 0);
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    Label l3 = new Label();
    instr.visitTableSwitchInsn(0, 2, l3, new Label[] { l0, l1, l2 });
    instr.visitLabel(l0);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 1);
    instr.visitJumpInsn(GOTO, l3);
    instr.visitLabel(l1);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 2);
    instr.visitJumpInsn(GOTO, l3);
    instr.visitLabel(l2);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 3);
    instr.visitLabel(l3);
    instr.visitInsn(RETURN);
    BytecodeCFGBuilder.BytecodeCFG cfg = instr.cfg();

    CFG.IBlock<Instruction> entry = cfg.entry();
    BytecodeEGWalker walker = new BytecodeEGWalker(new BehaviorCache(null, null));
    walker.programState = ProgramState.EMPTY_STATE.stackValue(new SymbolicValue());
    walker.handleBlockExit(new ProgramPoint(entry));

    assertThat(walker.workList).hasSize(entry.successors().size());

    walker.workList.forEach(node -> {
      assertThat(node.programState.peekValue()).isNull();
      assertThat(entry.successors().contains(node.programPoint.block)).isTrue();
    });
  }

  @Test
  public void test_lookupswitch() throws Exception {
    Instructions instr = new Instructions();
    instr.visitVarInsn(ILOAD, 0);
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    Label l3 = new Label();
    instr.visitLookupSwitchInsn(l3, new int[] { 0, 1, 2, 50 }, new Label[] { l0, l1, l2, l3 });
    instr.visitLabel(l0);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 1);
    instr.visitJumpInsn(GOTO, l3);
    instr.visitLabel(l1);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 2);
    instr.visitJumpInsn(GOTO, l3);
    instr.visitLabel(l2);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 3);
    instr.visitJumpInsn(GOTO, l3);
    instr.visitLabel(l3);
    instr.visitInsn(RETURN);
    BytecodeCFGBuilder.BytecodeCFG cfg = instr.cfg();

    CFG.IBlock<Instruction> entry = cfg.entry();
    BytecodeEGWalker walker = new BytecodeEGWalker(new BehaviorCache(null, null));
    walker.programState = ProgramState.EMPTY_STATE.stackValue(new SymbolicValue());
    walker.handleBlockExit(new ProgramPoint(entry));

    assertThat(walker.workList).hasSize(entry.successors().size());

    walker.workList.forEach(node -> {
      assertThat(node.programState.peekValue()).isNull();
      assertThat(entry.successors().contains(node.programPoint.block)).isTrue();
    });
  }

  @Test
  public void test_getstatic() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.GETSTATIC));
    assertThat(programState.peekValue()).isNotNull();
  }

  @Test
  public void test_putstatic() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.PUTSTATIC), ProgramState.EMPTY_STATE.stackValue(new SymbolicValue()));
    assertThat(programState.peekValue()).isNull();
  }

  @Test
  public void test_getfield() throws Exception {
    SymbolicValue objectRef = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.GETFIELD), ProgramState.EMPTY_STATE.stackValue(objectRef));
    SymbolicValue fieldValue = programState.peekValue();
    assertThat(fieldValue).isNotNull();
    assertThat(fieldValue).isNotEqualTo(objectRef);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.GETFIELD))).hasMessage("GETFIELD needs 1 value on stack");
  }

  @Test
  public void test_putfield() throws Exception {
    SymbolicValue objectRef = new SymbolicValue();
    SymbolicValue value = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.PUTFIELD), ProgramState.EMPTY_STATE.stackValue(objectRef).stackValue(value));
    assertThat(programState.peekValue()).isNull();

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.PUTFIELD), ProgramState.EMPTY_STATE.stackValue(value))).hasMessage("PUTFIELD needs 2 values on stack");
  }

  @Test
  public void test_newarray() throws Exception {
    SymbolicValue size = new SymbolicValue();
    int[] opcodes = {Opcodes.NEWARRAY, Opcodes.ANEWARRAY};
    for (int opcode : opcodes) {
      ProgramState programState = execute(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(size));
      assertThat(programState.peekValue()).isNotEqualTo(size);
      assertStack(programState, ObjectConstraint.NOT_NULL);

      assertThatThrownBy(() -> execute(new Instruction(opcode), ProgramState.EMPTY_STATE)).hasMessage("NEWARRAY needs 1 value on stack");
    }
  }

  @Test
  public void test_arraylength() throws Exception {
    SymbolicValue arrayRef = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.ARRAYLENGTH), ProgramState.EMPTY_STATE.stackValue(arrayRef));
    SymbolicValue length = programState.peekValue();
    assertStack(programState, ObjectConstraint.NOT_NULL);
    assertThat(length).isNotEqualTo(arrayRef);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.ARRAYLENGTH), ProgramState.EMPTY_STATE)).hasMessage("ARRAYLENGTH needs 1 value on stack");
  }

  @Test
  public void test_checkcast() throws Exception {
    SymbolicValue objectRef = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.CHECKCAST), ProgramState.EMPTY_STATE.stackValue(objectRef));
    assertThat(programState.peekValue()).isEqualTo(objectRef);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.CHECKCAST), ProgramState.EMPTY_STATE)).hasMessage("CHECKCAST needs 1 value on stack");
  }

  private BytecodeCFGBuilder.Instruction invokeMethod(int opcode, String desc) {
    return new Instruction(opcode, new Instruction.FieldOrMethod("owner", "name", desc, false));
  }

  private boolean hasConstraint(SymbolicValue sv, ProgramState ps, Constraint constraint) {
    ConstraintsByDomain constraints = ps.getConstraints(sv);
    return constraints != null && constraints.get(constraint.getClass()) == constraint;
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
    BytecodeEGWalker walker = new BytecodeEGWalker(new BehaviorCache(null, null));
    ProgramPoint programPoint = mock(ProgramPoint.class);
    when(programPoint.next()).thenReturn(programPoint);
    walker.programPosition = programPoint;
    walker.programState = startingState;
    walker.executeInstruction(instruction);
    return walker.programState;
  }
}
