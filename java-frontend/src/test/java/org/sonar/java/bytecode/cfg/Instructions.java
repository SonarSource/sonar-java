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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Printer;

import org.sonar.java.resolve.JavaSymbol;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.objectweb.asm.Opcodes.*;

public class Instructions {

  private final ClassWriter cw;
  private final MethodVisitor mv;

  static final ImmutableSet<Integer> NO_OPERAND_INSN = ImmutableSet.of(NOP, ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1,
    ICONST_2, ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1,
    FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, IALOAD,
    LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD,
    IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE,
    SASTORE, POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1,
    DUP2_X2, SWAP, IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB,
    IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM,
    FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL, LSHL, ISHR, LSHR,
    IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR, I2L, I2F, I2D,
    L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S,
    LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IRETURN, LRETURN, FRETURN,
    DRETURN, ARETURN, RETURN, ARRAYLENGTH, ATHROW, MONITORENTER, MONITOREXIT);

  static final ImmutableSet<Integer> INT_INSN = ImmutableSet.of(BIPUSH, SIPUSH, NEWARRAY);
  static final ImmutableSet<Integer> VAR_INSN = ImmutableSet.of(ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, RET);
  static final ImmutableSet<Integer> TYPE_ISNSN = ImmutableSet.of(NEW, ANEWARRAY, CHECKCAST, INSTANCEOF);
  static final ImmutableSet<Integer> FIELD_ISNSN = ImmutableSet.of(GETSTATIC, PUTSTATIC, GETFIELD, PUTFIELD);
  static final ImmutableSet<Integer> METHOD_ISNS = ImmutableSet.of(INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE);
  static final ImmutableSet<Integer> JUMP_ISNS = ImmutableSet.of(IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
    IF_ACMPNE, GOTO, JSR, IFNULL, IFNONNULL);

  static final ImmutableSet<Integer> OTHER_ISNS = ImmutableSet.of(LDC, IINC, TABLESWITCH, LOOKUPSWITCH, MULTIANEWARRAY, INVOKEDYNAMIC);

  static final ImmutableSet<Integer> ALL = ImmutableSet.<Integer>builder()
    .addAll(NO_OPERAND_INSN)
    .addAll(INT_INSN)
    .addAll(VAR_INSN)
    .addAll(TYPE_ISNSN)
    .addAll(FIELD_ISNSN)
    .addAll(METHOD_ISNS)
    .addAll(JUMP_ISNS)
    .addAll(OTHER_ISNS)
    .build();

  static final Set<Integer> ASM_OPCODES = ImmutableSet.copyOf(IntStream.range(0, Printer.OPCODES.length)
    .filter(i -> !Printer.OPCODES[i].isEmpty())
    .boxed()
    .collect(Collectors.toSet()));

  Instructions() {
    cw = new ClassWriter(Opcodes.ASM5);
    cw.visit(V1_8, ACC_PUBLIC, "A", null, "java/lang/Object", null);
    mv = cw.visitMethod(ACC_PUBLIC, "test", "()V", null, null);
  }

  public Instructions visitInsn(int opcode) {
    Preconditions.checkArgument(NO_OPERAND_INSN.contains(opcode));
    mv.visitInsn(opcode);
    return this;
  }

  public Instructions visitIntInsn(int opcode, int operand) {
    Preconditions.checkArgument(INT_INSN.contains(opcode));
    mv.visitIntInsn(opcode, operand);
    return this;
  }

  public Instructions visitVarInsn(int opcode, int var) {
    Preconditions.checkArgument(VAR_INSN.contains(opcode));
    mv.visitVarInsn(opcode, var);
    return this;
  }

  public Instructions visitTypeInsn(int opcode, String type) {
    Preconditions.checkArgument(TYPE_ISNSN.contains(opcode));
    mv.visitTypeInsn(opcode, type);
    return this;
  }

  public Instructions visitFieldInsn(int opcode, String owner, String name, String desc) {
    Preconditions.checkArgument(FIELD_ISNSN.contains(opcode));
    mv.visitFieldInsn(opcode, owner, name, desc);
    return this;
  }

  public Instructions visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    Preconditions.checkArgument(METHOD_ISNS.contains(opcode));
    mv.visitMethodInsn(opcode, owner, name, desc, itf);
    return this;
  }

  public Instructions visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
    mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    return this;
  }

  public Instructions visitJumpInsn(int opcode, Label label) {
    Preconditions.checkArgument(JUMP_ISNS.contains(opcode));
    mv.visitJumpInsn(opcode, label);
    return this;
  }

  public Instructions visitLabel(Label label) {
    mv.visitLabel(label);
    return this;
  }

  public Instructions visitLdcInsn(Object cst) {
    mv.visitLdcInsn(cst);
    return this;
  }

  public Instructions visitIincInsn(int var, int increment) {
    mv.visitIincInsn(var, increment);
    return this;
  }

  public Instructions visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    mv.visitTableSwitchInsn(min, max, dflt, labels);
    return this;
  }

  public Instructions visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    mv.visitLookupSwitchInsn(dflt, keys, labels);
    return this;
  }

  public Instructions visitMultiANewArrayInsn(String desc, int dims) {
    mv.visitMultiANewArrayInsn(desc, dims);
    return this;
  }

  public byte[] bytes() {
    mv.visitEnd();
    cw.visitEnd();
    return cw.toByteArray();
  }

  public BytecodeCFGBuilder.BytecodeCFG cfg() {
    JavaSymbol.MethodJavaSymbol methodStub = new JavaSymbol.MethodJavaSymbol(0, "test", null);
    return BytecodeCFGBuilder.buildCFG(methodStub, bytes());
  }
}
