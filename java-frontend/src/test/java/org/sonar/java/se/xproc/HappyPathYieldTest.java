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
package org.sonar.java.se.xproc;

import org.junit.Test;

import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.SETestUtils.getMethodBehavior;
import static org.sonar.java.se.SETestUtils.mockMethodBehavior;

public class HappyPathYieldTest {

  ConstraintsByDomain NOT_NULL_CONSTRAINT = ConstraintsByDomain.empty().put(ObjectConstraint.NOT_NULL);

  @Test
  public void test_equals() {
    MethodBehavior mb = mockMethodBehavior(1, false);

    HappyPathYield yield = new HappyPathYield(mb);
    HappyPathYield otherYield = new HappyPathYield(mb);

    assertThat(yield).isNotEqualTo(null);
    assertThat(yield).isEqualTo(yield);
    assertThat(yield).isEqualTo(otherYield);

    // same arity and constraints but different return value
    otherYield.setResult(0, yield.resultConstraint());
    assertThat(yield).isNotEqualTo(otherYield);

    // same arity but different return constraint
    otherYield = new HappyPathYield(mb);
    otherYield.setResult(yield.resultIndex(), NOT_NULL_CONSTRAINT);
    assertThat(yield).isNotEqualTo(otherYield);

    // same return constraint
    yield.setResult(-1, NOT_NULL_CONSTRAINT);
    otherYield = new HappyPathYield(mb);
    otherYield.setResult(-1, NOT_NULL_CONSTRAINT);
    assertThat(yield).isEqualTo(otherYield);

    // same arity and parameters but exceptional yield
    assertThat(yield).isNotEqualTo(new ExceptionalYield(mb));
  }

  @Test
  public void test_hashCode() {
    MethodBehavior mb = mockMethodBehavior(0, false);

    HappyPathYield methodYield = new HappyPathYield(mb);
    HappyPathYield other = new HappyPathYield(mb);

    // same values for same yields
    assertThat(methodYield.hashCode()).isEqualTo(other.hashCode());

    // different values for different yields
    other.setResult(-1, NOT_NULL_CONSTRAINT);
    assertThat(methodYield.hashCode()).isNotEqualTo(other.hashCode());

    // exceptional method yield
    assertThat(methodYield.hashCode()).isNotEqualTo(new ExceptionalYield(mb));
  }

  @Test
  public void test_toString() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/HappyPathYields.java");
    Set<String> yieldsToString = getMethodBehavior(sev, "bar").yields().stream().map(MethodYield::toString).collect(Collectors.toSet());
    assertThat(yieldsToString).contains(
      "{params: [[TRUE], [NOT_NULL]], result: null (-1)}",
      "{params: [[FALSE], []], result: null (-1)}");
  }

}
