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

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.cfg.Instruction;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramPoint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.DivisionByZeroCheck.ZeroConstraint;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.ExceptionalYield;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BytecodeSECheckTest implements BytecodeSECheck {

  private static SquidClassLoader squidClassLoader;
  private static BytecodeEGWalker walker;
  private static SemanticModel semanticModel;
  private SymbolicValue sv1;
  private SymbolicValue sv2;
  private ProgramState zeroZeroPs;
  private ProgramState zeroNonZeroPs;
  private ProgramState nonZeroZeroPs;
  private ProgramState nonZeroNonZeroPs;
  private ProgramState noConstraints;

  @BeforeClass
  public static void initializeWalker() {
    List<File> files = new ArrayList<>();
    files.add(new File("target/test-classes"));
    squidClassLoader = new SquidClassLoader(files);
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse("class A {}");
    semanticModel = SemanticModel.createFor(tree, squidClassLoader);
    walker = getEGWalker();
  }

  @Before
  public void setUp() {
    sv1 = new SymbolicValue();
    sv2 = new SymbolicValue();

    zeroZeroPs = ProgramState.EMPTY_STATE;
    zeroZeroPs = zeroZeroPs.stackValue(sv1).addConstraints(sv1, ConstraintsByDomain.empty().put(ZeroConstraint.ZERO).put(BooleanConstraint.FALSE));
    zeroZeroPs = zeroZeroPs.stackValue(sv2).addConstraints(sv2, ConstraintsByDomain.empty().put(ZeroConstraint.ZERO).put(BooleanConstraint.FALSE));

    zeroNonZeroPs = ProgramState.EMPTY_STATE;
    zeroNonZeroPs = zeroNonZeroPs.stackValue(sv1).addConstraints(sv1, ConstraintsByDomain.empty().put(ZeroConstraint.ZERO).put(BooleanConstraint.FALSE));
    zeroNonZeroPs = zeroNonZeroPs.stackValue(sv2).addConstraints(sv2, ConstraintsByDomain.empty().put(ZeroConstraint.NON_ZERO).put(BooleanConstraint.TRUE));

    nonZeroZeroPs = ProgramState.EMPTY_STATE;
    nonZeroZeroPs = nonZeroZeroPs.stackValue(sv1).addConstraints(sv1, ConstraintsByDomain.empty().put(ZeroConstraint.NON_ZERO).put(BooleanConstraint.TRUE));
    nonZeroZeroPs = nonZeroZeroPs.stackValue(sv2).addConstraints(sv2, ConstraintsByDomain.empty().put(ZeroConstraint.ZERO).put(BooleanConstraint.FALSE));

    nonZeroNonZeroPs = ProgramState.EMPTY_STATE;
    nonZeroNonZeroPs = nonZeroNonZeroPs.stackValue(sv1).addConstraints(sv1, ConstraintsByDomain.empty().put(ZeroConstraint.NON_ZERO));
    nonZeroNonZeroPs = nonZeroNonZeroPs.stackValue(sv2).addConstraints(sv2, ConstraintsByDomain.empty().put(ZeroConstraint.NON_ZERO));

    noConstraints = ProgramState.EMPTY_STATE.stackValue(sv1).stackValue(sv2);
  }

  @Test
  public void zeroness_check_add() {
    int[] opCodes = {Opcodes.DADD, Opcodes.FADD, Opcodes.IADD, Opcodes.LADD};

    for (int addOpCode : opCodes) {
      Instruction instruction = new Instruction(addOpCode);
      ProgramState ps = execute(instruction, zeroZeroPs);
      SymbolicValue peekValue = ps.peekValue();
      assertThat(peekValue).isEqualTo(sv2);
      ConstraintsByDomain constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isEqualTo(BooleanConstraint.FALSE);

      ps = execute(instruction, zeroNonZeroPs);
      peekValue = ps.peekValue();
      assertThat(peekValue).isEqualTo(sv2);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.NON_ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isEqualTo(BooleanConstraint.TRUE);

      ps = execute(instruction, nonZeroZeroPs);
      peekValue = ps.peekValue();
      assertThat(peekValue).isEqualTo(sv1);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.NON_ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isEqualTo(BooleanConstraint.TRUE);

      ps = execute(instruction, nonZeroNonZeroPs);
      peekValue = ps.peekValue();
      assertThat(peekValue).isNotIn(sv1, sv2);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isNull();
      assertThat(constraints.get(BooleanConstraint.class)).isNull();
    }
  }

  @Test
  public void zeroness_check_mul() {
    int[] opCodes = {Opcodes.DMUL, Opcodes.FMUL, Opcodes.IMUL, Opcodes.LMUL};

    for (int mulOpCode : opCodes) {
      Instruction instruction = new Instruction(mulOpCode);
      ProgramState ps = execute(instruction, zeroZeroPs);
      SymbolicValue peekValue = ps.peekValue();
      assertThat(peekValue).isEqualTo(sv2);
      ConstraintsByDomain constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isEqualTo(BooleanConstraint.FALSE);

      ps = execute(instruction, zeroNonZeroPs);
      peekValue = ps.peekValue();
      assertThat(peekValue).isEqualTo(sv1);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isEqualTo(BooleanConstraint.FALSE);

      ps = execute(instruction, nonZeroZeroPs);
      peekValue = ps.peekValue();
      assertThat(peekValue).isEqualTo(sv2);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isEqualTo(BooleanConstraint.FALSE);

      ps = execute(instruction, nonZeroNonZeroPs);
      peekValue = ps.peekValue();
      assertThat(peekValue).isNotIn(sv1, sv2);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.NON_ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isNull();

      ps = execute(instruction, noConstraints);
      peekValue = ps.peekValue();
      assertThat(peekValue).isNotIn(sv1, sv2);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isNull();
      assertThat(constraints.get(BooleanConstraint.class)).isNull();
    }
  }

  @Test
  public void zeroness_check_div_rem() {
    int[] opCodes = {
      Opcodes.DDIV, Opcodes.FDIV, Opcodes.IDIV, Opcodes.LDIV,
      Opcodes.DREM, Opcodes.FREM, Opcodes.IREM, Opcodes.LREM
    };

    Set<Integer> remOpcodes = ImmutableSet.of(Opcodes.DREM, Opcodes.FREM, Opcodes.IREM, Opcodes.LREM);

    for (int divOpCode : opCodes) {
      Instruction instruction = new Instruction(divOpCode);
      ProgramState ps = execute(instruction, zeroZeroPs);
      assertThat(ps).isNull();

      ps = execute(instruction, zeroNonZeroPs);
      assertThat(ps).isNull();

      ps = execute(instruction, nonZeroZeroPs);
      SymbolicValue result = ps.peekValue();
      assertThat(result).isEqualTo(sv2);
      ConstraintsByDomain constraints = ps.getConstraints(result);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isEqualTo(BooleanConstraint.FALSE);

      ps = execute(instruction, nonZeroNonZeroPs);
      result = ps.peekValue();
      assertThat(result).isNotIn(sv1, sv2);
      constraints = ps.getConstraints(result);
      if (remOpcodes.contains(divOpCode)) {
        assertThat(constraints.get(ZeroConstraint.class)).isNull();
      } else {
        assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.NON_ZERO);
      }
      assertThat(constraints.get(BooleanConstraint.class)).isNull();

      ps = execute(instruction, noConstraints);
      result = ps.peekValue();
      assertThat(result).isNotIn(sv1, sv2);
      constraints = ps.getConstraints(result);
      assertThat(constraints.get(ZeroConstraint.class)).isNull();
      assertThat(constraints.get(BooleanConstraint.class)).isNull();
    }
  }

  @Test
  public void zeroness_check_negations() {
    SymbolicValue sv1 = new SymbolicValue();

    int[] opCodes = {Opcodes.DNEG, Opcodes.FNEG, Opcodes.INEG, Opcodes.LNEG};

    ProgramState zeroPs = ProgramState.EMPTY_STATE;
    zeroPs = zeroPs.stackValue(sv1).addConstraints(sv1, ConstraintsByDomain.empty().put(ZeroConstraint.ZERO).put(BooleanConstraint.FALSE));

    ProgramState nonZeroPs = ProgramState.EMPTY_STATE;
    nonZeroPs = nonZeroPs.stackValue(sv1).addConstraints(sv1, ConstraintsByDomain.empty().put(ZeroConstraint.NON_ZERO).put(BooleanConstraint.TRUE));

    ProgramState noConstraint = ProgramState.EMPTY_STATE.stackValue(sv1);

    for (int negOpCode : opCodes) {
      Instruction instruction = new Instruction(negOpCode);
      ProgramState ps = execute(instruction, zeroPs);
      SymbolicValue peekValue = ps.peekValue();
      assertThat(peekValue).isEqualTo(sv1);
      ConstraintsByDomain constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isEqualTo(BooleanConstraint.FALSE);

      ps = execute(instruction, nonZeroPs);
      peekValue = ps.peekValue();
      assertThat(peekValue).isNotEqualTo(sv1);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.NON_ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isNull();

      ps = execute(instruction, noConstraint);
      peekValue = ps.peekValue();
      assertThat(peekValue).isNotEqualTo(sv1);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isNull();
      assertThat(constraints.get(BooleanConstraint.class)).isNull();
    }
  }

  @Test
  public void zeroness_check_shifts() {
    int[] opCodes = {Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR, Opcodes.IUSHR, Opcodes.LUSHR};

    for (int shiftOpCode : opCodes) {
      Instruction instruction = new Instruction(shiftOpCode);
      ProgramState ps = execute(instruction, zeroZeroPs);
      SymbolicValue peekValue = ps.peekValue();
      assertThat(peekValue).isEqualTo(sv2);
      ConstraintsByDomain constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isEqualTo(BooleanConstraint.FALSE);

      ps = execute(instruction, zeroNonZeroPs);
      peekValue = ps.peekValue();
      assertThat(peekValue).isNotIn(sv1, sv2);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.NON_ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isNull();

      ps = execute(instruction, nonZeroZeroPs);
      peekValue = ps.peekValue();
      assertThat(peekValue).isEqualTo(sv2);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isEqualTo(BooleanConstraint.FALSE);

      ps = execute(instruction, nonZeroNonZeroPs);
      peekValue = ps.peekValue();
      assertThat(peekValue).isNotIn(sv1, sv2);
      constraints = ps.getConstraints(peekValue);
      assertThat(constraints.get(ZeroConstraint.class)).isEqualTo(ZeroConstraint.NON_ZERO);
      assertThat(constraints.get(BooleanConstraint.class)).isNull();
    }
  }

  @Test
  public void method_with_numerical_operations() throws Exception {
    BytecodeEGWalker walker = getEGWalker();
    MethodBehavior behavior = walker.getMethodBehavior("org.sonar.java.bytecode.se.BytecodeSECheckTest#foo(II)I", squidClassLoader);

    assertThat(behavior).isNotNull();
    assertThat(behavior.yields()).hasSize(3);
    assertThat(behavior.happyPathYields().map(HappyPathYield::toString).collect(Collectors.toList())).containsExactly("{params: [[], []], result: null (0)}");
    assertThat(behavior.exceptionalPathYields().map(ExceptionalYield::toString).collect(Collectors.toList()))
      .containsExactly("{params: [[NON_ZERO], []], exceptional (java.lang.IndexOutOfBoundsException)}",
        "{params: [[], []], exceptional (java.lang.IndexOutOfBoundsException)}");
  }

  private static BytecodeEGWalker getEGWalker() {
    BehaviorCache behaviorCache = new BehaviorCache(squidClassLoader);
    behaviorCache.setFileContext(null, semanticModel);
    return new BytecodeEGWalker(behaviorCache, semanticModel);
  }

  private static ProgramState execute(Instruction instruction, ProgramState startingState) {
    walker.workList.clear();
    ProgramPoint programPoint = mock(ProgramPoint.class);
    when(programPoint.next()).thenReturn(programPoint);
    ExplodedGraph.Node startingNode = walker.explodedGraph.node(programPoint, startingState);
    walker.setNode(startingNode);
    walker.executeInstruction(instruction);
    if (walker.workList.isEmpty()) {
      return null;
    }
    return walker.workList.getFirst().programState;
  }

  // --------------------------- Methods used for behaviors -----------------------------------------------------------

  static int foo(int i1, int i2) {
    if (i1 < 0 || i1 > i2) {
      throw new IndexOutOfBoundsException();
    }
    return i1;
  }
}
