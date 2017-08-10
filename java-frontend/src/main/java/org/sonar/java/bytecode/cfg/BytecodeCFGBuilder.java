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

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Printer;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.cfg.CFG;
import org.sonar.java.resolve.Convert;
import org.sonar.java.resolve.Java9Support;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BytecodeCFGBuilder {

  private BytecodeCFGBuilder() {
  }

  public static BytecodeCFG buildCFG(Symbol.MethodSymbol methodSymbol, SquidClassLoader classLoader) {
    try(InputStream is = classLoader.getResourceAsStream(Convert.bytecodeName(((JavaSymbol.TypeJavaSymbol) methodSymbol.owner()).getFullyQualifiedName()) + ".class")) {
      byte[] bytes = ByteStreams.toByteArray(is);
      // to read bytecode with ASM not supporting Java 9, we will set major version to Java 8
      if (Java9Support.isJava9Class(bytes)) {
        Java9Support.setJava8MajorVersion(bytes);
      }
      ClassReader cr = new ClassReader(bytes);
      BytecodeCFGMethodVisitor methodVisitor = new BytecodeCFGMethodVisitor(methodSymbol);
      cr.accept(new ClassVisitor(Opcodes.ASM5) {
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
          // TODO : name matching is not sufficient in case of overloading.
          if (name.equals(methodSymbol.name())) {
            return methodVisitor;
          }
          return null;
        }
      }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      return methodVisitor.cfg;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public static class BytecodeCFG {
    public List<Block> blocks;
    Symbol.MethodSymbol methodSymbol;

    BytecodeCFG(Symbol.MethodSymbol methodSymbol) {
      this.methodSymbol = methodSymbol;
      blocks = new ArrayList<>();
      // create exit block
      blocks.add(new Block(this));
    }

    public CFG.IBlock<Instruction> entry() {
      return blocks.get(1);
    }
  }
  public static class Block implements CFG.IBlock<Instruction> {
    int id;
    BytecodeCFG cfg;
    List<Instruction> instructions;
    List<Block> successors;

    Block(BytecodeCFG cfg) {
      this.cfg = cfg;
      this.id = cfg.blocks.size();
      instructions = new ArrayList<>();
      successors = new ArrayList<>();
    }

    void addInsn(int opcode) {
      instructions.add(new Instruction(opcode));
    }

    Block createSuccessor() {
      Block newBlock = new Block(cfg);
      successors.add(newBlock);
      cfg.blocks.add(newBlock);
      return newBlock;
    }

    public String printBlock() {
      StringBuilder sb = new StringBuilder();
      sb.append("B").append(id);
      if (id == 0) {
        sb.append("(Exit)\n");
        return sb.toString();
      }
      sb.append("\n");
      int index = 0;
      for (Instruction instruction : instructions) {
        sb.append(index).append(": ").append(Printer.OPCODES[instruction.opcode]).append("\n");
        index++;
      }
      sb.append("Jumps to: ");
      successors.forEach(s -> sb.append("B").append(s.id).append(" "));
      sb.append("\n");
      return sb.toString();
    }

    @Override
    public int id() {
      return id;
    }

    @Override
    public List<Instruction> elements() {
      return instructions;
    }

    @Override
    public Set<Block> successors() {
      return new HashSet<>(successors);
    }
  }

  private static class BytecodeCFGMethodVisitor extends MethodVisitor {
    Map<Label, Block> blockByLabel = new HashMap<>();
    private Block currentBlock;
    private BytecodeCFG cfg;

    BytecodeCFGMethodVisitor(Symbol.MethodSymbol methodSymbol) {
      super(Opcodes.ASM5);
      cfg = new BytecodeCFG(methodSymbol);
      currentBlock = new Block(cfg);
      cfg.blocks.add(currentBlock);
    }

    @Override
    public void visitInsn(int opcode) {
      currentBlock.addInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
      currentBlock.addInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      currentBlock.addInsn(opcode);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      currentBlock.addInsn(opcode);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      currentBlock.addInsn(opcode);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
      currentBlock.addInsn(Opcodes.INVOKEDYNAMIC);
    }

    @Override
    public void visitLdcInsn(Object cst) {
      // FIXME (npe) LDC is generic information about constant but the real value of constant could be loaded into instructions
      currentBlock.addInsn(Opcodes.LDC);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
      currentBlock.addInsn(Opcodes.IINC);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
      currentBlock.addInsn(Opcodes.MULTIANEWARRAY);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
      blockByLabel.computeIfAbsent(dflt, l -> currentBlock.createSuccessor());
      for (Label label : labels) {
        blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor());
      }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      for (Label label : labels) {
        blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor());
      }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
      currentBlock.addInsn(opcode);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
      blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor());
      currentBlock = currentBlock.createSuccessor();
    }

    @Override
    public void visitLabel(Label label) {
      currentBlock = blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor());
    }

    @Override
    public void visitEnd() {
      // if a block ends up with no successors, it is returning or throwing, link it to exit block.
      cfg.blocks.stream().filter(b -> b.successors.isEmpty() && b.id != 0).forEach(b -> b.successors.add(cfg.blocks.get(0)));
    }
  }

  /**
   * Bytecode instruction.
   */
  public static class Instruction {

    public final int opcode;

    Instruction(int opcode) {
      this.opcode = opcode;
    }
  }
}
