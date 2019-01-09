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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.sonar.java.resolve.BytecodeCompleter;
import org.sonar.java.resolve.Convert;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.SETestUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.JSR;
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
    BytecodeCFG cfg = getCFGForMethod("fun");
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
    private Object fun(boolean a, Object b) {
      if (a) {
        if (b == null) {
          return null;
        }
        return "";
      } else {
        return "not a";
      }
    }
    private boolean label_goto(Object b) {
      return  b == null;
    }
  }

  @Test
  public void label_goto_successors() throws Exception {
    BytecodeCFG cfg = getCFGForMethod("label_goto");
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

  private BytecodeCFG getCFGForMethod(String methodName) {
    SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    File file = new File("src/test/java/org/sonar/java/bytecode/cfg/BytecodeCFGBuilderTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, squidClassLoader);
    Symbol.TypeSymbol innerClass = ((Symbol.TypeSymbol) ((ClassTree) tree.types().get(0)).symbol().lookupSymbols("InnerClass").iterator().next());
    Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) innerClass.lookupSymbols(methodName).iterator().next();
    return SETestUtils.bytecodeCFG(symbol.signature(), squidClassLoader);
  }

  @Test
  public void test_all_instructions_are_part_of_CFG() throws Exception {
    SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    File file = new File("src/test/java/org/sonar/java/bytecode/cfg/testdata/CFGTestData.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, squidClassLoader);
    Symbol.TypeSymbol testClazz = ((ClassTree) tree.types().get(0)).symbol();
    ClassReader cr = new ClassReader(squidClassLoader.getResourceAsStream(Convert.bytecodeName(CFGTestData.class.getCanonicalName()) + ".class"));
    ClassNode classNode = new ClassNode(BytecodeCompleter.ASM_API_VERSION);
    cr.accept(classNode, 0);
    for (MethodNode method : classNode.methods) {
      Multiset<String> opcodes = Arrays.stream(method.instructions.toArray())
        .map(AbstractInsnNode::getOpcode)
        .filter(opcode -> opcode != -1)
        .map(opcode -> Printer.OPCODES[opcode])
        .collect(Collectors.toCollection(HashMultiset::create));

      Symbol methodSymbol = Iterables.getOnlyElement(testClazz.lookupSymbols(method.name));
      BytecodeCFG bytecodeCFG = SETestUtils.bytecodeCFG(((Symbol.MethodSymbol) methodSymbol).signature(), squidClassLoader);
      Multiset<String> cfgOpcodes = cfgOpcodes(bytecodeCFG);
      assertThat(cfgOpcodes).isEqualTo(opcodes);
    }
  }

  private Multiset<String> cfgOpcodes(BytecodeCFG bytecodeCFG) {
    return bytecodeCFG.blocks.stream()
          .flatMap(block -> Stream.concat(block.instructions.stream(), Stream.of(block.terminator)))
          .filter(Objects::nonNull)
          .map(Instruction::opcode)
          .map(opcode -> Printer.OPCODES[opcode])
          .collect(Collectors.toCollection(HashMultiset::create));
  }

  @Test
  public void all_opcodes_should_be_visited() throws Exception {
    Instructions ins = new Instructions();
    Predicate<Integer> filterReturnAndThrow = opcode -> !((Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW);
    NO_OPERAND_INSN.stream().filter(filterReturnAndThrow).forEach(ins::visitInsn);
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


    BytecodeCFG cfg = ins.cfg();
    Multiset<String> cfgOpcodes = cfgOpcodes(cfg);
    List<String> collect = Instructions.OPCODES.stream().filter(filterReturnAndThrow).map(op -> Printer.OPCODES[op]).collect(Collectors.toList());
    assertThat(cfgOpcodes).containsAll(collect);
  }

  @Test
  public void visited_label_should_be_assigned_to_true_successor() throws Exception {
    Label label0 = new Label();
    Label label1 = new Label();
    BytecodeCFG cfg = new Instructions()
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

    BytecodeCFG.Block block3 = cfg.blocks.get(3);
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
    BytecodeCFG cfg = new Instructions()
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

  @Test
  public void isNotBlank_goto_followed_by_label() throws Exception {
    SquidClassLoader classLoader = new SquidClassLoader(Lists.newArrayList(new File("src/test/commons-lang-2.1")));
    // apache commons 2.1 isNotBlank has a goto followed by an unreferenced label : see SONARJAVA-2461
    BytecodeCFG bytecodeCFG = SETestUtils.bytecodeCFG("org.apache.commons.lang.StringUtils#isNotBlank(Ljava/lang/String;)Z", classLoader);
    assertThat(bytecodeCFG).isNotNull();
    assertThat(bytecodeCFG.blocks).hasSize(11);
    assertThat(bytecodeCFG.blocks.get(4).successors).containsExactly(bytecodeCFG.blocks.get(6));
  }

  @Test
  public void supportJSRandRET() throws Exception {
    SquidClassLoader classLoader = new SquidClassLoader(Lists.newArrayList(new File("src/test/JsrRet")));
    BytecodeCFG bytecodeCFG = SETestUtils.bytecodeCFG("jdk3.AllInstructions#jsrAndRetInstructions(I)I", classLoader);
    assertThat(bytecodeCFG).isNotNull();
    bytecodeCFG.blocks.stream().map(b-> b.terminator).filter(Objects::nonNull).forEach(t -> assertThat(t.opcode).isNotEqualTo(JSR));
  }

  @Test
  public void try_catch_finally() throws Exception {
    String methodName = "tryCatch";
    String expectedCFG = "B0(Exit)\n" +
      "B1\n" +
      "Jumps to: B2 \n" +
      "B2\n" +
      "0: ALOAD\n" +
      "1: INVOKESPECIAL\n" +
      "2: ALOAD\n" +
      "3: INVOKESPECIAL\n" +
      "Jumps to: B3 B5(Exception:java.io.IOException) B7(Exception:!UncaughtException!) \n" +
      "B3\n" +
      "0: GETSTATIC\n" +
      "1: LDC\n" +
      "2: INVOKEVIRTUAL\n" +
      "GOTO Jumps to: B4 \n" +
      "B4\n" +
      "0: RETURN\n" +
      "Jumps to: B0 \n" +
      "B5\n" +
      "0: ASTORE\n" +
      "1: ALOAD\n" +
      "2: INVOKEVIRTUAL\n" +
      "Jumps to: B6 B7(Exception:!UncaughtException!) \n" +
      "B6\n" +
      "0: GETSTATIC\n" +
      "1: LDC\n" +
      "2: INVOKEVIRTUAL\n" +
      "GOTO Jumps to: B4 \n" +
      "B7\n" +
      "0: ASTORE\n" +
      "1: GETSTATIC\n" +
      "2: LDC\n" +
      "3: INVOKEVIRTUAL\n" +
      "4: ALOAD\n" +
      "5: ATHROW\n" +
      "Jumps to: B0 \n";
    assertCFGforMethod(methodName, expectedCFG);
  }

  @Test
  public void try_catch_finally_with_multiplePaths_in_try() throws Exception {
    String methodName = "tryCatchWithMultiplePaths";
    assertCFGforMethod(methodName, "B0(Exit)\n" +
      "B1\n" +
      "Jumps to: B2 \n" +
      "B2\n" +
      "0: ALOAD\n" +
      "1: INVOKESPECIAL\n" +
      "2: ILOAD\n" +
      "IFNE Jumps to: B3(true) B4(false) B8(Exception:java.io.IOException) B10(Exception:!UncaughtException!) \n" +
      "B3\n" +
      "0: ALOAD\n" +
      "1: INVOKESPECIAL\n" +
      "Jumps to: B5 B8(Exception:java.io.IOException) B10(Exception:!UncaughtException!) \n" +
      "B4\n" +
      "0: ALOAD\n" +
      "1: INVOKESPECIAL\n" +
      "GOTO Jumps to: B5 B8(Exception:java.io.IOException) B10(Exception:!UncaughtException!) \n" +
      "B5\n" +
      "0: GETSTATIC\n" +
      "1: LDC\n" +
      "2: INVOKEVIRTUAL\n" +
      "Jumps to: B6 B8(Exception:java.io.IOException) B10(Exception:!UncaughtException!) \n" +
      "B6\n" +
      "0: GETSTATIC\n" +
      "1: LDC\n" +
      "2: INVOKEVIRTUAL\n" +
      "GOTO Jumps to: B7 \n" +
      "B7\n" +
      "0: RETURN\n" +
      "Jumps to: B0 \n" +
      "B8\n" +
      "0: ASTORE\n" +
      "1: ALOAD\n" +
      "2: INVOKEVIRTUAL\n" +
      "Jumps to: B9 B10(Exception:!UncaughtException!) \n" +
      "B9\n" +
      "0: GETSTATIC\n" +
      "1: LDC\n" +
      "2: INVOKEVIRTUAL\n" +
      "GOTO Jumps to: B7 \n" +
      "B10\n" +
      "0: ASTORE\n" +
      "1: GETSTATIC\n" +
      "2: LDC\n" +
      "3: INVOKEVIRTUAL\n" +
      "4: ALOAD\n" +
      "5: ATHROW\n" +
      "Jumps to: B0 \n");
  }

  @Test
  public void last_block_is_ends_with_GOTO() throws Exception {
    assertCFGforMethod("loopWithoutStopCondition", "B0(Exit)\n" +
      "B1\n" +
      "0: ICONST_0\n" +
      "1: ISTORE\n" +
      "Jumps to: B2 \n" +
      "B2\n" +
      "0: ILOAD\n" +
      "1: GETSTATIC\n" +
      "2: ILOAD\n" +
      "3: IALOAD\n" +
      "IF_ICMPGT Jumps to: B3(true) B4(false) \n" +
      "B3\n" +
      "0: IINC\n" +
      "GOTO Jumps to: B2 \n" +
      "B4\n" +
      "0: ILOAD\n" +
      "1: ICONST_1\n" +
      "2: IADD\n" +
      "3: IRETURN\n" +
      "Jumps to: B0 \n");
  }

  @Test
  public void test_class_not_found_logs() throws Exception {
    SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    BytecodeCFG cfg = SETestUtils.bytecodeCFG("nonsense#foo", squidClassLoader);
    assertThat(cfg).isNull();
  }

  private void assertCFGforMethod(String methodName, String expectedCFG) {
    BytecodeCFG cfg = getBytecodeCFG(methodName, "src/test/java/org/sonar/java/bytecode/cfg/BytecodeCFGBuilderTest.java");
    StringBuilder sb = new StringBuilder();
    cfg.blocks.forEach(b-> sb.append(b.printBlock()));
    assertThat(sb.toString()).isEqualTo(expectedCFG);
  }

  public static BytecodeCFG getBytecodeCFG(String methodName, String filename) {
    SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    File file = new File(filename);
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, squidClassLoader);
    List<Tree> classMembers = ((ClassTree) tree.types().get(0)).members();
    Symbol.MethodSymbol symbol = classMembers.stream()
      .filter( m-> m instanceof MethodTree)
      .map(m -> ((MethodTree) m).symbol())
      .filter(s -> methodName.equals(s.name()))
      .findFirst()
      .orElseThrow(IllegalStateException::new);
    return SETestUtils.bytecodeCFG(symbol.signature(), squidClassLoader);
  }

  private void tryCatch() {
    try {
      bar();
      fun();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      System.out.println("finally");
    }

  }

  private void tryCatchWithMultiplePaths(int i) {
    try {
      bar();
      if(i==0) {
        fun();
      } else {
        fun2();
      }
      System.out.println("endOfTry");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      System.out.println("finally");
    }
  }
  private void bar() {
  }
  private void fun() throws IOException {
  }
  private void fun2() throws IOException {
  }

  final static int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE};
  static int loopWithoutStopCondition(int x) {
    for (int i = 0;; i++)
      if (x <= sizeTable[i])
        return i + 1;
  }
}
