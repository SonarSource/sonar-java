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

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.sonar.java.bytecode.se.MethodLookup;
import org.sonar.java.resolve.Flags;

import static org.objectweb.asm.Opcodes.GOTO;

public class BytecodeCFGMethodVisitor extends MethodLookup.LookupMethodVisitor {

  private static final Set<String> SIGNATURE_BLACKLIST = ImmutableSet.of("java.lang.Class#", "java.lang.Object#wait", "java.util.Optional#");

  Map<Label, BytecodeCFG.Block> blockByLabel = new HashMap<>();
  private BytecodeCFG.Block currentBlock;
  private BytecodeCFG cfg;
  private List<TryCatchBlock> tryCatchBlocks = new ArrayList<>();
  private List<TryCatchBlock> currentTryCatches = new ArrayList<>();
  private Map<BytecodeCFG.Block, List<TryCatchBlock>> handlersToWire =  new HashMap<>();

  @Override
  public boolean shouldVisitMethod(int methodFlags, String methodSignature) {
    return isStatic(methodFlags) && !methodIsBlacklisted(methodSignature);
  }

  private static boolean isStatic(int methodFlags) {
    return Flags.isFlagged(methodFlags, Flags.STATIC);
  }

  private static boolean methodIsBlacklisted(String signature) {
    return SIGNATURE_BLACKLIST.stream().anyMatch(signature::startsWith);
  }

  @CheckForNull
  public BytecodeCFG getCfg() {
    return cfg;
  }

  @Override
  public void visitCode() {
    cfg = new BytecodeCFG();
    currentBlock = new BytecodeCFG.Block(cfg);
    cfg.blocks.add(currentBlock);
  }

  @Override
  public void visitInsn(int opcode) {
    currentBlock.addInsn(opcode);
    if ((Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
      currentBlock.successors.add(cfg.blocks.get(0));
      currentBlock = null;
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
      List<BytecodeCFG.Block> successors = new ArrayList<>();
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
    BytecodeCFG.Block previous = currentBlock;
    if (currentBlock == null) {
      // previous instruction was unconditional jump : so create new block or use the one defined for the label
      currentBlock = blockByLabel.computeIfAbsent(label, l -> {
        BytecodeCFG.Block block = new BytecodeCFG.Block(cfg);
        cfg.blocks.add(block);
        return block;
      });
    } else {
      currentBlock = blockByLabel.computeIfAbsent(label, l -> currentBlock.createSuccessor());
      previous.successors.add(currentBlock);
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

    BytecodeCFG.Block blockHandler(Map<Label, BytecodeCFG.Block> blockByLabel) {
      BytecodeCFG.Block blockHandler = blockByLabel.get(handler);
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

}
