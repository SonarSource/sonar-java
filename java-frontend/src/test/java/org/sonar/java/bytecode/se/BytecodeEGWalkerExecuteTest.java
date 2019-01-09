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
package org.sonar.java.bytecode.se;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Printer;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.cfg.BytecodeCFG;
import org.sonar.java.bytecode.cfg.Instruction;
import org.sonar.java.bytecode.cfg.Instructions;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.cfg.CFG;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.ProgramPoint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SETestUtils;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.constraint.TypedConstraint;
import org.sonar.java.se.symbolicvalues.BinarySymbolicValue;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DREM;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LAND;
import static org.objectweb.asm.Opcodes.LDIV;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LOR;
import static org.objectweb.asm.Opcodes.LREM;
import static org.objectweb.asm.Opcodes.LSHL;
import static org.objectweb.asm.Opcodes.LSHR;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.LUSHR;
import static org.objectweb.asm.Opcodes.LXOR;
import static org.objectweb.asm.Opcodes.RETURN;

public class BytecodeEGWalkerExecuteTest {

  private static final Set<Integer> LONG_OPCODE = ImmutableSet.of(LADD, LSUB, LMUL, LDIV, LAND, LOR, LXOR, LREM, LSHL, LSHR, LUSHR,
    DADD, DSUB, DMUL, DDIV, DREM);

  private static final String TRY_CATCH_SIGNATURE = BytecodeEGWalkerExecuteTest.class.getCanonicalName() + "#tryCatch(Z)V";
  private static final String TRY_WRONG_CATCH_SIGNATURE = BytecodeEGWalkerExecuteTest.class.getCanonicalName() + "#tryWrongCatch(Z)V";


  static SemanticModel semanticModel;
  private BytecodeEGWalker walker;
  private static SquidClassLoader squidClassLoader;

