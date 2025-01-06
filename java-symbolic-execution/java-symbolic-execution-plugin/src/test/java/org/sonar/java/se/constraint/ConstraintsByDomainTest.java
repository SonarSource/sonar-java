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
package org.sonar.java.se.constraint;

import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintsByDomainTest {

  @Test
  void test_isEmpty() {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty();
    assertThat(constraints.isEmpty()).isTrue();
    constraints = constraints.put(ObjectConstraint.NULL);
    assertThat(constraints.isEmpty()).isFalse();
  }

  @Test
  void test_remove() {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty().put(ObjectConstraint.NULL);
    assertThat(constraints.get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NULL);
    constraints = constraints.remove(ObjectConstraint.class);
    assertThat(constraints).isSameAs(ConstraintsByDomain.empty());

    ConstraintsByDomain c1 = ConstraintsByDomain.empty().put(ObjectConstraint.NOT_NULL);
    ConstraintsByDomain c2 = c1.remove(BooleanConstraint.class);
    assertThat(c1).isSameAs(c2);
  }

  @Test
  void test_domains() {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty();
    assertThat(constraints.domains()).isEmpty();
    constraints = constraints.put(ObjectConstraint.NOT_NULL).put(BooleanConstraint.TRUE);
    assertThat(constraints.domains()).contains(ObjectConstraint.class, BooleanConstraint.class);
  }

  @Test
  void test_constraints_stream() {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty();
    assertThat(constraints.stream()).isEmpty();
    constraints = constraints.put(ObjectConstraint.NOT_NULL).put(BooleanConstraint.TRUE);
    assertThat(constraints.stream()).contains(ObjectConstraint.NOT_NULL, BooleanConstraint.TRUE);
  }

  @Test
  void test_put() {
    ConstraintsByDomain c1 = ConstraintsByDomain.empty().put(ObjectConstraint.NOT_NULL);
    ConstraintsByDomain c2 = c1.put(ObjectConstraint.NOT_NULL);
    assertThat(c1).isSameAs(c2);
  }

  @Test
  void test_forEach() {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty();
    Counter counter = new Counter();
    constraints.forEach(counter);
    assertThat(counter.count).isZero();
    constraints = constraints.put(ObjectConstraint.NOT_NULL);
    counter = new Counter();
    constraints.forEach(counter);
    assertThat(counter.count).isEqualTo(1);
  }

  @Test
  void test_toString() {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty();
    assertThat(constraints).hasToString("[]");

    // uses constraints value
    constraints = constraints.put(ObjectConstraint.NOT_NULL);
    assertThat(constraints).hasToString("[NOT_NULL]");

    // ordered by constraint name
    constraints = constraints.put(BooleanConstraint.FALSE);
    assertThat(constraints).hasToString("[FALSE,NOT_NULL]");

    constraints = constraints.put(BooleanConstraint.TRUE);
    assertThat(constraints).hasToString("[NOT_NULL,TRUE]");
  }

  private static class Counter implements BiConsumer<Class<? extends Constraint>, Constraint> {

    private int count;

    @Override
    public void accept(Class<? extends Constraint> aClass, Constraint constraint) {
      count++;
    }
  }

  @Test
  void test_equals_hashcode() {
    ConstraintsByDomain c1 = ConstraintsByDomain.empty();
    ConstraintsByDomain c2 = c1.put(ObjectConstraint.NOT_NULL);
    assertThat(c1)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .isNotEqualTo(c2)
      .isEqualTo(c1);
    assertThat(c1.hashCode()).isNotEqualTo(c2.hashCode());
  }

  @Test
  void test_has_constraint() {
    ConstraintsByDomain c = ConstraintsByDomain.empty();
    assertThat(c.hasConstraint(ObjectConstraint.NULL)).isFalse();
    c = c.put(ObjectConstraint.NULL);
    assertThat(c.hasConstraint(ObjectConstraint.NULL)).isTrue();
    assertThat(c.hasConstraint(ObjectConstraint.NOT_NULL)).isFalse();
  }
}
