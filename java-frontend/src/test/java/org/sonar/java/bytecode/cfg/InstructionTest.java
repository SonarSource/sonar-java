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
package org.sonar.java.bytecode.cfg;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static org.assertj.core.api.Assertions.assertThat;

public class InstructionTest {

  @Test
  public void test_instruction_equals() throws Exception {
    Instruction nop1 = new Instruction(Opcodes.NOP);
    Instruction nop2 = new Instruction(Opcodes.NOP);
    Instruction.MultiANewArrayInsn mana = new Instruction.MultiANewArrayInsn("java/lang/Object", 1);

    assertThat(nop1.equals(nop1)).isTrue();
    assertThat(nop1.equals(nop2)).isTrue();
    assertThat(nop1.equals(null)).isFalse();
    assertThat(nop1.equals(mana)).isFalse();
    assertThat(nop1.hashCode()).isEqualTo(nop2.hashCode());

    Instruction aload0 = new Instruction(Opcodes.ALOAD, 0);
    assertThat(aload0).isNotEqualTo(nop1);

    Instruction aload1 = new Instruction(Opcodes.ALOAD, 1);
    assertThat(aload0).isNotEqualTo(aload1);
  }

  @Test
  public void test_multianewarray() throws Exception {
    Instruction.MultiANewArrayInsn mana1 = new Instruction.MultiANewArrayInsn("java/lang/Object", 1);
    Instruction.MultiANewArrayInsn mana2 = new Instruction.MultiANewArrayInsn("java/lang/Object", 1);
    Instruction.MultiANewArrayInsn mana3 = new Instruction.MultiANewArrayInsn("java/lang/Object", 2);
    Instruction.MultiANewArrayInsn mana4 = new Instruction.MultiANewArrayInsn("java/lang/String", 2);
    assertThat(mana1.equals(mana1)).isTrue();
    assertThat(mana1.equals(mana2)).isTrue();
    assertThat(mana1.hashCode()).isEqualTo(mana2.hashCode());

    assertThat(mana1.equals(new Instruction(Opcodes.NOP))).isFalse();
    assertThat(mana1.equals(null)).isFalse();
    assertThat(mana1.equals(mana3)).isFalse();
    assertThat(mana1.equals(mana4)).isFalse();
  }

  @Test
  public void test_invoke_dynamic_equals() throws Exception {
    Instruction.InvokeDynamicInsn indy1 = new Instruction.InvokeDynamicInsn("()V");
    assertThat(indy1.equals(indy1)).isTrue();
    assertThat(indy1.equals(null)).isFalse();
    assertThat(indy1.equals(new Instruction(Opcodes.NOP))).isFalse();

    Instruction.InvokeDynamicInsn indy2 = new Instruction.InvokeDynamicInsn("()V");
    assertThat(indy1).isEqualTo(indy2);
    assertThat(indy1.hashCode()).isEqualTo(indy2.hashCode());

    Instruction.InvokeDynamicInsn indy3 = new Instruction.InvokeDynamicInsn("()Ljava/util/function/Consumer;");
    assertThat(indy2).isNotEqualTo(indy3);
  }

  @Test
  public void test_ldc_equals() throws Exception {
    Instruction nop = new Instruction(Opcodes.NOP);
    Instruction.LdcInsn ldc1 = new Instruction.LdcInsn("a");
    Instruction.LdcInsn ldc2 = new Instruction.LdcInsn("a");
    Instruction.LdcInsn ldc3 = new Instruction.LdcInsn(1L);
    assertThat(ldc1.equals(ldc1)).isTrue();
    assertThat(ldc1.equals(null)).isFalse();
    assertThat(ldc1.equals(nop)).isFalse();
    assertThat(ldc1.equals(ldc2)).isTrue();
    assertThat(ldc1.equals(ldc3)).isFalse();
    assertThat(ldc1.hashCode()).isEqualTo(ldc2.hashCode());
  }

  @Test
  public void test_instruction_tostring() throws Exception {
    Instruction nop = new Instruction(Opcodes.NOP);
    assertThat(nop).hasToString("NOP");
  }
}
