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
package org.sonar.java.se.constraint;

import org.junit.Test;

import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

public class ConstraintsByDomainTest {

  @Test
  public void test_isEmpty() throws Exception {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty();
    assertThat(constraints.isEmpty()).isTrue();
    constraints = constraints.put(ObjectConstraint.NULL);
    assertThat(constraints.isEmpty()).isFalse();
  }

  @Test
  public void test_remove() throws Exception {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty().put(ObjectConstraint.NULL);
    assertThat(constraints.get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NULL);
    constraints = constraints.remove(ObjectConstraint.class);
    assertThat(constraints).isSameAs(ConstraintsByDomain.empty());

    ConstraintsByDomain c1 = ConstraintsByDomain.empty().put(ObjectConstraint.NOT_NULL);
    ConstraintsByDomain c2 = c1.remove(BooleanConstraint.class);
    assertThat(c1).isSameAs(c2);
  }

  @Test
  public void test_domains() throws Exception {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty();
    assertThat(constraints.domains()).isEmpty();
    constraints = constraints.put(ObjectConstraint.NOT_NULL).put(BooleanConstraint.TRUE);
    assertThat(constraints.domains()).contains(ObjectConstraint.class, BooleanConstraint.class);
  }

  @Test
  public void test_constraints_stream() throws Exception {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty();
    assertThat(constraints.stream()).isEmpty();
    constraints = constraints.put(ObjectConstraint.NOT_NULL).put(BooleanConstraint.TRUE);
    assertThat(constraints.stream()).contains(ObjectConstraint.NOT_NULL, BooleanConstraint.TRUE);
  }

  @Test
  public void test_put() throws Exception {
    ConstraintsByDomain c1 = ConstraintsByDomain.empty().put(ObjectConstraint.NOT_NULL);
    ConstraintsByDomain c2 = c1.put(ObjectConstraint.NOT_NULL);
    assertThat(c1).isSameAs(c2);
  }

  @Test
  public void test_forEach() {
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
  public void test_toString() {
    ConstraintsByDomain constraints = ConstraintsByDomain.empty();
    assertThat(constraints.toString()).isEqualTo("[]");

    // uses constraints value
    constraints = constraints.put(ObjectConstraint.NOT_NULL);
    assertThat(constraints.toString()).isEqualTo("[NOT_NULL]");

    // ordered by constraint name
    constraints = constraints.put(BooleanConstraint.FALSE);
    assertThat(constraints.toString()).isEqualTo("[FALSE,NOT_NULL]");

    constraints = constraints.put(BooleanConstraint.TRUE);
    assertThat(constraints.toString()).isEqualTo("[NOT_NULL,TRUE]");
  }

  private static class Counter implements BiConsumer<Class<? extends Constraint>, Constraint> {

    private int count;

    @Override
    public void accept(Class<? extends Constraint> aClass, Constraint constraint) {
      count++;
    }
  }

  @Test
  public void test_equals_hashcode() throws Exception {
    ConstraintsByDomain c1 = ConstraintsByDomain.empty();
    assertThat(c1.equals(null)).isFalse();
    ConstraintsByDomain c2 = c1.put(ObjectConstraint.NOT_NULL);
    assertThat(c1.equals(c2)).isFalse();
    assertThat(c1.hashCode()).isNotEqualTo(c2.hashCode());
  }

  @Test
  public void test_has_constraint() {
    ConstraintsByDomain c = ConstraintsByDomain.empty();
    assertThat(c.hasConstraint(ObjectConstraint.NULL)).isFalse();
    c = c.put(ObjectConstraint.NULL);
    assertThat(c.hasConstraint(ObjectConstraint.NULL)).isTrue();
    assertThat(c.hasConstraint(ObjectConstraint.NOT_NULL)).isFalse();
  }
}
