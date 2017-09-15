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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;

import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.cfg.testdata.CFGTestData;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.Convert;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.NOP;
import static org.sonar.java.bytecode.cfg.Instructions.FIELD_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.INT_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.JUMP_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.METHOD_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.NO_OPERAND_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.TYPE_INSN;
import static org.sonar.java.bytecode.cfg.Instructions.VAR_INSN;

public class BytecodeCFGBuilderTest {

  @Test
  public void test() throws Exception {
    SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    File file = new File("src/test/java/org/sonar/java/bytecode/cfg/BytecodeCFGBuilderTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, squidClassLoader);
    Symbol.MethodSymbol symbol = ((MethodTree) ((ClassTree) ((ClassTree) tree.types().get(0)).members().get(1)).members().get(0)).symbol();
    BytecodeCFGBuilder.BytecodeCFG cfg = BytecodeCFGBuilder.buildCFG(((JavaSymbol.MethodJavaSymbol) symbol).completeSignature(), squidClassLoader);
    StringBuilder sb = new StringBuilder();
    cfg.blocks.forEach(b-> sb.append(b.printBlock()));
    assertThat(sb.toString()).isEqualTo(
     "B0(Exit)\n" +
       "B1\n" +
       "0: ILOAD\n" +
       "IFEQ Jumps to: B2(true) B3(false) \n" +
       "B2\n" +
       "0: LDC\n" +
       "1: ARETURN\n" +
       "Jumps to: B0 \n" +
       "B3\n" +
       "0: ALOAD\n" +
       "IFNONNULL Jumps to: B4(true) B5(false) \n" +
       "B4\n" +
       "0: LDC\n" +
       "1: ARETURN\n" +
       "Jumps to: B0 \n" +
       "B5\n" +
       "0: ACONST_NULL\n" +
       "1: ARETURN\n" +
       "Jumps to: B0 \n");
  }

  static class InnerClass {
    Object fun(boolean a, Object b) {
      if (a) {
        if (b == null) {
          return null;
        }
        return "";
      } else {
        return "not a";
      }
    }
    boolean label_goto(Object b) {
      return  b == null;
    }
  }

  @Test
  public void label_goto_successors() throws Exception {
    SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    File file = new File("src/test/java/org/sonar/java/bytecode/cfg/BytecodeCFGBuilderTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, squidClassLoader);
    Symbol.MethodSymbol symbol = ((MethodTree) ((ClassTree) ((ClassTree) tree.types().get(0)).members().get(1)).members().get(1)).symbol();
    BytecodeCFGBuilder.BytecodeCFG cfg = BytecodeCFGBuilder.buildCFG(((JavaSymbol.MethodJavaSymbol) symbol).completeSignature(), squidClassLoader);
    StringBuilder sb = new StringBuilder();
    cfg.blocks.forEach(b-> sb.append(b.printBlock()));
    assertThat(sb.toString()).isEqualTo("B0(Exit)\n" +
      "B1\n" +
      "0: ALOAD\n" +
      "IFNONNULL Jumps to: B2(true) B3(false) \n" +
      "B2\n" +
      "0: ICONST_0\n" +
      "Jumps to: B4 \n" +
      "B3\n" +
      "0: ICONST_1\n" +
      "GOTO Jumps to: B4 \n" +
      "B4\n" +
      "0: IRETURN\n" +
      "Jumps to: B0 \n");
  }

  @Test
  public void test_all_instructions_are_part_of_CFG() throws Exception {
    SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    File file = new File("src/test/java/org/sonar/java/bytecode/cfg/testdata/CFGTestData.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, squidClassLoader);
    Symbol.TypeSymbol testClazz = ((ClassTree) tree.types().get(0)).symbol();
    ClassReader cr = new ClassReader(squidClassLoader.getResourceAsStream(Convert.bytecodeName(CFGTestData.class.getCanonicalName()) + ".class"));
    ClassNode classNode = new ClassNode(Opcodes.ASM5);
    cr.accept(classNode, 0);
    for (MethodNode method : classNode.methods) {
      Multiset<String> opcodes = Arrays.stream(method.instructions.toArray())
        .map(AbstractInsnNode::getOpcode)
        .filter(opcode -> opcode != -1)
        .map(opcode -> Printer.OPCODES[opcode])
        .collect(Collectors.toCollection(HashMultiset::create));

      Symbol methodSymbol = Iterables.getOnlyElement(testClazz.lookupSymbols(method.name));
      BytecodeCFGBuilder.BytecodeCFG bytecodeCFG = BytecodeCFGBuilder.buildCFG(((JavaSymbol.MethodJavaSymbol) methodSymbol).completeSignature(), squidClassLoader);
      Multiset<String> cfgOpcodes = cfgOpcodes(bytecodeCFG);
      assertThat(cfgOpcodes).isEqualTo(opcodes);
    }
  }

  private Multiset<String> cfgOpcodes(BytecodeCFGBuilder.BytecodeCFG bytecodeCFG) {
    return bytecodeCFG.blocks.stream()
          .flatMap(block -> Stream.concat(block.instructions.stream(), Stream.of(block.terminator)))
          .filter(Objects::nonNull)
          .map(BytecodeCFGBuilder.Instruction::opcode)
          .map(opcode -> Printer.OPCODES[opcode])
          .collect(Collectors.toCollection(HashMultiset::create));
  }

  @Test
  public void all_opcodes_should_be_visited() throws Exception {
    Instructions ins = new Instructions();
    NO_OPERAND_INSN.forEach(ins::visitInsn);
    INT_INSN.forEach(i -> ins.visitIntInsn(i, 0));
    VAR_INSN.forEach(i -> ins.visitVarInsn(i, 0));
    TYPE_INSN.forEach(i -> ins.visitTypeInsn(i, "java/lang/Object"));
    FIELD_INSN.forEach(i -> ins.visitFieldInsn(i, "java/lang/Object", "foo", "D(D)"));
    METHOD_INSN.forEach(i -> ins.visitMethodInsn(i, "java/lang/Object", "foo", "()V", i == INVOKEINTERFACE));

    JUMP_INSN.forEach(i -> {
      Label jumpLabel = new Label();
      ins.visitJumpInsn(i, jumpLabel);
      ins.visitLabel(jumpLabel);
    });

    ins.visitLdcInsn("a");
    ins.visitIincInsn(0, 1);
    Handle handle = new Handle(H_INVOKESTATIC, "", "", "()V", false);
    ins.visitInvokeDynamicInsn("sleep", "()V", handle);
    ins.visitLookupSwitchInsn(new Label(), new int[] {}, new Label[] {});
    ins.visitMultiANewArrayInsn("B", 1);

    Label l0 = new Label();
    Label dflt = new Label();
    Label case0 = new Label();
    ins.visitTableSwitchInsn(0, 1, dflt, case0);
    ins.visitLabel(dflt);
    ins.visitInsn(NOP);
    ins.visitLabel(l0);
    ins.visitInsn(NOP);


    BytecodeCFGBuilder.BytecodeCFG cfg = ins.cfg();
    Multiset<String> cfgOpcodes = cfgOpcodes(cfg);
    List<String> collect = Instructions.ASM_OPCODES.stream().map(op -> Printer.OPCODES[op]).collect(Collectors.toList());
    assertThat(cfgOpcodes).containsAll(collect);
  }

  @Test
  public void visited_label_should_be_assigned_to_true_successor() throws Exception {
    Label label0 = new Label();
    Label label1 = new Label();
    BytecodeCFGBuilder.BytecodeCFG cfg = new Instructions()
      .visitVarInsn(Opcodes.ALOAD, 0)
      .visitJumpInsn(Opcodes.IFNULL, label0)
      .visitJumpInsn(Opcodes.IFEQ, label0)
      .visitInsn(Opcodes.ICONST_0)
      .visitJumpInsn(Opcodes.GOTO, label1)
      .visitLabel(label0)
      .visitInsn(Opcodes.ICONST_1)
      .visitLabel(label1)
      .visitInsn(Opcodes.IRETURN)
      .cfg();

    BytecodeCFGBuilder.Block block3 = cfg.blocks.get(3);
    assertThat(block3.terminator.opcode).isEqualTo(Opcodes.IFEQ);
    assertThat(block3.falseSuccessor()).isNotNull().isSameAs(cfg.blocks.get(4));
    assertThat(block3.trueSuccessor()).isNotNull().isSameAs(cfg.blocks.get(2));
    assertThat(block3.successors).hasSize(2);
    assertThat(block3.successors()).hasSize(2);
  }
  @Test
  public void goto_successors() throws Exception {
    Label label0 = new Label();
    Label label1 = new Label();
    BytecodeCFGBuilder.BytecodeCFG cfg = new Instructions()
      .visitVarInsn(Opcodes.ALOAD, 0)
      .visitJumpInsn(Opcodes.IFNULL, label0)
      .visitVarInsn(Opcodes.ALOAD, 0)
      .visitJumpInsn(Opcodes.IFNULL, label1)
      .visitVarInsn(Opcodes.ALOAD, 0)
      .visitVarInsn(Opcodes.ALOAD, 0)
      .visitJumpInsn(Opcodes.IFEQ, label0)
      .visitInsn(Opcodes.ICONST_0)
      .visitJumpInsn(Opcodes.GOTO, label1)
      .visitLabel(label0)
      .visitInsn(Opcodes.ICONST_1)
      .visitLabel(label1)
      .visitInsn(Opcodes.IRETURN)
      .cfg();
      assertThat(cfg.blocks.get(6).successors).containsExactly(cfg.blocks.get(4));
  }
}
