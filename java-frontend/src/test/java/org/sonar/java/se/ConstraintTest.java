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
package org.sonar.java.se;

import org.junit.Test;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.constraint.TypedConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ConstraintTest {

  private enum TestStatus implements ObjectConstraint.Status {
    OPENED, CLOSED
  }

  @Test
  public void open_status() {
    final IdentifierTree tree = new IdentifierTreeImpl(new InternalSyntaxToken(1, 1, "id", Collections.<SyntaxTrivia>emptyList(), 0, 0, false));
    ObjectConstraint<TestStatus> constraint = new ObjectConstraint<>(TestStatus.OPENED);
    assertThat(constraint.isNull()).as("Opened constraint is not null").isFalse();
    assertThat(constraint.isDisposable()).isTrue();
    assertThat(constraint.inverse().isNull()).as("Inverse of opened constraint is NULL").isTrue();
    assertThat(constraint.hasStatus(TestStatus.OPENED)).as("Opened constraint is 'opened'").isTrue();
    assertThat(constraint.hasStatus(TestStatus.CLOSED)).as("Opened constraint is not 'opened'").isFalse();
    assertThat(constraint.toString()).as("Constraint's string").isEqualTo("NOT_NULL(OPENED)");

    constraint = constraint.withStatus(TestStatus.CLOSED);
    assertThat(constraint.hasStatus(TestStatus.OPENED)).as("Opened constraint is 'opened'").isFalse();
    assertThat(constraint.hasStatus(TestStatus.CLOSED)).as("Opened constraint is not 'opened'").isTrue();

  }

  @Test
  public void null_constraint() throws Exception {
    assertThat(ObjectConstraint.nullConstraint().hasStatus(TestStatus.OPENED)).isFalse();
    assertThat(ObjectConstraint.nullConstraint().hasStatus(null)).isTrue();
  }

  @Test
  public void boolean_constraint_should_not_be_null() {
    for (BooleanConstraint booleanConstraint : BooleanConstraint.values()) {
      assertThat(booleanConstraint.isNull()).isFalse();
    }
  }

  @Test
  public void typed_constraint_should_not_be_null() {
    assertThat(new TypedConstraint().isNull()).isFalse();
  }

  @Test
  public void testConstraintWithStatus() {
    ProgramState state = ProgramState.EMPTY_STATE;
    final SymbolicValue sv3 = new SymbolicValue(3);
    SymbolicValue sv4 = new SymbolicValue(4) {
      public SymbolicValue wrappedValue() {
        return sv3;
      }
    };
    ObjectConstraint<TestStatus> constraint = new ObjectConstraint<>(TestStatus.OPENED);
    assertThat(state.getConstraintWithStatus(sv4, TestStatus.OPENED)).isNull();
    state = state.addConstraint(sv3, constraint);
    assertThat(state.getConstraintWithStatus(sv4, TestStatus.OPENED)).isEqualTo(constraint);
    assertThat(state.getConstraintWithStatus(sv4, TestStatus.CLOSED)).isNull();
  }

}
