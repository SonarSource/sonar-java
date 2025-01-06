/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.se.xproc;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.utils.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.utils.SETestUtils.getMethodBehavior;
import static org.sonar.java.se.utils.SETestUtils.mockMethodBehavior;

class HappyPathYieldTest {

  private static final ConstraintsByDomain NOT_NULL_CONSTRAINT = ConstraintsByDomain.empty().put(ObjectConstraint.NOT_NULL);

  @Test
  void test_equals() {
    MethodBehavior mb = mockMethodBehavior(1, false);

    HappyPathYield happyPathYield = new HappyPathYield(mb);
    HappyPathYield otherYield = new HappyPathYield(mb);

    assertThat(happyPathYield)
      .isNotEqualTo(null)
      .isEqualTo(happyPathYield)
      .isEqualTo(otherYield);

    // same arity and constraints but different return value
    otherYield.setResult(0, happyPathYield.resultConstraint());
    assertThat(happyPathYield).isNotEqualTo(otherYield);

    // same arity but different return constraint
    otherYield = new HappyPathYield(mb);
    otherYield.setResult(happyPathYield.resultIndex(), NOT_NULL_CONSTRAINT);
    assertThat(happyPathYield).isNotEqualTo(otherYield);

    // same return constraint
    happyPathYield.setResult(-1, NOT_NULL_CONSTRAINT);
    otherYield = new HappyPathYield(mb);
    otherYield.setResult(-1, NOT_NULL_CONSTRAINT);
    assertThat(happyPathYield)
      .isEqualTo(otherYield)
      // same arity and parameters but exceptional yield
      .isNotEqualTo(new ExceptionalYield(mb));
  }

  @Test
  void test_hashCode() {
    MethodBehavior mb = mockMethodBehavior(0, false);

    HappyPathYield methodYield = new HappyPathYield(mb);
    HappyPathYield other = new HappyPathYield(mb);

    // same values for same yields
    assertThat(methodYield).hasSameHashCodeAs(other);

    // different values for different yields
    other.setResult(-1, NOT_NULL_CONSTRAINT);
    assertThat(methodYield.hashCode()).isNotEqualTo(other.hashCode());

    // exceptional method yield
    assertThat(methodYield.hashCode()).isNotEqualTo(new ExceptionalYield(mb).hashCode());
  }

  @Test
  void test_toString() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/HappyPathYields.java", new NullDereferenceCheck());
    Set<String> yieldsToString = getMethodBehavior(sev, "bar").yields().stream().map(MethodYield::toString).collect(Collectors.toSet());
    assertThat(yieldsToString).contains(
      "{params: [[TRUE], [NOT_NULL]], result: null (-1)}",
      "{params: [[FALSE], []], result: null (-1)}");
  }

}
