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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.objectweb.asm.util.Printer;
import org.sonar.java.cfg.CFG;

public class BytecodeCFG {
  List<Block> blocks;

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

  public List<Block> blocks() {
    return blocks;
  }

  public static class Block implements CFG.IBlock<Instruction> {
    int id;
    BytecodeCFG cfg;
    List<Instruction> instructions;
    List<Block> successors;
    String exceptionType;

    Instruction terminator;
    private Block trueBlock;
    Block falseBlock;
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
}
