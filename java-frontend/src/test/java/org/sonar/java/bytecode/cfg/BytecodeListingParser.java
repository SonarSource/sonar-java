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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.Printer;
import org.sonar.java.resolve.JavaSymbol;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.IINC;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.sonar.java.bytecode.cfg.Instructions.FIELD_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.INT_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.JUMP_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.METHOD_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.NO_OPERAND_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.TYPE_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.VAR_INSN;
import static org.sonar.java.resolve.BytecodeCompleter.ASM_API_VERSION;

public class BytecodeListingParser {


  public static BytecodeCFG getCFG(String bytecodeInstructions) {
    // Define class and method stub for instructions
    ClassWriter cw = new ClassWriter(ASM_API_VERSION);
    cw.visit(V1_8, ACC_PUBLIC, "A", null, "java/lang/Object", null);
    MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "test", "()V", null, null);
    JavaSymbol.MethodJavaSymbol methodStub = new JavaSymbol.MethodJavaSymbol(0, "test", null);
    Map<Integer, Label> labelIndexes =  new HashMap<>();
    String[] lines = bytecodeInstructions.split("\n");
    for (String line : lines) {
      visitLine(line.trim().split(" "), mv, labelIndexes);
    }
    mv.visitEnd();
    cw.visitEnd();
    byte[] bytes = cw.toByteArray();
    return Instructions.getBytecodeCFG(bytes);
  }

  private static void visitLine(String[] words, MethodVisitor mv, Map<Integer, Label> labelIndexes) {
    // labels :
    String initWord = words[0];
    if(initWord.matches("L\\d+")) {
      mv.visitLabel(labelIndexes.computeIfAbsent(Integer.parseInt(initWord.substring(1)), k -> new Label()));
    }

    int opcode = Arrays.asList(Printer.OPCODES).indexOf(initWord);
    if(opcode == -1) {
      return;
    }
    if(NO_OPERAND_INSN.contains(opcode)) {
      mv.visitInsn(opcode);
    } else if (INT_INSN.contains(opcode)) {
      mv.visitIntInsn(opcode, Integer.parseInt(words[1]));
    } else if (VAR_INSN.contains(opcode)) {
      mv.visitVarInsn(opcode, Integer.parseInt(words[1]));
    } else if (TYPE_INSN.contains(opcode)) {
      mv.visitTypeInsn(opcode, words[1]);
    } else if (FIELD_INSN.contains(opcode)) {
      mv.visitFieldInsn(opcode, words[1], words[2], words[3]);
    } else if (METHOD_INSN.contains(opcode)) {
      // FIXME: interface flag is hardcoded.
      mv.visitMethodInsn(opcode,  words[1], words[2], words[3], false);
    } else if (JUMP_INSN.contains(opcode)) {
      mv.visitJumpInsn(opcode, labelIndexes.computeIfAbsent(Integer.parseInt(words[1].substring(1)), k -> new Label()));
    } else if(opcode == IINC) {
      mv.visitIincInsn(Integer.parseInt(words[1]), Integer.parseInt(words[2]));
    }

  }
}
