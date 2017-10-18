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
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.util.Printer;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.cfg.CFG;
import org.sonar.java.resolve.Convert;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.Java9Support;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.GOTO;

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
    cr.accept(new BytecodeCFGClassVisitor(methodVisitor, sign), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return methodVisitor.cfg();
  }

  public static class BytecodeCFG {
    List<Block> blocks;
    boolean isStaticMethod;
    boolean isVarArgs;
    boolean isOverrideableOrNativeMethod;

    BytecodeCFG() {
      blocks = new ArrayList<>();
      // create exit block
      Block exit = new Block(this);
      exit.successors = Collections.emptyList();
      blocks.add(exit);
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

    public boolean isOverrideableOrNativeMethod() {
      return isOverrideableOrNativeMethod;
    }

    public List<Block> blocks() {
      return blocks;
    }
  }
  public static class Block implements CFG.IBlock<Instruction> {
    int id;
    BytecodeCFG cfg;
    List<Instruction> instructions;
    List<Block> successors;
    String exceptionType;

    Instruction terminator;
    private Block trueBlock;
    private Block falseBlock;
    Block(BytecodeCFG cfg) {
      this.cfg = cfg;
      this.id = cfg.blocks.size();
      instructions = new ArrayList<>();
      successors = new ArrayList<>();
    }

    public String getExceptionType() {
      return exceptionType;
    }

    public boolean isUncaughtException() {
      return exceptionType != null && exceptionType.charAt(0) == '!';
    }

    void addInsn(Instruction insn) {
      instructions.add(insn);
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
      successors().stream().sorted(Comparator.comparingInt(s -> s.id)).forEachOrdered(s -> {
        sb.append("B").append(s.id);
        if(s == trueBlock) {
          sb.append("(true)");
        }
        if(s == falseBlock) {
          sb.append("(false)");
        }
        if(s.exceptionType != null) {
          sb.append("(Exception:").append(s.exceptionType).append(")");
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

  private static class BytecodeCFGClassVisitor extends ClassVisitor {

    private final BytecodeCFGMethodVisitor methodVisitor;
    private final String methodSignature;
    private boolean isFinalClass = false;

    public BytecodeCFGClassVisitor(BytecodeCFGMethodVisitor methodVisitor, String targetedMethodSignatures) {
      super(Opcodes.ASM5);
      this.methodVisitor = methodVisitor;
      this.methodSignature = targetedMethodSignatures;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      isFinalClass = Flags.isFlagged(Flags.filterAccessBytecodeFlags(access), Flags.FINAL);
      super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if (name.equals(methodSignature.substring(methodSignature.indexOf('#') + 1, methodSignature.indexOf('(')))
        && desc.equals(methodSignature.substring(methodSignature.indexOf('(')))) {
        methodVisitor.isStaticMethod = Flags.isFlagged(access, Flags.STATIC);
        methodVisitor.isVarArgs = Flags.isFlagged(access, Flags.VARARGS);
        methodVisitor.isOverrideableOrNativeMethod = isOverrideableOrNativeMethod(access);
        return new JSRInlinerAdapter(methodVisitor, access, name, desc, signature, exceptions);
      }
      return null;
    }

    private boolean isOverrideableOrNativeMethod(int methodFlags) {
      if (Flags.isFlagged(methodFlags, Flags.NATIVE)) {
        return true;
      }
      return Flags.isFlagged(methodFlags, Flags.ABSTRACT) || !(isFinalClass || Flags.isFlagged(methodFlags, Flags.PRIVATE | Flags.FINAL | Flags.STATIC));
    }
  }

  private static class BytecodeCFGMethodVisitor extends MethodVisitor {
    Map<Label, Block> blockByLabel = new HashMap<>();
    private Block currentBlock;
    private BytecodeCFG cfg;
    private boolean isStaticMethod;
    private boolean isVarArgs;
    private boolean isOverrideableOrNativeMethod;
    private List<TryCatchBlock> tryCatchBlocks = new ArrayList<>();
    private List<TryCatchBlock> currentTryCatches = new ArrayList<>();
    private Map<Block, List<TryCatchBlock>> handlersToWire =  new HashMap<>();

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
      currentBlock.addInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      currentBlock.addInsn(opcode, var);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      currentBlock.addInsn(opcode, new Instruction.FieldOrMethod(owner, name, desc));
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      currentBlock.addInsn(opcode, new Instruction.FieldOrMethod(owner, name, desc, itf));
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
      currentBlock.addInsn(new Instruction.InvokeDynamicInsn(desc));
    }

    @Override
    public void visitLdcInsn(Object cst) {
      currentBlock.addInsn(new Instruction.LdcInsn(cst));
    }

    @Override
    public void visitIincInsn(int var, int increment) {
      currentBlock.addInsn(Opcodes.IINC, var);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
      currentBlock.addInsn(new Instruction.MultiANewArrayInsn(desc, dims));
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
      blockByLabel.computeIfAbsent(dflt, l -> currentBlock.createSuccessor());
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
      if (opcode == GOTO) {
        currentBlock.terminator = new Instruction(opcode);
        List<Block> successors = new ArrayList<>();
        successors.add(blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor()));
        currentBlock.successors = successors;
        currentBlock = null;
        return;
      }
      currentBlock.setTrueBlock(blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor()));
      currentBlock.terminator = new Instruction(opcode);
      currentBlock.falseBlock = currentBlock.createSuccessor();
      currentBlock = currentBlock.falseBlock;
      handlersToWire.put(currentBlock, new ArrayList<>(currentTryCatches));
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
      currentTryCatches.addAll(handlersStartingWith(label));
      currentTryCatches.removeAll(handlersEndingWith(label));
      handlersToWire.put(currentBlock, new ArrayList<>(currentTryCatches));
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, @Nullable String type) {
      String exception = type == null ? null : Type.getObjectType(type).getClassName();
      tryCatchBlocks.add(new TryCatchBlock(start, end, handler, exception));
    }

    private static class TryCatchBlock {
      private final Label start;
      private final Label end;
      private final Label handler;
      @Nullable
      private final String type;

      TryCatchBlock(Label start, Label end, Label handler, @Nullable String type) {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.type = type;
      }

      Block blockHandler(Map<Label, Block> blockByLabel) {
        Block blockHandler = blockByLabel.get(handler);
        String exType = type;
        if(exType == null) {
          exType = "!UncaughtException!";
        }
        blockHandler.exceptionType = exType;
        return blockHandler;
      }
    }
    @Override
    public void visitEnd() {
      // if the last block ends with GOTO, currentBlock will be null
      if (currentBlock != null
        // if last block ends up with no successors, it is returning or throwing, link it to exit block.
        && currentBlock.successors.isEmpty()) {
        currentBlock.successors.add(cfg.blocks.get(0));
      }
      handlersToWire.forEach((b, tcbs) -> tcbs.forEach(tcb -> b.successors.add(tcb.blockHandler(blockByLabel))));
    }

    private List<TryCatchBlock> handlersStartingWith(Label label) {
      return tryCatchBlocks.stream().filter(h -> h.start == label).collect(Collectors.toList());
    }

    private List<TryCatchBlock> handlersEndingWith(Label label) {
      return tryCatchBlocks.stream().filter(h -> h.end == label).collect(Collectors.toList());
    }

    public BytecodeCFG cfg() {
      cfg.isStaticMethod = isStaticMethod;
      cfg.isVarArgs = isVarArgs;
      cfg.isOverrideableOrNativeMethod = isOverrideableOrNativeMethod;
      return cfg;
    }
  }

}