  @BeforeClass
  public static void initializeClassLoaderAndSemanticModel() {
    List<File> files = new ArrayList<>(FileUtils.listFiles(new File("target/test-jars"), new String[]{"jar", "zip"}, true));
    files.add(new File("target/classes"));
    files.add(new File("target/test-classes"));
    squidClassLoader = new SquidClassLoader(files);
    File file = new File("src/test/java/org/sonar/java/bytecode/se/BytecodeEGWalkerExecuteTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    semanticModel = SemanticModel.createFor(tree, squidClassLoader);
  }

  @Before
  public void initializeWalker() {
    BehaviorCache behaviorCache = new BehaviorCache(squidClassLoader);
    behaviorCache.setFileContext(null, semanticModel);
    walker = new BytecodeEGWalker(behaviorCache, semanticModel);
  }

  @Test
  public void athrow_should_not_be_linked_to_next_label() throws Exception {
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse("class A {int field;}");
    SquidClassLoader classLoader = new SquidClassLoader(Collections.singletonList(new File("src/test/JsrRet")));
    SemanticModel semanticModel = SemanticModel.createFor(tree, classLoader);
    BehaviorCache behaviorCache = new BehaviorCache(classLoader);
    behaviorCache.setFileContext(null, semanticModel);
    BytecodeEGWalker walker = new BytecodeEGWalker(behaviorCache, semanticModel);
    MethodBehavior methodBehavior = walker.getMethodBehavior("org.apache.commons.io.FileUtils#readFileToString(Ljava/io/File;)Ljava/lang/String;", classLoader);
    assertThat(methodBehavior.happyPathYields().collect(Collectors.toList())).hasSize(1);
    assertThat(methodBehavior.exceptionalPathYields().collect(Collectors.toList())).hasSize(2);
  }

  @Test
  public void behavior_with_no_yield_should_stack_value() throws Exception {
    BehaviorCache behaviorCache = new BehaviorCache(squidClassLoader);
    MethodBehavior methodBehavior = behaviorCache.get("org.mypackage.MyClass#MyMethod()Ljava/lang/Exception;");
    methodBehavior.completed();
    BytecodeEGWalker walker = new BytecodeEGWalker(behaviorCache, semanticModel);
    walker.programState = ProgramState.EMPTY_STATE;
    CFG.IBlock block = mock(CFG.IBlock.class);
    when(block.successors()).thenReturn(Collections.emptySet());
    walker.programPosition = new ProgramPoint(block);

    walker.workList.clear();
    walker.executeInstruction(new Instruction(INVOKESTATIC, new Instruction.FieldOrMethod("org.mypackage.MyClass", "MyMethod", "()Ljava/lang/Exception;")));
    assertThat(walker.workList.getFirst().programState.peekValue()).isNotNull();
  }
  @Test
  public void test_nop() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.NOP));
    assertThat(programState).isEqualTo(ProgramState.EMPTY_STATE);
  }

  @Test
  public void test_ldc() throws Exception {
    ProgramState programState = execute(new Instruction.LdcInsn("a"));
    assertStack(programState, ObjectConstraint.NOT_NULL);
    SymbolicValue sv = programState.peekValue();
    assertThat(isDoubleOrLong(programState, sv)).isFalse();
    programState = execute(new Instruction.LdcInsn(1L));
    sv = programState.peekValue();
    assertThat(isDoubleOrLong(programState, sv)).isTrue();
    programState = execute(new Instruction.LdcInsn(1D));
    sv = programState.peekValue();
    assertThat(isDoubleOrLong(programState, sv)).isTrue();
  }

  @Test
  public void test_aconst_null() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.ACONST_NULL));
    assertStack(programState, ObjectConstraint.NULL);
  }

  @Test
  public void test_xReturn() throws Exception {
    SymbolicValue returnValue = new SymbolicValue();
    int[] opcodes = {Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN};
    for (int opcode : opcodes) {
      ProgramState programState = execute(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(returnValue));
      assertThat(programState.peekValue()).isNull();
      assertThat(programState.exitValue()).isEqualTo(returnValue);
    }
  }

  @Test
  public void test_return() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.RETURN), ProgramState.EMPTY_STATE);
    assertThat(programState.peekValue()).isNull();
    assertThat(programState.exitValue()).isNull();
  }

  @Test
  public void test_iconst() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.ICONST_0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, BooleanConstraint.FALSE, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.ICONST_1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, BooleanConstraint.TRUE, ObjectConstraint.NOT_NULL}});

    int[] opCodesConst = new int[] {Opcodes.ICONST_M1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5};
    for (int opcode : opCodesConst) {
      programState = execute(new Instruction(opcode));
      assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL}});
    }
  }

  @Test
  public void test_lconst() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.LCONST_0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, BooleanConstraint.FALSE, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.LCONST_1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, BooleanConstraint.TRUE, ObjectConstraint.NOT_NULL}});
  }

  @Test
  public void test_fconst() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.FCONST_0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, BooleanConstraint.FALSE, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.FCONST_1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, BooleanConstraint.TRUE, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.FCONST_2));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL}});
  }

  @Test
  public void test_bipush() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.BIPUSH, 42));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.BIPUSH, 1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL, BooleanConstraint.TRUE}});

    programState = execute(new Instruction(Opcodes.BIPUSH, 0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, ObjectConstraint.NOT_NULL, BooleanConstraint.FALSE}});
  }

  @Test
  public void test_sipush() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.SIPUSH, 42));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.SIPUSH, 1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, ObjectConstraint.NOT_NULL, BooleanConstraint.TRUE}});

    programState = execute(new Instruction(Opcodes.SIPUSH, 0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, ObjectConstraint.NOT_NULL, BooleanConstraint.FALSE}});
  }

  @Test
  public void test_array_load() throws Exception {
    int[] loadRefOpcodes = new int[] {Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD, Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD};
    SymbolicValue array = new SymbolicValue();
    SymbolicValue index = new SymbolicValue();
    ProgramState initState = ProgramState.EMPTY_STATE.stackValue(array).stackValue(index);
    for (int opcode : loadRefOpcodes) {
      ProgramState programState = execute(new Instruction(opcode), initState);
      if (opcode != Opcodes.AALOAD) {
        assertStack(programState, ObjectConstraint.NOT_NULL);
      }
      ProgramState.Pop result = programState.unstackValue(1);
      assertThat(result.values).hasSize(1);
      assertThat(result.values.get(0)).isNotEqualTo(array);
      assertThat(result.values.get(0)).isNotEqualTo(index);
      if (opcode == Opcodes.DALOAD || opcode == Opcodes.LALOAD) {
        assertThat(isDoubleOrLong(programState, result.values.get(0))).isTrue();
      }
    }
  }

  @Test
  public void test_array_store() throws Exception {
    int[] storeArrayOpcodes = new int[] {Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE, Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE};
    SymbolicValue array = new SymbolicValue();
    SymbolicValue index = new SymbolicValue();
    SymbolicValue value = new SymbolicValue();
    ProgramState initState = ProgramState.EMPTY_STATE.stackValue(array).stackValue(index).stackValue(value);
    for (int opcode : storeArrayOpcodes) {
      ProgramState ps = execute(new Instruction(opcode), initState);
      assertEmptyStack(ps);
    }
  }

  @Test
  public void test_dconst() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.DCONST_0));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.ZERO, BooleanConstraint.FALSE, ObjectConstraint.NOT_NULL}});

    programState = execute(new Instruction(Opcodes.DCONST_1));
    assertStack(programState, new Constraint[][] {{DivisionByZeroCheck.ZeroConstraint.NON_ZERO, BooleanConstraint.TRUE, ObjectConstraint.NOT_NULL}});
  }

  @Test
  public void test_load() throws Exception {
    int[] loadRefOpcodes = new int[] {Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD};
    for (int loadRefOpcode : loadRefOpcodes) {
      SymbolicValue loadRef = new SymbolicValue();
      ProgramState programState = execute(new Instruction(loadRefOpcode, 0), ProgramState.EMPTY_STATE.put(0, loadRef));
      assertThat(programState.peekValue()).isEqualTo(loadRef);
      // no SV indexed should failed
      assertThatThrownBy(() -> execute(new Instruction(loadRefOpcode, 0), ProgramState.EMPTY_STATE)).hasMessage("Loading a symbolic value unindexed");
    }
  }

  @Test
  public void test_pop() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.POP), ProgramState.EMPTY_STATE.stackValue(sv1).stackValue(sv2));
    assertThat(programState.peekValue()).isEqualTo(sv1);
    programState = execute(new Instruction(Opcodes.POP));
    assertEmptyStack(programState);
  }

  @Test
  public void test_pop2() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.POP2), ProgramState.EMPTY_STATE.stackValue(sv1).stackValue(sv2).stackValue(sv3));
    assertThat(programState.peekValue()).isEqualTo(sv1);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.POP2))).hasMessage("POP2 on empty stack");
  }

  @Test
  public void test_pop2_long_double() throws Exception {
    SymbolicValue normalSv = new SymbolicValue();
    SymbolicValue longSv = new SymbolicValue();
    ProgramState startingState = ProgramState.EMPTY_STATE.stackValue(normalSv).stackValue(longSv);
    startingState = setDoubleOrLong(startingState, longSv, true);
    ProgramState programState = execute(new Instruction(Opcodes.POP2), startingState);
    assertThat(programState.peekValue()).isEqualTo(normalSv);
  }

  @Test
  public void test_new() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.NEW, "java.lang.Object"));
    assertStack(programState, new Constraint[][] {{ ObjectConstraint.NOT_NULL, new TypedConstraint("java.lang.Object")}});
  }

  @Test
  public void test_dup() throws Exception {
    SymbolicValue sv = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP), ProgramState.EMPTY_STATE.stackValue(sv));
    ProgramState.Pop pop = programState.unstackValue(2);
    assertThat(pop.values).containsOnly(sv);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP)))
      .hasMessage("DUP on empty stack");
  }

  @Test
  public void test_dup_x1() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP_X1), ProgramState.EMPTY_STATE.stackValue(sv3).stackValue(sv2).stackValue(sv1));
    ProgramState.Pop pop = programState.unstackValue(4);
    assertThat(pop.values).containsExactly(sv1, sv2, sv1, sv3);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP_X1), ProgramState.EMPTY_STATE.stackValue(sv1)))
      .hasMessage("DUP_X1 needs 2 values on stack");
  }

  @Test
  public void test_dup_x2() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP_X2), ProgramState.EMPTY_STATE.stackValue(sv3).stackValue(sv2).stackValue(sv1));
    ProgramState.Pop pop = programState.unstackValue(4);
    assertThat(pop.values).containsExactly(sv1, sv2, sv3, sv1);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP_X2), ProgramState.EMPTY_STATE.stackValue(sv1).stackValue(sv2)))
      .hasMessage("DUP_X2 needs 3 values on stack");
  }

  @Test
  public void test_dup_x2_long_double() throws Exception {
    SymbolicValue normalSv = new SymbolicValue();
    SymbolicValue longSv = new SymbolicValue();
    SymbolicValue another = new SymbolicValue();
    ProgramState startingState = ProgramState.EMPTY_STATE.stackValue(another).stackValue(longSv).stackValue(normalSv);
    startingState = setDoubleOrLong(startingState, longSv, true);
    ProgramState programState = execute(new Instruction(Opcodes.DUP_X2), startingState);
    ProgramState.Pop pop = programState.unstackValue(4);
    assertThat(pop.values).containsExactly(normalSv, longSv, normalSv, another);
  }

  @Test
  public void test_dup2() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP2), ProgramState.EMPTY_STATE.stackValue(sv2).stackValue(sv1));
    ProgramState.Pop pop = programState.unstackValue(4);
    assertThat(pop.values).containsExactly(sv1, sv2, sv1, sv2);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP2)))
      .hasMessage("DUP2 needs at least 1 value on stack");
  }

  @Test
  public void test_dup2_long_double() throws Exception {
    SymbolicValue longSv = new SymbolicValue();
    SymbolicValue another = new SymbolicValue();
    ProgramState startingState = ProgramState.EMPTY_STATE.stackValue(another).stackValue(longSv);
    startingState = setDoubleOrLong(startingState, longSv, true);
    ProgramState programState = execute(new Instruction(Opcodes.DUP2), startingState);
    ProgramState.Pop pop = programState.unstackValue(4);
    assertThat(pop.values).containsExactly(longSv, longSv, another);
  }

  @Test
  public void test_dup2_x1() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP2_X1), ProgramState.EMPTY_STATE.stackValue(sv3).stackValue(sv2).stackValue(sv1));
    ProgramState.Pop pop = programState.unstackValue(5);
    assertThat(pop.values).containsExactly(sv1, sv2, sv3, sv1, sv2);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP2_X1), ProgramState.EMPTY_STATE.stackValue(sv1)))
      .hasMessage("DUP2_X1 needs 3 values on stack");
  }

  @Test
  public void test_dup2_x1_long_double() throws Exception {
    SymbolicValue normalSv = new SymbolicValue();
    SymbolicValue longSv = new SymbolicValue();
    SymbolicValue another = new SymbolicValue();
    ProgramState startingState = ProgramState.EMPTY_STATE.stackValue(another).stackValue(normalSv).stackValue(longSv);
    startingState = setDoubleOrLong(startingState, longSv, true);
    ProgramState programState = execute(new Instruction(Opcodes.DUP2_X1), startingState);
    ProgramState.Pop pop = programState.unstackValue(5);
    assertThat(pop.values).containsExactly(longSv, normalSv, longSv, another);
  }

  @Test
  public void test_swap() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.SWAP), ProgramState.EMPTY_STATE.stackValue(sv1).stackValue(sv2));
    ProgramState.Pop pop = programState.unstackValue(2);
    assertThat(pop.values).containsExactly(sv1, sv2);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.SWAP), ProgramState.EMPTY_STATE.stackValue(sv1)))
      .hasMessage("SWAP needs 2 values on stack");
  }

  @Test
  public void test_dup2_x2() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    SymbolicValue sv4 = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.DUP2_X2), ProgramState.EMPTY_STATE.stackValue(sv4).stackValue(sv3).stackValue(sv2).stackValue(sv1));
    ProgramState.Pop pop = programState.unstackValue(6);
    assertThat(pop.values).containsExactly(sv1, sv2, sv3, sv4, sv1, sv2);
    assertThat(pop.state).isEqualTo(ProgramState.EMPTY_STATE);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.DUP2_X2), ProgramState.EMPTY_STATE.stackValue(sv1).stackValue(sv2).stackValue(sv3)))
      .hasMessage("DUP2_X2 needs 4 values on stack");
  }

  // see https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html#jvms-6.5.dup2_x2
  @Test
  public void test_dup2_x2_form2() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    SymbolicValue sv4 = new SymbolicValue();
    ProgramState startingState = ProgramState.EMPTY_STATE.stackValue(sv4).stackValue(sv3).stackValue(sv2).stackValue(sv1);
    startingState = setDoubleOrLong(startingState, sv1, true);
    ProgramState programState = execute(new Instruction(Opcodes.DUP2_X2), startingState);
    ProgramState.Pop pop = programState.unstackValue(6);
    assertThat(pop.values).containsExactly(sv1, sv2, sv3, sv1, sv4);
  }

  @Test
  public void test_dup2_x2_form3() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    SymbolicValue sv4 = new SymbolicValue();
    ProgramState startingState = ProgramState.EMPTY_STATE.stackValue(sv4).stackValue(sv3).stackValue(sv2).stackValue(sv1);
    startingState = setDoubleOrLong(startingState, sv3, true);
    ProgramState programState = execute(new Instruction(Opcodes.DUP2_X2), startingState);
    ProgramState.Pop pop = programState.unstackValue(6);
    assertThat(pop.values).containsExactly(sv1, sv2, sv3, sv1, sv2, sv4);
  }

  @Test
  public void test_dup2_x2_form4() throws Exception {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    SymbolicValue sv4 = new SymbolicValue();
    ProgramState startingState = ProgramState.EMPTY_STATE.stackValue(sv4).stackValue(sv3).stackValue(sv2).stackValue(sv1);
    startingState = setDoubleOrLong(startingState, sv1, true);
    startingState = setDoubleOrLong(startingState, sv2, true);
    ProgramState programState = execute(new Instruction(Opcodes.DUP2_X2), startingState);
    ProgramState.Pop pop = programState.unstackValue(6);
    assertThat(pop.values).containsExactly(sv1, sv2, sv1, sv3, sv4);
  }

  @Test
  public void test_add_sub_mul_div_rem() throws Exception {
    int[] opcodes = new int[] {
      Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD,
      Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB,
      Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL,
      Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV,
      Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM,
    };
    assertConsume2produceNotNull(opcodes);

    assertThrowWhenInvalidStack(opcodes, " needs 2 values on stack");
  }

  @Test
  public void test_neg() throws Exception {
    SymbolicValue sv = new SymbolicValue();

    int[] negOpcodes = new int[] {Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG};
    ProgramState initState = ProgramState.EMPTY_STATE.stackValue(sv);
    for (int negOpcode : negOpcodes) {
      ProgramState programState = execute(new Instruction(negOpcode), initState);
      assertStack(programState, new Constraint[][] {{ObjectConstraint.NOT_NULL}});
      assertThat(programState.peekValue()).isNotEqualTo(sv);
    }

    for (int opcode : negOpcodes) {
      assertThatThrownBy(() -> execute(new Instruction(opcode), ProgramState.EMPTY_STATE))
        .hasMessage(Printer.OPCODES[opcode] + " needs 1 values on stack");
    }
  }

  @Test
  public void test_shift() throws Exception {
    int[] shiftOpcodes = new int[] {Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR, Opcodes.LUSHR};
    assertConsume2produceNotNull(shiftOpcodes);

    assertThrowWhenInvalidStack(shiftOpcodes, " needs 2 values on stack");
  }

  @Test
  public void test_and() throws Exception {
    int[] opcodes = new int[] {Opcodes.IAND, Opcodes.LAND};
    assertBinarySymbolicValue(opcodes, SymbolicValue.AndSymbolicValue.class);

    assertThrowWhenInvalidStack(opcodes, " needs 2 values on stack");
  }

  private void assertThrowWhenInvalidStack(int[] opcodes, String message) {
    for (int opcode : opcodes) {
      assertThatThrownBy(() -> execute(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(new SymbolicValue())))
        .hasMessage(Printer.OPCODES[opcode] + message);
    }
  }

  private void assertBinarySymbolicValue(int[] opcodes, Class<? extends BinarySymbolicValue> binarySvClass) {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    ProgramState initState = ProgramState.EMPTY_STATE.stackValue(sv2).stackValue(sv1);
    for (int opcode : opcodes) {
      ProgramState programState = execute(new Instruction(opcode), initState);
      ProgramState.Pop pop = programState.unstackValue(1);
      assertStack(programState, new Constraint[][] {{ObjectConstraint.NOT_NULL}});
      SymbolicValue result = pop.values.get(0);
      assertThat(result).isNotEqualTo(sv1);
      assertThat(result).isNotEqualTo(sv2);
      assertThat(result).isInstanceOf(binarySvClass);
      assertThat(isDoubleOrLong(programState, result)).isEqualTo(LONG_OPCODE.contains(opcode));
      BinarySymbolicValue andSv = (BinarySymbolicValue) result;
      assertThat(andSv.getRightOp()).isEqualTo(sv1);
      assertThat(andSv.getLeftOp()).isEqualTo(sv2);
    }
  }

  @Test
  public void test_or() throws Exception {
    int[] opcodes = new int[] {Opcodes.IOR, Opcodes.LOR};
    assertBinarySymbolicValue(opcodes, SymbolicValue.OrSymbolicValue.class);

    assertThrowWhenInvalidStack(opcodes, " needs 2 values on stack");
  }

  @Test
  public void test_xor() throws Exception {
    int[] opcodes = new int[] {Opcodes.IXOR, Opcodes.LXOR};
    assertBinarySymbolicValue(opcodes, SymbolicValue.XorSymbolicValue.class);
    assertThrowWhenInvalidStack(opcodes, " needs 2 values on stack");
  }

  @Test
  public void test_iinc() throws Exception {
    SymbolicValue sv = new SymbolicValue();
    ProgramState programState = ProgramState.EMPTY_STATE.put(2, sv);
    programState = execute(new Instruction(Opcodes.IINC, 2), programState);
    SymbolicValue result = programState.getValue(2);
    assertThat(result).isNotEqualTo(sv);
    assertThat(programState.getConstraint(result, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    assertEmptyStack(programState);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.IINC, 1), ProgramState.EMPTY_STATE))
      .hasMessage("Local variable 1 not found");
  }

  @Test
  public void test_cmp() throws Exception {
    int[] opcodes = new int[] {Opcodes.LCMP, Opcodes.FCMPG, Opcodes.FCMPL, Opcodes.DCMPG, Opcodes.FCMPL};
    assertConsume2produceNotNull(opcodes);

    assertThrowWhenInvalidStack(opcodes, " needs 2 values on stack");
  }

  private void assertConsume2produceNotNull(int... opcodes) {
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    ProgramState initState = ProgramState.EMPTY_STATE.stackValue(sv2).stackValue(sv1);
    for (int opcode : opcodes) {
      ProgramState programState = execute(new Instruction(opcode), initState);
      ProgramState.Pop pop = programState.unstackValue(1);
      assertStack(programState, new Constraint[][] {{ObjectConstraint.NOT_NULL}});
      SymbolicValue result = pop.values.get(0);
      assertThat(result).isNotEqualTo(sv1);
      assertThat(result).isNotEqualTo(sv2);
      assertThat(isDoubleOrLong(programState, result)).isEqualTo(LONG_OPCODE.contains(opcode));
    }
  }

  @Test
  public void test_invoke_instance_method() throws Exception {
    int[] opcodes = new int[] {Opcodes.INVOKESPECIAL, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE};
    for (int opcode : opcodes) {
      SymbolicValue thisSv = new SymbolicValue();
      ProgramState stateWithThis = ProgramState.EMPTY_STATE.stackValue(thisSv);
      ProgramState programState = execute(invokeMethod(opcode, "methodWithoutArgument", "()V"), stateWithThis);
      assertEmptyStack(programState);
      assertThat(programState.getConstraints(thisSv).get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);

      programState = execute(invokeMethod(opcode, "finalVoid", "()V"), stateWithThis);
      assertEmptyStack(programState);
      assertThat(programState.getConstraints(thisSv).get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);

      programState = execute(invokeMethod(opcode, "booleanMethod", "()Z"), stateWithThis);
      assertStack(programState, new Constraint[] {null});
      assertThat(isDoubleOrLong(programState, programState.peekValue())).isFalse();

      SymbolicValue arg = new SymbolicValue();
      programState = execute(invokeMethod(opcode, "intMethodWithIntArgument", "(I)I"), stateWithThis.stackValue(arg));
      assertStack(programState, new Constraint[] {null});
      assertThat(programState.peekValue()).isNotEqualTo(arg);

      programState = execute(invokeMethod(opcode, "methodWithIntIntArgument", "(II)V"), stateWithThis.stackValue(arg).stackValue(arg));
      assertEmptyStack(programState);
      assertThatThrownBy(() -> execute(invokeMethod(opcode, "methodWithIntIntArgument", "(II)V"), stateWithThis))
        .isInstanceOf(IllegalStateException.class);

      programState = execute(invokeMethod(opcode, "returningLong", "()J"), stateWithThis);
      assertThat(isDoubleOrLong(programState, programState.peekValue())).isTrue();
      programState = execute(invokeMethod(opcode, "returningDouble", "()D"), stateWithThis);
      assertThat(isDoubleOrLong(programState, programState.peekValue())).isTrue();
    }
  }

  @Test
  public void test_invoke_static() throws Exception {
    ProgramState programState = execute(invokeStatic("staticMethod", "()V"));
    assertEmptyStack(programState);

    programState = execute(invokeStatic("staticBooleanMethod", "()Z"));
    assertStack(programState, new Constraint[][] {{ObjectConstraint.NOT_NULL, BooleanConstraint.FALSE}});
    assertThat(isDoubleOrLong(programState, programState.peekValue())).isFalse();

    SymbolicValue arg = new SymbolicValue();
    programState = execute(invokeStatic("staticIntMethodWithIntArgument", "(I)I"), ProgramState.EMPTY_STATE.stackValue(arg));
    assertStack(programState, new Constraint[][] {{ObjectConstraint.NOT_NULL, DivisionByZeroCheck.ZeroConstraint.ZERO}});
    assertThat(programState.peekValue()).isNotEqualTo(arg);
    assertThat(isDoubleOrLong(programState, programState.peekValue())).isFalse();

    programState = execute(invokeStatic("staticMethodWithIntIntArgument", "(II)V"), ProgramState.EMPTY_STATE.stackValue(arg).stackValue(arg));
    assertEmptyStack(programState);

    programState = execute(invokeStatic("staticMethodWithIntIntArgument", "(II)V"), ProgramState.EMPTY_STATE.stackValue(arg).stackValue(arg));
    assertEmptyStack(programState);

    programState = execute(invokeStatic("staticReturningLong", "()J"), ProgramState.EMPTY_STATE);
    assertThat(isDoubleOrLong(programState, programState.peekValue())).isTrue();

    programState = execute(invokeStatic("staticReturningDouble", "()D"), ProgramState.EMPTY_STATE);
    assertThat(isDoubleOrLong(programState, programState.peekValue())).isTrue();

    assertThatThrownBy(() -> execute(invokeStatic("staticBooleanMethodWithIntArgument", "(I)V")))
      .hasMessage("Arguments mismatch for INVOKE");
  }

  @Test
  public void test_athrow() throws Exception {
    SymbolicValue sv = new SymbolicValue();
    Type exceptionType = semanticModel.getClassType("java.lang.RuntimeException");
    ProgramState initialState = ProgramState.EMPTY_STATE.stackValue(sv)
        .addConstraint(sv, new TypedConstraint("java.lang.RuntimeException"));
    ProgramState programState = execute(new Instruction(Opcodes.ATHROW), initialState);
    SymbolicValue exception = programState.peekValue();
    assertThat(exception).isInstanceOf(SymbolicValue.ExceptionalSymbolicValue.class);
    assertThat(((SymbolicValue.ExceptionalSymbolicValue) exception).exceptionType()).isEqualTo(exceptionType);
    assertThat(programState.exitValue()).isEqualTo(exception);
  }

  @Test
  public void test_nullness_check() throws Exception {
    SymbolicValue thisSv = new SymbolicValue();
    ProgramState startingState = ProgramState.EMPTY_STATE.stackValue(thisSv);
    ProgramState programState = execute(invokeMethod(Opcodes.INVOKESPECIAL, "methodWithoutArgument", "()V"), startingState);
    assertThat(hasConstraint(thisSv, programState, ObjectConstraint.NOT_NULL)).isTrue();

    programState = execute(invokeStatic("staticBooleanMethod", "()Z"), startingState);
    assertStack(programState, new Constraint[][] {{ObjectConstraint.NOT_NULL, BooleanConstraint.FALSE}, {null}});

    programState = execute(invokeMethod(Opcodes.INVOKESPECIAL, "methodWithIntArgument", "(I)V"), startingState.stackValue(new SymbolicValue()));
    assertThat(hasConstraint(thisSv, programState, ObjectConstraint.NOT_NULL)).isTrue();
  }

  @Test
  public void test_store() throws Exception {
    int[] storeOpcodes = new int[] {Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE};
    SymbolicValue sv = new SymbolicValue();
    ProgramState startState = ProgramState.EMPTY_STATE.stackValue(sv);
    for (int opcode : storeOpcodes) {
      ProgramState programState = execute(new Instruction(opcode, 67), startState);
      assertThat(programState.getValue(67)).isEqualTo(sv);
    }
  }

  @Test
  public void test_tableswitch() throws Exception {
    Instructions instr = new Instructions();
    instr.visitVarInsn(ILOAD, 0);
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    Label l3 = new Label();
    instr.visitTableSwitchInsn(0, 2, l3, new Label[] {l0, l1, l2});
    instr.visitLabel(l0);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 1);
    instr.visitJumpInsn(GOTO, l3);
    instr.visitLabel(l1);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 2);
    instr.visitJumpInsn(GOTO, l3);
    instr.visitLabel(l2);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 3);
    instr.visitLabel(l3);
    instr.visitInsn(RETURN);
    BytecodeCFG cfg = instr.cfg();

    CFG.IBlock<Instruction> entry = cfg.entry();
    BytecodeEGWalker walker = new BytecodeEGWalker(null, null);
    walker.programState = ProgramState.EMPTY_STATE.stackValue(new SymbolicValue());
    walker.handleBlockExit(new ProgramPoint(entry));

    assertThat(walker.workList).hasSize(entry.successors().size());

    walker.workList.forEach(node -> {
      assertThat(node.programState.peekValue()).isNull();
      assertThat(entry.successors().contains(node.programPoint.block)).isTrue();
    });
  }

  @Test
  public void test_lookupswitch() throws Exception {
    Instructions instr = new Instructions();
    instr.visitVarInsn(ILOAD, 0);
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    Label l3 = new Label();
    instr.visitLookupSwitchInsn(l3, new int[] {0, 1, 2, 50}, new Label[] {l0, l1, l2, l3});
    instr.visitLabel(l0);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 1);
    instr.visitJumpInsn(GOTO, l3);
    instr.visitLabel(l1);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 2);
    instr.visitJumpInsn(GOTO, l3);
    instr.visitLabel(l2);
    instr.visitInsn(ICONST_0);
    instr.visitVarInsn(ISTORE, 3);
    instr.visitJumpInsn(GOTO, l3);
    instr.visitLabel(l3);
    instr.visitInsn(RETURN);
    BytecodeCFG cfg = instr.cfg();

    CFG.IBlock<Instruction> entry = cfg.entry();
    BytecodeEGWalker walker = new BytecodeEGWalker(null, null);
    walker.programState = ProgramState.EMPTY_STATE.stackValue(new SymbolicValue());
    walker.handleBlockExit(new ProgramPoint(entry));

    assertThat(walker.workList).hasSize(entry.successors().size());

    walker.workList.forEach(node -> {
      assertThat(node.programState.peekValue()).isNull();
      assertThat(entry.successors().contains(node.programPoint.block)).isTrue();
    });
  }

  @Test
  public void test_getstatic() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.GETSTATIC, new Instruction.FieldOrMethod("", "", "D", false)));
    assertThat(programState.peekValue()).isNotNull();
    assertThat(isDoubleOrLong(programState, programState.peekValue())).isTrue();
    programState = execute(new Instruction(Opcodes.GETSTATIC, new Instruction.FieldOrMethod("", "", "I", false)));
    assertThat(programState.peekValue()).isNotNull();
    assertThat(isDoubleOrLong(programState, programState.peekValue())).isFalse();
  }

  @Test
  public void test_putstatic() throws Exception {
    ProgramState programState = execute(new Instruction(Opcodes.PUTSTATIC), ProgramState.EMPTY_STATE.stackValue(new SymbolicValue()));
    assertThat(programState.peekValue()).isNull();
  }

  @Test
  public void test_getfield() throws Exception {
    SymbolicValue objectRef = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.GETFIELD, new Instruction.FieldOrMethod("", "", "D", false)), ProgramState.EMPTY_STATE.stackValue(objectRef));
    SymbolicValue fieldValue = programState.peekValue();
    assertThat(fieldValue).isNotNull();
    assertThat(isDoubleOrLong(programState, fieldValue)).isTrue();
    assertThat(fieldValue).isNotEqualTo(objectRef);

    programState = execute(new Instruction(Opcodes.GETFIELD, new Instruction.FieldOrMethod("", "", "I", false)), ProgramState.EMPTY_STATE.stackValue(objectRef));
    fieldValue = programState.peekValue();
    assertThat(fieldValue).isNotNull();
    assertThat(isDoubleOrLong(programState, fieldValue)).isFalse();

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.GETFIELD))).hasMessage("GETFIELD needs 1 values on stack");
  }

  @Test
  public void test_putfield() throws Exception {
    SymbolicValue objectRef = new SymbolicValue();
    SymbolicValue value = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.PUTFIELD), ProgramState.EMPTY_STATE.stackValue(objectRef).stackValue(value));
    assertThat(programState.peekValue()).isNull();

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.PUTFIELD), ProgramState.EMPTY_STATE.stackValue(value))).hasMessage("PUTFIELD needs 2 values on stack");
  }

  @Test
  public void test_newarray() throws Exception {
    SymbolicValue size = new SymbolicValue();
    int[] opcodes = {Opcodes.NEWARRAY, Opcodes.ANEWARRAY};
    for (int opcode : opcodes) {
      ProgramState programState = execute(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(size));
      assertThat(programState.peekValue()).isNotEqualTo(size);
      assertStack(programState, ObjectConstraint.NOT_NULL);

      assertThatThrownBy(() -> execute(new Instruction(opcode), ProgramState.EMPTY_STATE)).hasMessage(Printer.OPCODES[opcode] + " needs 1 values on stack");
    }
  }

  @Test
  public void test_arraylength() throws Exception {
    SymbolicValue arrayRef = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.ARRAYLENGTH), ProgramState.EMPTY_STATE.stackValue(arrayRef));
    SymbolicValue length = programState.peekValue();
    assertStack(programState, new Constraint[] {null});
    assertThat(length).isNotEqualTo(arrayRef);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.ARRAYLENGTH), ProgramState.EMPTY_STATE)).hasMessage("ARRAYLENGTH needs 1 values on stack");
  }

  @Test
  public void test_checkcast() throws Exception {
    SymbolicValue objectRef = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.CHECKCAST), ProgramState.EMPTY_STATE.stackValue(objectRef));
    assertThat(programState.peekValue()).isEqualTo(objectRef);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.CHECKCAST), ProgramState.EMPTY_STATE)).hasMessage("CHECKCAST needs 1 value on stack");
  }

  @Test
  public void test_instanceof() throws Exception {
    SymbolicValue sv = new SymbolicValue();
    ProgramState programState = execute(new Instruction(Opcodes.INSTANCEOF), ProgramState.EMPTY_STATE.stackValue(sv));
    SymbolicValue result = programState.peekValue();
    assertThat(result).isInstanceOf(SymbolicValue.InstanceOfSymbolicValue.class);
    assertThat(result.computedFrom().get(0)).isEqualTo(sv);

    assertThatThrownBy(() -> execute(new Instruction(Opcodes.INSTANCEOF))).hasMessage("INSTANCEOF needs 1 values on stack");
  }

  @Test
  public void test_monitor_enter_exit() throws Exception {
    int opcodes[] = {Opcodes.MONITORENTER, Opcodes.MONITOREXIT};

    for (int opcode : opcodes) {
      ProgramState programState = execute(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(new SymbolicValue()));
      assertEmptyStack(programState);

      assertThatThrownBy(() -> execute(new Instruction(opcode))).hasMessage(Printer.OPCODES[opcode] + " needs 1 values on stack");
    }
  }

  @Test
  public void test_multianewarray() throws Exception {
    ProgramState programState = execute(new Instruction.MultiANewArrayInsn("B", 1), ProgramState.EMPTY_STATE.stackValue(new SymbolicValue()));
    assertStack(programState, ObjectConstraint.NOT_NULL);
    programState = execute(new Instruction.MultiANewArrayInsn("B", 2), ProgramState.EMPTY_STATE
      .stackValue(new SymbolicValue())
      .stackValue(new SymbolicValue()));
    assertStack(programState, ObjectConstraint.NOT_NULL);

    assertThatThrownBy(() -> execute(new Instruction.MultiANewArrayInsn("B", 2))).hasMessage("MULTIANEWARRAY needs 2 values on stack");
  }

  @Test
  public void test_invoke_dynamic() throws Exception {
    SymbolicValue lambdaArg = new SymbolicValue();
    ProgramState programState = execute(new Instruction.InvokeDynamicInsn("(I)Ljava/util/function/Supplier;"), ProgramState.EMPTY_STATE.stackValue(lambdaArg));
    assertStack(programState, ObjectConstraint.NOT_NULL);
    assertThat(programState.peekValue()).isNotEqualTo(lambdaArg);

    programState = execute(new Instruction.InvokeDynamicInsn("()Ljava/util/function/Supplier;"), ProgramState.EMPTY_STATE);
    assertStack(programState, ObjectConstraint.NOT_NULL);

    assertThatThrownBy(() -> execute(new Instruction.InvokeDynamicInsn("()V"), ProgramState.EMPTY_STATE))
      .hasMessage("Lambda should always evaluate to target functional interface");
  }


  @Test
  public void test_compare_with_zero() {
    SymbolicValue sv = new SymbolicValue();
    int[] opcodes = {Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE};
    for (int opcode : opcodes) {
      ProgramState programState = walker.branchingState(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(sv));
      RelationalSymbolicValue relSV = (RelationalSymbolicValue) programState.peekValue();
      assertThat(relSV.getLeftOp()).isSameAs(sv);
      assertThat(relSV.getRightOp()).isNotSameAs(sv);
      assertThat(programState.getConstraints(relSV.getRightOp()).hasConstraint(DivisionByZeroCheck.ZeroConstraint.ZERO)).isTrue();
    }

    // these opcodes inverse operator and swap operands
    int[] swapOperandsOpcodes = {Opcodes.IFLE, Opcodes.IFGT};
    for (int opcode : swapOperandsOpcodes) {
      ProgramState programState = walker.branchingState(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(sv));
      RelationalSymbolicValue relSV = (RelationalSymbolicValue) programState.peekValue();
      assertThat(relSV.getRightOp()).isSameAs(sv);
      assertThat(relSV.getLeftOp()).isNotSameAs(sv);
      assertThat(programState.getConstraints(relSV.getLeftOp()).hasConstraint(DivisionByZeroCheck.ZeroConstraint.ZERO)).isTrue();
    }
  }

  @Test
  public void test_compare_with_null() {
    SymbolicValue sv = new SymbolicValue();
    int[] opcodes = {Opcodes.IFNULL, Opcodes.IFNONNULL};
    for (int opcode : opcodes) {
      ProgramState programState = walker.branchingState(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(sv));
      RelationalSymbolicValue relSV = (RelationalSymbolicValue) programState.peekValue();
      assertThat(relSV.getLeftOp()).isSameAs(sv);
      assertThat(relSV.getRightOp()).isSameAs(SymbolicValue.NULL_LITERAL);
    }
  }

  @Test
  public void test_compare_instructions() {
    int[] opcodes = {Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE};
    SymbolicValue left = new SymbolicValue();
    SymbolicValue right = new SymbolicValue();
    for (int opcode : opcodes) {
      ProgramState programState = walker.branchingState(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(left).stackValue(right));
      RelationalSymbolicValue relSV = (RelationalSymbolicValue) programState.peekValue();
      assertThat(relSV.getLeftOp()).isSameAs(left);
      assertThat(relSV.getRightOp()).isSameAs(right);
    }

    // these opcodes inverse operator and swap operands
    int[] swapOperandsOpcodes = {Opcodes.IF_ICMPLE, Opcodes.IF_ICMPGT};
    for (int opcode : swapOperandsOpcodes) {
      ProgramState programState = walker.branchingState(new Instruction(opcode), ProgramState.EMPTY_STATE.stackValue(left).stackValue(right));
      RelationalSymbolicValue relSV = (RelationalSymbolicValue) programState.peekValue();
      assertThat(relSV.getRightOp()).isSameAs(left);
      assertThat(relSV.getLeftOp()).isSameAs(right);
    }
  }

  @Test
  public void test_invalid_branch_instruction() {
    assertThatThrownBy(() -> walker.branchingState(new Instruction(Opcodes.GOTO), ProgramState.EMPTY_STATE))
        .isInstanceOf(IllegalStateException.class);
  }


  @Test
  public void test_conversion() throws Exception {
    int[] toLongOrDouble = {Opcodes.I2D, Opcodes.I2L, Opcodes.F2D, Opcodes.F2L};
    for (int opcode : toLongOrDouble) {
      SymbolicValue sv = new SymbolicValue();
      ProgramState initialPs = ProgramState.EMPTY_STATE.stackValue(sv);
      ProgramState ps = execute(new Instruction(opcode), initialPs);
      assertThat(isDoubleOrLong(ps, sv)).isTrue();
      assertThatThrownBy(() -> execute(new Instruction(opcode))).hasMessage(Printer.OPCODES[opcode] + " needs value on stack");
    }
    int[] fromLongOrDouble = {Opcodes.D2F, Opcodes.D2I, Opcodes.L2F, Opcodes.L2I};
    for (int opcode : fromLongOrDouble) {
      SymbolicValue sv = new SymbolicValue();
      ProgramState initialPs = ProgramState.EMPTY_STATE.stackValue(sv);
      initialPs = setDoubleOrLong(initialPs, sv, true);
      ProgramState ps = execute(new Instruction(opcode), initialPs);
      assertThat(isDoubleOrLong(ps, sv)).isFalse();
      assertThatThrownBy(() -> execute(new Instruction(opcode))).hasMessage(Printer.OPCODES[opcode] + " needs value on stack");
    }
  }

  private boolean hasConstraint(SymbolicValue sv, ProgramState ps, Constraint constraint) {
    ConstraintsByDomain constraints = ps.getConstraints(sv);
    return constraints != null && constraints.get(constraint.getClass()) == constraint;
  }

  private Instruction invokeStatic(String methodName, String desc) {
    return invokeMethod(Opcodes.INVOKESTATIC, methodName, desc);
  }

  private Instruction invokeMethod(int opcode, String methodName, String desc) {
    return new Instruction(opcode, new Instruction.FieldOrMethod("org/sonar/java/bytecode/se/BytecodeEGWalkerExecuteTest", methodName, desc, false));
  }

  private void assertStack(ProgramState ps, Constraint... constraints) {
    Constraint[][] cs = new Constraint[constraints.length][1];
    int i = 0;
    for (Constraint constraint : constraints) {
      cs[i] = new Constraint[] {constraint};
      i++;
    }
    assertStack(ps, cs);
  }

  private void assertStack(ProgramState ps, Constraint[]... constraints) {
    ProgramState.Pop pop = ps.unstackValue(constraints.length);
    assertEmptyStack(pop.state);
    assertThat(pop.valuesAndSymbols).hasSize(constraints.length);
    List<SymbolicValue> symbolicValues = pop.values;
    int idx = 0;
    for (SymbolicValue sv : symbolicValues) {
      ConstraintsByDomain constraintsByDomain = ps.getConstraints(sv);
      if (constraintsByDomain != null) {
        constraintsByDomain = constraintsByDomain.remove(BytecodeEGWalker.StackValueCategoryConstraint.class);
      }
      for (Constraint expectedConstraint : constraints[idx]) {
        if (expectedConstraint != null) {
          Class<? extends Constraint> expectedConstraintDomain = expectedConstraint.getClass();
          Constraint constraint = constraintsByDomain.get(expectedConstraintDomain);
          assertThat(constraint).isEqualTo(expectedConstraint);
          constraintsByDomain = constraintsByDomain.remove(expectedConstraintDomain);
        } else {
          assertThat(constraintsByDomain).isNull();
        }
      }
      if (constraintsByDomain != null) {
        assertThat(constraintsByDomain.isEmpty()).isTrue();
      }
      idx++;
    }
  }

  private void assertEmptyStack(ProgramState programState) {
    assertThat(programState.peekValue()).isNull();
  }

  private ProgramState execute(Instruction instruction) {
    return execute(instruction, ProgramState.EMPTY_STATE);
  }

  private ProgramState execute(Instruction instruction, ProgramState startingState) {
    CFG.IBlock block = mock(CFG.IBlock.class);
    when(block.successors()).thenReturn(Collections.emptySet());
    walker.programPosition = new ProgramPoint(block);
    walker.programState = startingState;
    walker.workList.clear();
    walker.executeInstruction(instruction);
    ProgramState programState = walker.programState;
    // X-PROC will enqueue new nodes
    if (instruction.isInvoke() && !walker.workList.isEmpty()) {
      programState = walker.workList.getFirst().programState;
    }
    return programState;
  }

  private static boolean isDoubleOrLong(ProgramState programState, SymbolicValue sv) {
    return programState.getConstraint(sv, BytecodeEGWalker.StackValueCategoryConstraint.class) == BytecodeEGWalker.StackValueCategoryConstraint.LONG_OR_DOUBLE;
  }

  private static ProgramState setDoubleOrLong(ProgramState programState, SymbolicValue sv, boolean value) {
    if (value) {
      return programState.addConstraint(sv, BytecodeEGWalker.StackValueCategoryConstraint.LONG_OR_DOUBLE);
    } else {
      return programState.removeConstraintsOnDomain(sv, BytecodeEGWalker.StackValueCategoryConstraint.class);
    }
  }

  @Test
  public void test_enqueuing_only_happy_path() {
    BytecodeCFG cfg = SETestUtils.bytecodeCFG(TRY_CATCH_SIGNATURE, squidClassLoader);
    BytecodeCFG.Block b2 = cfg.blocks().get(2);
    walker.workList.clear();
    walker.programState = ProgramState.EMPTY_STATE.stackValue(new SymbolicValue());
    walker.handleBlockExit(new ProgramPoint(b2));
    assertThat(walker.workList).hasSize(1);
    assertThat(walker.workList.getFirst().programPoint.block.id()).isEqualTo(3);
  }

  @Test
  public void test_enqueuing_exceptional_yields() {
    BytecodeCFG cfg = SETestUtils.bytecodeCFG(TRY_CATCH_SIGNATURE, squidClassLoader);
    BytecodeCFG.Block b2 = cfg.blocks().get(2);
    walker.programState = ProgramState.EMPTY_STATE.stackValue(new SymbolicValue()).stackValue(new SymbolicValue());
    walker.programPosition = new ProgramPoint(b2).next().next();

    walker.executeInstruction(b2.elements().get(3));
    assertThat(walker.workList).hasSize(4);
  }

  @Test
  public void test_enqueuing_exceptional_yields2() {
    BytecodeCFG cfg = SETestUtils.bytecodeCFG(TRY_WRONG_CATCH_SIGNATURE, squidClassLoader);
    BytecodeCFG.Block b2 = cfg.blocks().get(2);
    walker.programState = ProgramState.EMPTY_STATE.stackValue(new SymbolicValue()).stackValue(new SymbolicValue());
    walker.programPosition = new ProgramPoint(b2).next().next();
    walker.executeInstruction(b2.elements().get(3));

    assertThat(walker.workList).hasSize(3);
    assertThat(walker.workList.pop().programState.exitValue()).isNotNull()
      .isInstanceOf(SymbolicValue.ExceptionalSymbolicValue.class)
      .extracting(sv -> ((SymbolicValue.ExceptionalSymbolicValue) sv).exceptionType().fullyQualifiedName()).containsExactly("java.lang.IllegalStateException");
    assertThat(walker.workList.pop().programState.exitValue()).isNull();
  }

  @Test
  public void test_switch_enqueuing_in_trycatch() throws Exception {
    Instructions mv = new Instructions();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
    mv.visitLabel(l0);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitLookupSwitchInsn(l1, new int[] {0}, new Label[] {l1});
    mv.visitLabel(l1);
    Label l3 = new Label();
    mv.visitJumpInsn(GOTO, l3);
    mv.visitLabel(l2);
    mv.visitVarInsn(ASTORE, 2);
    mv.visitLabel(l3);
    mv.visitInsn(RETURN);

    BytecodeCFG cfg = mv.cfg();
    BytecodeCFG.Block switchBlock = cfg.blocks().get(2);

    assertThat(switchBlock.terminator().opcode).isEqualTo(Opcodes.LOOKUPSWITCH);
    walker.programState = ProgramState.EMPTY_STATE;
    walker.handleBlockExit(new ProgramPoint(switchBlock));
    assertThat(walker.workList).hasSize(1);
  }

  @Test
  public void test_goto_enqueuing_in_trycatch() throws Exception {
    Instructions mv = new Instructions();
    /*
     void test_goto(int i) {
       try {
          switch (i) {
            case 0:
              i = 1; // GOTO within try-catch
              break;
            case 1:
              i = 2;
              break;
          }
          i = 3;
        } catch (Exception e) {
          i = 4;
       }
     }
    */
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
    mv.visitLabel(l0);
    mv.visitVarInsn(ILOAD, 1);
    Label l3 = new Label();
    Label l4 = new Label();
    Label l5 = new Label();
    mv.visitLookupSwitchInsn(l5, new int[] {0, 1}, new Label[] {l3, l4});
    mv.visitLabel(l3);
    mv.visitInsn(ICONST_1);
    mv.visitVarInsn(ISTORE, 1);
    mv.visitJumpInsn(GOTO, l5); // tested GOTO instruction
    mv.visitLabel(l4);
    mv.visitInsn(ICONST_2);
    mv.visitVarInsn(ISTORE, 1);
    mv.visitLabel(l5);
    mv.visitInsn(ICONST_3);
    mv.visitVarInsn(ISTORE, 1);
    mv.visitLabel(l1);
    Label l6 = new Label();
    mv.visitJumpInsn(GOTO, l6);
    mv.visitLabel(l2);
    mv.visitVarInsn(ASTORE, 2);
    mv.visitInsn(ICONST_4);
    mv.visitVarInsn(ISTORE, 1);
    mv.visitLabel(l6);
    mv.visitInsn(RETURN);
    BytecodeCFG cfg = mv.cfg();

    BytecodeCFG.Block gotoBlock = cfg.blocks().get(4);
    assertThat(gotoBlock.terminator().opcode).isEqualTo(GOTO);
    walker.programState = ProgramState.EMPTY_STATE;
    walker.handleBlockExit(new ProgramPoint(gotoBlock));

    assertThat(walker.workList).hasSize(1);
  }

  @Test
  public void test_analysis_failure() throws Exception {
    BytecodeEGWalker walkerSpy = spy(walker);
    IllegalStateException ex = new IllegalStateException();
    doThrow(ex).when(walkerSpy).executeInstruction(any());

    assertThatThrownBy(() -> walkerSpy.getMethodBehavior("java.lang.String#valueOf(Z)Ljava/lang/String;", squidClassLoader))
      .isInstanceOf(BytecodeEGWalker.BytecodeAnalysisException.class)
      .hasMessage("Failed dataflow analysis for java.lang.String#valueOf(Z)Ljava/lang/String;")
      .hasCause(ex);
  }

  @Test
  public void method_returning_new_should_have_not_null_result() {
    MethodBehavior mb = walker.getMethodBehavior(BytecodeEGWalkerExecuteTest.class.getCanonicalName() + "#newObject()Ljava/lang/Object;", squidClassLoader);
    List<MethodYield> yields = mb.yields();
    assertThat(yields).hasSize(1);
    MethodYield yield = yields.get(0);
    assertThat(yield).isInstanceOf(HappyPathYield.class);
    ConstraintsByDomain resultConstraint = ((HappyPathYield) yield).resultConstraint();
    assertThat(resultConstraint).isNotNull();
    assertThat(resultConstraint.get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    TypedConstraint typeConstraint = (TypedConstraint) resultConstraint.get(TypedConstraint.class);
    assertThat(typeConstraint.type.equals("java.lang.String")).isTrue();
  }

  @Test
  public void behavior_should_have_declared_exceptions() {
    MethodBehavior mb = walker.getMethodBehavior(BytecodeEGWalkerExecuteTest.class.getCanonicalName() + "#throwing()V", squidClassLoader);
    assertThat(mb.isComplete()).isFalse();
    assertThat(mb.getDeclaredExceptions()).containsExactly("java.io.IOException");
  }

  @Test
  public void exceptional_paths_should_be_enqueued() {
    MethodBehavior mb = walker.getMethodBehavior(BytecodeEGWalkerExecuteTest.class.getCanonicalName() + "#enqueue_exceptional_paths(Lorg/sonar/java/bytecode/se/BytecodeEGWalkerExecuteTest;)Ljava/lang/Object;", squidClassLoader);
    assertThat(mb.yields()).hasSize(2);
    List<Constraint> resultConstraints = mb.yields().stream().map(y -> ((HappyPathYield) y).resultConstraint()).map(c -> c.get(ObjectConstraint.class)).collect(Collectors.toList());
    assertThat(resultConstraints).contains(ObjectConstraint.NOT_NULL, ObjectConstraint.NULL);
  }

  /**
   * ---------------- used by test checking methods ---------
   */
  static void staticMethod() {
  }

  static boolean staticBooleanMethod() {
    return false;
  }

  static int staticIntMethodWithIntArgument(int i) {
    return 0;
  }

  static void staticMethodWithIntIntArgument(int i1, int i2) {
  }

  static boolean staticBooleanMethodWithIntArgument(int i) {
    return true;
  }

  void methodWithoutArgument() {
  }

  void methodWithIntArgument(int i) {
  }

  boolean booleanMethod() {
    return false;
  }

  int intMethodWithIntArgument(int i) {
    return 0;
  }

  void methodWithIntIntArgument(int i1, int i2) {
  }

  static long staticReturningLong() { return 1L; }
  static double staticReturningDouble() { return 1.0d; }

  long returningLong() { return 1L; }

  double returningDouble() { return 1.0d; }

  static Object returnArg(Object o) { return o; }

  final void finalVoid() {}

  static Object newObject() { return new String(); }

  private void tryCatch(boolean param) {
    try {
      staticBooleanMethod();
      Preconditions.checkState(param);
    } catch (IllegalStateException e) {
      e.printStackTrace();
    } finally {
      System.out.println("finally");
    }
  }

  private void tryWrongCatch(boolean param) {
    try {
      staticBooleanMethod();
      Preconditions.checkState(param);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } finally {
      System.out.println("finally");
    }
  }

  void throwing() throws IOException {}

  static Object enqueue_exceptional_paths(BytecodeEGWalkerExecuteTest o) {
    try {
      o.throwing();
      return new Object();
    } catch (IOException e) {
      return null;
    }
  }
}
