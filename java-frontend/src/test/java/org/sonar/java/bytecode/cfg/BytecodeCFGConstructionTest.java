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
package org.sonar.java.bytecode.cfg;

import com.google.common.collect.ImmutableList;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Printer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
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
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEDYNAMIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.JSR;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LOOKUPSWITCH;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.MULTIANEWARRAY;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.NEWARRAY;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RET;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.TABLESWITCH;

@RunWith(Parameterized.class)
public class BytecodeCFGConstructionTest {

  public static final String JAVA_LANG_OBJECT = "java/lang/Object";

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> testData = ImmutableList.builder();

    // Instructions without operand
    testData.addAll(
      Instructions.NO_OPERAND_INSN.stream()
        .map(opcode -> new Object[] {new TestInput(opcode), new InstructionExpectation(new BytecodeCFGBuilder.Instruction(opcode))})
        .collect(Collectors.toList()));

    // Instructions with int operand
    testData.add(new Object[] {new TestInput(BIPUSH, 1), null});
    testData.add(new Object[] {new TestInput(SIPUSH, 1), null});
    testData.add(new Object[] {new TestInput(NEWARRAY, 1), null});

    // LOAD STORE
    testData.add(new Object[] {new TestInput(ILOAD, 1), null});
    testData.add(new Object[] {new TestInput(LLOAD, 1), null});
    testData.add(new Object[] {new TestInput(FLOAD, 1), null});
    testData.add(new Object[] {new TestInput(DLOAD, 1), null});
    testData.add(new Object[] {new TestInput(ALOAD, 1), null});
    testData.add(new Object[] {new TestInput(ISTORE, 1), null});
    testData.add(new Object[] {new TestInput(LSTORE, 1), null});
    testData.add(new Object[] {new TestInput(FSTORE, 1), null});
    testData.add(new Object[] {new TestInput(DSTORE, 1), null});
    testData.add(new Object[] {new TestInput(ASTORE, 1), null});
    testData.add(new Object[] {new TestInput(RET, 1), null});

    // Instructions with type argument
    testData.add(new Object[] {new TestInput(NEW, JAVA_LANG_OBJECT), null});
    testData.add(new Object[] {new TestInput(ANEWARRAY, JAVA_LANG_OBJECT), null});
    testData.add(new Object[] {new TestInput(CHECKCAST, JAVA_LANG_OBJECT), null});
    testData.add(new Object[] {new TestInput(INSTANCEOF, JAVA_LANG_OBJECT), null});

    // Instructions with field argument
    testData.add(new Object[] {new TestInput(GETSTATIC, new FieldOrMethod(JAVA_LANG_OBJECT, "field", "")), null});
    testData.add(new Object[] {new TestInput(PUTSTATIC, new FieldOrMethod(JAVA_LANG_OBJECT, "field", "")), null});
    testData.add(new Object[] {new TestInput(GETFIELD, new FieldOrMethod(JAVA_LANG_OBJECT, "field", "")), null});
    testData.add(new Object[] {new TestInput(PUTFIELD, new FieldOrMethod(JAVA_LANG_OBJECT, "field", "")), null});

    // Instructions with method argument
    testData.add(new Object[] {new TestInput(INVOKESPECIAL, new FieldOrMethod(JAVA_LANG_OBJECT, "field", "")), null});
    testData.add(new Object[] {new TestInput(INVOKESTATIC, new FieldOrMethod(JAVA_LANG_OBJECT, "field", "")), null});
    testData.add(new Object[] {new TestInput(INVOKEVIRTUAL, new FieldOrMethod(JAVA_LANG_OBJECT, "field", "")), null});
    testData.add(new Object[] {new TestInput(INVOKEINTERFACE, new FieldOrMethod(JAVA_LANG_OBJECT, "field", "")), null});

    // Jump instructions
    testData.add(new Object[] {new TestInput(IFEQ), null});
    testData.add(new Object[] {new TestInput(IFNE), null});
    testData.add(new Object[] {new TestInput(IFLT), null});
    testData.add(new Object[] {new TestInput(IFGE), null});
    testData.add(new Object[] {new TestInput(IFGT), null});
    testData.add(new Object[] {new TestInput(IFLE), null});
    testData.add(new Object[] {new TestInput(IF_ICMPEQ), null});
    testData.add(new Object[] {new TestInput(IF_ICMPNE), null});
    testData.add(new Object[] {new TestInput(IF_ICMPLT), null});
    testData.add(new Object[] {new TestInput(IF_ICMPGE), null});
    testData.add(new Object[] {new TestInput(IF_ICMPGT), null});
    testData.add(new Object[] {new TestInput(IF_ICMPLE), null});
    testData.add(new Object[] {new TestInput(IF_ACMPEQ), null});
    testData.add(new Object[] {new TestInput(IF_ACMPNE), null});
    testData.add(new Object[] {new TestInput(GOTO), null});
    testData.add(new Object[] {new TestInput(JSR), null});
    testData.add(new Object[] {new TestInput(IFNULL), null});
    testData.add(new Object[] {new TestInput(IFNONNULL), null});

