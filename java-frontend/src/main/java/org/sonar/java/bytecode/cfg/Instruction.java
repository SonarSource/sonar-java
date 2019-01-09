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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;

import java.util.Objects;
import java.util.Set;

import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DNEG;
import static org.objectweb.asm.Opcodes.DREM;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LALOAD;
import static org.objectweb.asm.Opcodes.LAND;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.LDIV;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LNEG;
import static org.objectweb.asm.Opcodes.LOR;
import static org.objectweb.asm.Opcodes.LREM;
import static org.objectweb.asm.Opcodes.LSHL;
import static org.objectweb.asm.Opcodes.LSHR;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.LUSHR;
import static org.objectweb.asm.Opcodes.LXOR;

/**
 * Bytecode instruction.
 */
public class Instruction {


  private static final Set<Integer> LONG_DOUBLE_OPCODES = ImmutableSet.of(LADD, LSUB, LMUL, LDIV, LAND, LOR, LXOR, LREM, LSHL, LSHR, LUSHR,
    DADD, DSUB, DMUL, DDIV, DREM, DCONST_0, DCONST_1, LCONST_0, LCONST_1, LALOAD, DALOAD, DNEG, LNEG);

  public final int opcode;
  public final Integer operand;
  public final String className;
  public final FieldOrMethod fieldOrMethod;

  @VisibleForTesting
  public Instruction(int opcode, int operand) {
    this.opcode = opcode;
    this.operand = operand;
    this.className = null;
    this.fieldOrMethod = null;
  }

  public Instruction(int opcode) {
    this.opcode = opcode;
    this.operand = null;
    this.className = null;
    this.fieldOrMethod = null;
  }

  public Instruction(int opcode, String className) {
    this.opcode = opcode;
    this.className = className;
    this.operand = null;
    this.fieldOrMethod = null;
  }

  public Instruction(int opcode, FieldOrMethod fieldOrMethod) {
    this.opcode = opcode;
    this.fieldOrMethod = fieldOrMethod;
    this.operand = null;
    this.className = null;
  }

  int opcode() {
    return opcode;
  }


  public int arity() {
    Preconditions.checkState(isInvoke(), "Not an INVOKE opcode");
    Type methodType = Type.getMethodType(fieldOrMethod.desc);
    return methodType.getArgumentTypes().length;
  }

  public boolean hasReturnValue() {
    Preconditions.checkState(isInvoke(), "Not an INVOKE opcode");
    return Type.getMethodType(fieldOrMethod.desc).getReturnType() != Type.VOID_TYPE;
  }

  @VisibleForTesting
  public boolean isInvoke() {
    return Opcodes.INVOKEVIRTUAL <= opcode && opcode <= Opcodes.INVOKEDYNAMIC;
  }

  public boolean isLongOrDoubleValue() {
    if (LONG_DOUBLE_OPCODES.contains(opcode)) {
      return true;
    }
    if (opcode == GETFIELD || opcode == GETSTATIC) {
      return Type.getType(fieldOrMethod.desc).getSize() == 2;
    }
    if (isInvoke()) {
      return Type.getReturnType(fieldOrMethod.desc).getSize() == 2;
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Instruction that = (Instruction) o;
    return opcode == that.opcode &&
      Objects.equals(operand, that.operand) &&
      Objects.equals(className, that.className) &&
      Objects.equals(fieldOrMethod, that.fieldOrMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(opcode, operand, className, fieldOrMethod);
  }

  @Override
  public String toString() {
    return Printer.OPCODES[opcode];
  }

  public static class FieldOrMethod {
    public final String owner;
    public final String name;
    public final String desc;
    public final boolean ownerIsInterface;

    public FieldOrMethod(String owner, String name, String desc, boolean ownerIsInterface) {
      this.owner = owner;
      this.name = name;
      this.desc = desc;
      this.ownerIsInterface = ownerIsInterface;
    }

    public FieldOrMethod(String owner, String name, String desc) {
      this(owner, name, desc, false);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      FieldOrMethod that = (FieldOrMethod) o;
      return ownerIsInterface == that.ownerIsInterface &&
        Objects.equals(owner, that.owner) &&
        Objects.equals(name, that.name) &&
        Objects.equals(desc, that.desc);
    }

    @Override
    public int hashCode() {
      return Objects.hash(owner, name, desc, ownerIsInterface);
    }

    public String completeSignature() {
      return Type.getObjectType(owner).getClassName() + "#" + name + desc;
    }
  }

  public static class MultiANewArrayInsn extends Instruction {

    public final int dim;

    public MultiANewArrayInsn(String className, int dim) {
      super(Opcodes.MULTIANEWARRAY, className);
      this.dim = dim;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      MultiANewArrayInsn that = (MultiANewArrayInsn) o;
      return dim == that.dim;
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), dim);
    }
  }

  public static class InvokeDynamicInsn extends Instruction {

    private final String desc;

    public InvokeDynamicInsn(String desc) {
      super(Opcodes.INVOKEDYNAMIC);
      this.desc = desc;
    }

    @Override
    public int arity() {
      Type methodType = Type.getMethodType(desc);
      return methodType.getArgumentTypes().length;
    }

    @Override
    public boolean hasReturnValue() {
      return Type.getMethodType(desc).getReturnType() != Type.VOID_TYPE;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      InvokeDynamicInsn that = (InvokeDynamicInsn) o;
      return Objects.equals(desc, that.desc);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), desc);
    }
  }

  public static class LdcInsn extends Instruction {

    public final Object cst;

    public LdcInsn(Object cst) {
      super(Opcodes.LDC);
      this.cst = cst;
    }

    @Override
    public boolean isLongOrDoubleValue() {
      return cst instanceof Long || cst instanceof Double;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      LdcInsn ldcInsn = (LdcInsn) o;
      return Objects.equals(cst, ldcInsn.cst);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), cst);
    }
  }
}
