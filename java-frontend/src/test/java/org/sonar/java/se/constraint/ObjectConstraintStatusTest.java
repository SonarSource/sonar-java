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
package org.sonar.java.se.constraint;

import org.junit.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectConstraintStatusTest {

  @Test
  public void valueAsString() throws Exception {
    Constraint constraint = new Constraint() {
      @Override
      public boolean isNull() {
        return false;
      }
    };
    assertThat(constraint.valueAsString()).isEqualTo("");
  }

  @Test
  public void statusPredicate() throws Exception {
    Constraint notObjectConstraint = () -> false;
    ObjectConstraint<MyStatus> status2Constraint = new ObjectConstraint<>(MyStatus.STATUS2);
    Predicate<Constraint> predicate = ObjectConstraint.statusPredicate(MyStatus.STATUS1);
    assertThat(predicate.test(notObjectConstraint)).isFalse();
    assertThat(predicate.test(status2Constraint)).isFalse();
    ObjectConstraint<MyStatus> status1Constraint = status2Constraint.withStatus(MyStatus.STATUS1);
    assertThat(predicate.test(status1Constraint)).isTrue();
  }

  enum MyStatus implements ObjectConstraint.Status {
    STATUS1, STATUS2
  }
}
