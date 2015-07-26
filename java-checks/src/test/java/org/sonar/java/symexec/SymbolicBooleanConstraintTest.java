/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.symexec;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.FALSE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.TRUE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.UNKNOWN;

public class SymbolicBooleanConstraintTest {

  @Test
  public void test_count() {
    assertThat(SymbolicBooleanConstraint.values().length).isEqualTo(3);
  }

  @Test
  public void test_negate() {
    assertThat(FALSE.negate()).isEqualTo(TRUE);
    assertThat(TRUE.negate()).isEqualTo(FALSE);
    assertThat(UNKNOWN.negate()).isEqualTo(UNKNOWN);
  }

  @Test
  public void test_union() {
    assertThat(FALSE.union(null)).isEqualTo(FALSE);
    assertThat(FALSE.union(FALSE)).isEqualTo(FALSE);
    assertThat(FALSE.union(TRUE)).isEqualTo(UNKNOWN);
    assertThat(FALSE.union(UNKNOWN)).isEqualTo(UNKNOWN);

    assertThat(TRUE.union(null)).isEqualTo(TRUE);
    assertThat(TRUE.union(FALSE)).isEqualTo(UNKNOWN);
    assertThat(TRUE.union(TRUE)).isEqualTo(TRUE);
    assertThat(TRUE.union(UNKNOWN)).isEqualTo(UNKNOWN);

    assertThat(UNKNOWN.union(null)).isEqualTo(UNKNOWN);
    assertThat(UNKNOWN.union(FALSE)).isEqualTo(UNKNOWN);
    assertThat(UNKNOWN.union(TRUE)).isEqualTo(UNKNOWN);
    assertThat(UNKNOWN.union(UNKNOWN)).isEqualTo(UNKNOWN);
  }

}
