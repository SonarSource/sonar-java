/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.bytecode.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.sonar.java.bytecode.asm.AsmClassProvider.DETAIL_LEVEL;

public class AsmMethodVisitor extends MethodVisitor {

  private final AsmMethod method;
  private final AsmClassProvider asmClassProvider;
  private int lineNumber = 0;
  private boolean emptyMethod = true;

  public AsmMethodVisitor(AsmMethod method, AsmClassProvider asmClassProvider) {
    super(Opcodes.ASM5);
    this.method = method;
    this.asmClassProvider = asmClassProvider;
    emptyMethod = true;
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescription) {
    AsmClass targetClass = asmClassProvider.getClass(owner, DETAIL_LEVEL.NOTHING);
    AsmField targetField = targetClass.getFieldOrCreateIt(fieldName);
    method.addEdge(new AsmEdge(method, targetField, SourceCodeEdgeUsage.CALLS_FIELD, lineNumber));
    emptyMethod = false;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    if (isNotCallToJavaArrayMethod(owner)) {
      AsmClass targetClass = asmClassProvider.getClass(owner, DETAIL_LEVEL.STRUCTURE);
      AsmMethod targetMethod = targetClass.getMethodOrCreateIt(name + desc);
      method.addEdge(new AsmEdge(method, targetMethod, SourceCodeEdgeUsage.CALLS_METHOD, lineNumber));
    }
    emptyMethod = false;
  }

  private static boolean isNotCallToJavaArrayMethod(String internalName) {
    return internalName.charAt(0) != '[';
  }

  @Override
  public void visitTryCatchBlock(Label start, Label end, Label handler, String exception) {
    if (exception != null) {
      AsmClass exceptionClass = asmClassProvider.getClass(exception, DETAIL_LEVEL.NOTHING);
      method.addEdge(new AsmEdge(method, exceptionClass, SourceCodeEdgeUsage.USES, lineNumber));
    }
    emptyMethod = false;
  }

  @Override
  public void visitTypeInsn(int opcode, String internalName) {
    AsmClass usedClass = asmClassProvider.getClass(internalName, DETAIL_LEVEL.NOTHING);
    method.addEdge(new AsmEdge(method, usedClass, SourceCodeEdgeUsage.USES, lineNumber));
    emptyMethod = false;
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
    lineNumber = line;
  }

  @Override
  public void visitEnd() {
    method.setEmpty(emptyMethod);
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    emptyMethod = false;
  }

  @Override
  public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
    emptyMethod = false;
  }

  @Override
  public void visitInsn(int opcode) {
    if (opcode != Opcodes.RETURN) {
      emptyMethod = false;
    }
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    emptyMethod = false;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    emptyMethod = false;
  }

  @Override
  public void visitLdcInsn(Object cst) {
    if (cst instanceof Type) {
      Type type = (Type) cst;
      AsmClass usedClass = asmClassProvider.getClass(type.getInternalName(), DETAIL_LEVEL.NOTHING);
      method.addEdge(new AsmEdge(method, usedClass, SourceCodeEdgeUsage.USES, lineNumber));
    }
    emptyMethod = false;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    emptyMethod = false;
  }

  @Override
  public void visitMultiANewArrayInsn(String desc, int dims) {
    emptyMethod = false;
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    emptyMethod = false;
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    emptyMethod = false;
  }
}
