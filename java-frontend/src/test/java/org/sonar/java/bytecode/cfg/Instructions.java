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

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.sonar.java.bytecode.cfg.Instruction.FieldOrMethod;

import static org.objectweb.asm.Opcodes.*;
import static org.sonar.java.resolve.BytecodeCompleter.ASM_API_VERSION;

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
  static final ImmutableSet<Integer> TYPE_INSN = ImmutableSet.of(NEW, ANEWARRAY, CHECKCAST, INSTANCEOF);
  static final ImmutableSet<Integer> FIELD_INSN = ImmutableSet.of(GETSTATIC, PUTSTATIC, GETFIELD, PUTFIELD);
  static final ImmutableSet<Integer> METHOD_INSN = ImmutableSet.of(INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE);
  static final ImmutableSet<Integer> JUMP_INSN = ImmutableSet.of(IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
    IF_ACMPNE, GOTO, IFNULL, IFNONNULL);

  static final ImmutableSet<Integer> OTHER_INSN = ImmutableSet.of(LDC, IINC, TABLESWITCH, LOOKUPSWITCH, MULTIANEWARRAY, INVOKEDYNAMIC);

  static final ImmutableSet<Integer> ALL = ImmutableSet.<Integer>builder()
    .addAll(NO_OPERAND_INSN)
    .addAll(INT_INSN)
    .addAll(VAR_INSN)
    .addAll(TYPE_INSN)
    .addAll(FIELD_INSN)
    .addAll(METHOD_INSN)
    .addAll(JUMP_INSN)
    .addAll(OTHER_INSN)
    .build();

  /**
   * List of opCodes variants which are equivalent to more general ones, but with a constant predefined.
   * Collected from 'org.objectweb.asm.Constants'
   */
  static final ImmutableSet<Integer> CONSTANTS = ImmutableSet.<Integer>builder()
    // (I/L/F/D/A)LOAD_X variants
    .addAll(IntStream.rangeClosed(26, 45).boxed().collect(Collectors.toList()))
    // (I/L/F/D/A)STORE_X variants
    .addAll(IntStream.rangeClosed(59, 78).boxed().collect(Collectors.toList()))
    // WIDE
    .add(196)
    // LDC, LDC2, GOTO, JSR wide variants
    .add(19, 20, 200, 201)
    .build();

  static final Set<Integer> OPCODES = IntStream.range(0, 201)
    .filter(i -> !CONSTANTS.contains(i))
    .filter(i -> i != JSR)
    .boxed()
    .collect(Collectors.toSet());

  public Instructions() {
    cw = new ClassWriter(ASM_API_VERSION);
    cw.visit(V1_8, ACC_PUBLIC, "A", null, "java/lang/Object", null);
    mv = cw.visitMethod(ACC_PRIVATE, "test", "()V", null, null);
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
    Preconditions.checkArgument(TYPE_INSN.contains(opcode));
    mv.visitTypeInsn(opcode, type);
    return this;
  }

  public Instructions visitFieldInsn(int opcode, String owner, String name, String desc) {
    Preconditions.checkArgument(FIELD_INSN.contains(opcode));
    mv.visitFieldInsn(opcode, owner, name, desc);
    return this;
  }

  public Instructions visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    Preconditions.checkArgument(METHOD_INSN.contains(opcode));
    mv.visitMethodInsn(opcode, owner, name, desc, itf);
    return this;
  }

  public Instructions visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
    mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    return this;
  }

  public Instructions visitJumpInsn(int opcode, Label label) {
    Preconditions.checkArgument(JUMP_INSN.contains(opcode));
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

  public Instructions visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    mv.visitTryCatchBlock(start, end, handler, type);
    return this;
  }

  public byte[] bytes() {
    mv.visitEnd();
    cw.visitEnd();
    return cw.toByteArray();
  }

  public BytecodeCFG cfg() {
    return getBytecodeCFG(bytes());
  }

  public BytecodeCFG cfg(int opcode) {
    if (NO_OPERAND_INSN.contains(opcode)) {
      visitInsn(opcode);
    } else {
      return cfg(opcode, 1, null, null);
    }
    return cfg();
  }

  public BytecodeCFG cfg(int opcode, int operand, @Nullable String className, @Nullable FieldOrMethod fieldOrMethod) {
    if (NO_OPERAND_INSN.contains(opcode)) {
      visitInsn(opcode);
    } else if (INT_INSN.contains(opcode)) {
      visitIntInsn(opcode, operand);
    } else if (VAR_INSN.contains(opcode)) {
      visitVarInsn(opcode, operand);
    } else if (TYPE_INSN.contains(opcode)) {
      visitTypeInsn(opcode, MoreObjects.firstNonNull(className, "SomeType"));
    } else if (FIELD_INSN.contains(opcode)) {
      if (fieldOrMethod != null) {
        visitFieldInsn(opcode, fieldOrMethod.owner, fieldOrMethod.name, fieldOrMethod.desc);
      } else {
        visitFieldInsn(opcode, "owner", "name", "desc");
      }
    } else if (METHOD_INSN.contains(opcode)) {
      if (fieldOrMethod != null) {
        visitMethodInsn(opcode, fieldOrMethod.owner, fieldOrMethod.name, fieldOrMethod.desc, fieldOrMethod.ownerIsInterface);
      } else {
        visitMethodInsn(opcode, "owner", "name", "()V", false);
      }
    } else if (JUMP_INSN.contains(opcode)) {
      Label label = new Label();
      visitJumpInsn(opcode, label);
      visitLabel(label);
      visitInsn(ICONST_0);
      visitInsn(NOP);
    } else if (OTHER_INSN.contains(opcode)) {
      switch (opcode) {
        case LDC:
          visitLdcInsn("a");
          break;
        case IINC:
          visitIincInsn(operand, 1);
          break;
        case INVOKEDYNAMIC:
          visitInvokeDynamicInsn("sleep", "()Ljava/util/function/Supplier;", new Handle(H_INVOKESTATIC, "", "", "()V", false));
          break;
        case LOOKUPSWITCH: {
          Label l0 = new Label();
          Label l1 = new Label();
          visitLookupSwitchInsn(l1, new int[] {0}, new Label[] {l0});
          visitLabel(l0);
          visitInsn(ICONST_1);
          visitVarInsn(ISTORE, 1);
          Label l2 = new Label();
          visitJumpInsn(GOTO, l2);
          visitLabel(l1);
          visitInsn(ICONST_0);
          visitVarInsn(ISTORE, 1);
          visitLabel(l2);
          visitInsn(RETURN);
        }
        break;
        case TABLESWITCH: {
          Label dflt = new Label();
          Label case0 = new Label();
          visitTableSwitchInsn(0, 1, dflt, case0);
          visitLabel(dflt);
          visitInsn(NOP);
          visitLabel(case0);
          visitInsn(NOP);
        }
          break;
        case MULTIANEWARRAY:
          visitMultiANewArrayInsn("B", 2);
          break;
        default:
          throw new IllegalStateException("unknown opcode " + opcode);
      }

    }
    return cfg();
  }

  static BytecodeCFG getBytecodeCFG(byte[] bytes) {
    ClassReader cr = new ClassReader(bytes);
    BytecodeCFGMethodVisitor cfgMethodVisitor = new BytecodeCFGMethodVisitor();
    cr.accept(new ClassVisitor(ASM_API_VERSION) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return cfgMethodVisitor;
      }
    }, 0);
    return cfgMethodVisitor.getCfg();
  }
}
