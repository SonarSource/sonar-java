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
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.cfg.CFG;
import org.sonar.java.resolve.Convert;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.Java9Support;

import javax.annotation.CheckForNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKEDYNAMIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.JSR;

public class BytecodeCFGBuilder {

  private BytecodeCFGBuilder() {
  }

  public static BytecodeCFG buildCFG(String signature, SquidClassLoader classLoader) {
    try(InputStream is = classLoader.getResourceAsStream(Convert.bytecodeName(signature.substring(0, signature.indexOf('#'))) + ".class")) {
      byte[] bytes = ByteStreams.toByteArray(is);
      // to read bytecode with ASM not supporting Java 9, we will set major version to Java 8
      if (Java9Support.isJava9Class(bytes)) {
        Java9Support.setJava8MajorVersion(bytes);
      }
      return buildCFG(signature, bytes);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @VisibleForTesting
  static BytecodeCFG buildCFG(String sign, byte[] bytes) {
    ClassReader cr = new ClassReader(bytes);
    BytecodeCFGMethodVisitor methodVisitor = new BytecodeCFGMethodVisitor();
    cr.accept(new ClassVisitor(Opcodes.ASM5) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals(sign.substring(sign.indexOf('#') + 1, sign.indexOf('('))) && desc.equals(sign.substring(sign.indexOf('(')))) {
          methodVisitor.isStaticMethod = (access & Flags.STATIC) != 0;
          methodVisitor.isVarArgs = (access & Flags.VARARGS) != 0;
          return methodVisitor;
        }
        return null;
      }
    }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return methodVisitor.cfg();
  }

  public static class BytecodeCFG {
    List<Block> blocks;
    boolean isStaticMethod;
    boolean isVarArgs;

    BytecodeCFG() {
      blocks = new ArrayList<>();
      // create exit block
      blocks.add(new Block(this));
    }

    public CFG.IBlock<Instruction> entry() {
      return blocks.get(1);
    }

    public boolean isStaticMethod() {
      return isStaticMethod;
    }

    public boolean isVarArgs() {
      return isVarArgs;
    }

    public Block exitBlock() {
      return blocks.get(0);
    }
  }
  public static class Block implements CFG.IBlock<Instruction> {
    int id;
    BytecodeCFG cfg;
    List<Instruction> instructions;
    List<Block> successors;
    Instruction terminator;
    private Block trueBlock;
    private Block falseBlock;

    Block(BytecodeCFG cfg) {
      this.cfg = cfg;
      this.id = cfg.blocks.size();
      instructions = new ArrayList<>();
      successors = new ArrayList<>();
    }

    void addInsn(int opcode) {
      instructions.add(new Instruction(opcode));
    }

    void addInsn(int opcode, int operand) {
      instructions.add(new Instruction(opcode, operand));
    }

    void addInsn(int opcode, String className) {
      instructions.add(new Instruction(opcode, className));
    }

    void addInsn(int opcode, Instruction.FieldOrMethod fieldOrMethod) {
      instructions.add(new Instruction(opcode, fieldOrMethod));
    }

    Block createSuccessor() {
      Block newBlock = new Block(cfg);
      successors.add(newBlock);
      cfg.blocks.add(newBlock);
      return newBlock;
    }

    public Block trueSuccessor() {
      return trueBlock;
    }

    public Block falseSuccessor() {
      return falseBlock;
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
      if (terminator != null) {
        sb.append(Printer.OPCODES[terminator.opcode]).append(" ");
      }
      sb.append("Jumps to: ");
      successors.forEach(s -> {
        sb.append("B").append(s.id);
        if(s == trueBlock) {
          sb.append("(true)");
        }
        if(s == falseBlock) {
          sb.append("(false)");
        }
        sb.append(" ");
      });
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

    @CheckForNull
    @Override
    public Instruction terminator() {
      return terminator;
    }

    @Override
    public Set<Block> successors() {
      return new HashSet<>(successors);
    }

    void setTrueBlock(Block trueBlock) {
      this.trueBlock = trueBlock;
      if(!successors.contains(trueBlock)) {
        successors.add(trueBlock);
      }
    }
  }

  private static class BytecodeCFGMethodVisitor extends MethodVisitor {
    Map<Label, Block> blockByLabel = new HashMap<>();
    private Block currentBlock;
    private BytecodeCFG cfg;
    private boolean isStaticMethod;
    private boolean isVarArgs;

    BytecodeCFGMethodVisitor() {
      super(Opcodes.ASM5);
      cfg = new BytecodeCFG();
      currentBlock = new Block(cfg);
      cfg.blocks.add(currentBlock);
    }

    @Override
    public void visitInsn(int opcode) {
      currentBlock.addInsn(opcode);
      if ((Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
        currentBlock.successors.add(cfg.blocks.get(0));
      }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
      currentBlock.addInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      currentBlock.addInsn(opcode, var);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      currentBlock.addInsn(opcode);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      currentBlock.addInsn(opcode, new BytecodeCFGBuilder.Instruction.FieldOrMethod(owner, name, desc, itf));
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
      currentBlock.terminator = new Instruction(Opcodes.TABLESWITCH);
      blockByLabel.computeIfAbsent(dflt, l -> currentBlock.createSuccessor());
      for (Label label : labels) {
        blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor());
      }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      currentBlock.terminator = new Instruction(Opcodes.LOOKUPSWITCH);
      for (Label label : labels) {
        blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor());
      }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
      currentBlock.addInsn(opcode, Type.getObjectType(type).getClassName());
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
      if (opcode == GOTO || opcode == JSR) {
        currentBlock.terminator = new Instruction(opcode);
        currentBlock.successors = Collections.singletonList(blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor()));
        currentBlock = null;
        return;
      }
      currentBlock.setTrueBlock(blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor()));
      currentBlock.terminator = new Instruction(opcode);
      currentBlock.falseBlock = currentBlock.createSuccessor();
      currentBlock = currentBlock.falseBlock;
    }

    @Override
    public void visitLabel(Label label) {
      Block previous = currentBlock;
      if (currentBlock == null) {
        // previous instruction was unconditional jump : so create new block or use the one defined for the label
        currentBlock = blockByLabel.computeIfAbsent(label, l -> {
          Block block = new Block(cfg);
          cfg.blocks.add(block);
          return block;
        });
      } else {
        currentBlock = blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor());
        if (previous.successors.isEmpty()) {
          previous.successors.add(currentBlock);
        }
      }
    }

    @Override
    public void visitEnd() {
      // if last block ends up with no successors, it is returning or throwing, link it to exit block.
      if(currentBlock.successors.isEmpty()) {
        currentBlock.successors.add(cfg.blocks.get(0));
      }
    }

    public BytecodeCFG cfg() {
      cfg.isStaticMethod = isStaticMethod;
      cfg.isVarArgs = isVarArgs;
      return cfg;
    }
  }

  /**
   * Bytecode instruction.
   */
  public static class Instruction {

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
      Preconditions.checkState(INVOKEVIRTUAL <= opcode && opcode <= INVOKEDYNAMIC, "Not an INVOKE opcode");
      Type methodType = Type.getMethodType(fieldOrMethod.desc);
      return methodType.getArgumentTypes().length;
    }

    public boolean hasReturnValue() {
      Preconditions.checkState(INVOKEVIRTUAL <= opcode && opcode <= INVOKEDYNAMIC, "Not an INVOKE opcode");
      return Type.getMethodType(fieldOrMethod.desc).getReturnType() == Type.VOID_TYPE;
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
  }
}
