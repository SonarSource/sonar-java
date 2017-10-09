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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;

import java.util.Objects;

/**
 * Bytecode instruction.
 */
public class Instruction {

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
    Preconditions.checkState(Opcodes.INVOKEVIRTUAL <= opcode && opcode <= Opcodes.INVOKEDYNAMIC, "Not an INVOKE opcode");
    Type methodType = Type.getMethodType(fieldOrMethod.desc);
    return methodType.getArgumentTypes().length;
  }

  public boolean hasReturnValue() {
    Preconditions.checkState(Opcodes.INVOKEVIRTUAL <= opcode && opcode <= Opcodes.INVOKEDYNAMIC, "Not an INVOKE opcode");
    return Type.getMethodType(fieldOrMethod.desc).getReturnType() != Type.VOID_TYPE;
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
}