    // The rest
    testData.add(new Object[] {new TestInput(LDC), null});
    testData.add(new Object[] {new TestInput(IINC), null});
    testData.add(new Object[] {new TestInput(INVOKEDYNAMIC), null});
    testData.add(new Object[] {new TestInput(TABLESWITCH), null});
    testData.add(new Object[] {new TestInput(LOOKUPSWITCH), null});
    testData.add(new Object[] {new TestInput(MULTIANEWARRAY), null});

    return testData.build();
  }

  static class TestInput {
    int opcode;
    int operandOrVar;
    String type;
    FieldOrMethod fieldOrMethod;

    @Override
    public String toString() {
      return Printer.OPCODES[opcode];
    }

    TestInput(int opcode) {
      this.opcode = opcode;
    }

    TestInput(int opcode, int operandOrVar) {
      this.opcode = opcode;
      this.operandOrVar = operandOrVar;
    }

    TestInput(int opcode, String type) {
      this.opcode = opcode;
      this.type = type;
    }

    TestInput(int opcode, FieldOrMethod fieldOrMethod) {
      this.opcode = opcode;
      this.fieldOrMethod = fieldOrMethod;
    }
  }

  static class FieldOrMethod {
    String owner;
    String name;
    String desc;

    FieldOrMethod(String owner, String name, String desc) {
      this.owner = owner;
      this.name = name;
      this.desc = desc;
    }
  }

  static class InstructionExpectation {
    BytecodeCFGBuilder.Instruction instruction;

    InstructionExpectation(BytecodeCFGBuilder.Instruction instruction) {
      this.instruction = instruction;
    }
  }

  private TestInput testInput;
  private InstructionExpectation expected;

  @BeforeClass
  public static void verifyTestData() {
    List<Integer> opcodes = data().stream().map(data -> ((TestInput) data[0]).opcode).collect(Collectors.toList());
    assertThat(opcodes).containsAll(Instructions.ASM_OPCODES);
  }

  public BytecodeCFGConstructionTest(TestInput testInput, InstructionExpectation expected) {
    this.testInput = testInput;
    this.expected = expected;
  }

  @Test
  public void test() throws Exception {
    int opcode = testInput.opcode;
    if (isJumpInstruction(opcode)) {
      test_jumps();
    } else {
      test_no_operand();
    }
  }

  private boolean isJumpInstruction(int opcode) {
    return Opcodes.IFEQ <= opcode && opcode <= LOOKUPSWITCH && opcode != RET || opcode==IFNULL || opcode==IFNONNULL;
  }

  private void test_no_operand() {
    BytecodeCFGBuilder.BytecodeCFG cfg = new Instructions().cfg(testInput.opcode);
    assertThat(cfg.blocks.size()).isEqualTo(2);
    int opcode = testInput.opcode;
    if(expected != null) {
      opcode = expected.instruction.opcode();
    }
    assertThat(cfg.blocks.get(1).instructions.get(0).opcode()).isEqualTo(opcode);
  }

  private void test_jumps() {
    BytecodeCFGBuilder.BytecodeCFG cfg = new Instructions().cfg(testInput.opcode);
    if(testInput.opcode == TABLESWITCH) {
      assertThat(cfg.blocks.size()).isEqualTo(5);
    } else if(testInput.opcode == LOOKUPSWITCH) {
      assertThat(cfg.blocks.size()).isEqualTo(3);
    }else {
      // exit block, jump block, jump-to block, other block
      assertThat(cfg.blocks.size()).isEqualTo(4);
      assertThat(cfg.blocks.get(1).instructions).isEmpty();
      assertThat(cfg.blocks.get(1).terminator().opcode()).isEqualTo(testInput.opcode);
      assertThat(cfg.blocks.get(2).instructions.get(0).opcode()).isEqualTo(NOP);
      assertThat(cfg.blocks.get(3).instructions.get(0).opcode()).isEqualTo(ICONST_0);
    }
  }
}
