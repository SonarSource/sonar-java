/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.se.checks;

import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValueTestUtil;
import org.sonar.java.se.utils.SETestUtils;
import org.sonarsource.analyzer.commons.collections.ListUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.se.checks.DivisionByZeroCheck.ZeroConstraint.NON_ZERO;
import static org.sonar.java.se.checks.DivisionByZeroCheck.ZeroConstraint.ZERO;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.GREATER_THAN_OR_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.LESS_THAN;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_EQUAL;

class DivisionByZeroCheckTest {

  @Test
  void test() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/DivisionByZeroCheck.java"))
      .withCheck(new DivisionByZeroCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void primitives() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/DivisionByZeroCheckPrimitives.java"))
      .withCheck(new DivisionByZeroCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_noncompiling() {
    SECheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("symbolicexecution/checks/DivisionByZeroCheck.java"))
      .withCheck(new DivisionByZeroCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void invocation_leading_to_ArithmeticException() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/MethodInvocationLeadingToDivisionByZero.java"))
      .withCheck(new DivisionByZeroCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_zero_constraint_copy() throws Exception {
    SymbolicValue a = new SymbolicValue();
    SymbolicValue b = new SymbolicValue();
    DivisionByZeroCheck.ZeroConstraint bConstraint = copyConstraint(a, b, EQUAL, ZERO);
    assertThat(bConstraint).isEqualTo(ZERO);

    bConstraint = copyConstraint(a, b, NOT_EQUAL, NON_ZERO);
    assertThat(bConstraint).isEqualTo(NON_ZERO);
    bConstraint = copyConstraint(a, b, LESS_THAN, NON_ZERO);
    assertThat(bConstraint).isEqualTo(NON_ZERO);

    bConstraint = copyConstraint(a, b, GREATER_THAN_OR_EQUAL, null);
    assertThat(bConstraint).isNull();
  }

  private DivisionByZeroCheck.ZeroConstraint copyConstraint(SymbolicValue a, SymbolicValue b, RelationalSymbolicValue.Kind relation,
    @Nullable DivisionByZeroCheck.ZeroConstraint expected) {
    ProgramState ps = ListUtils.getOnlyElement(a.setConstraint(ProgramState.EMPTY_STATE, ZERO));
    RelationalSymbolicValue rel = new RelationalSymbolicValue(relation);
    SymbolicValueTestUtil.computedFrom(rel, b, a);
    ps = ListUtils.getOnlyElement(rel.setConstraint(ps, BooleanConstraint.TRUE));
    return ps.getConstraint(b, DivisionByZeroCheck.ZeroConstraint.class);
  }
  
}
