/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThrowableUtilsTest {

  @Test
  void test_get_no_root_cause() {
    Exception ex = new Exception();
    assertThat(ThrowableUtils.getRootCause(ex)).isEqualTo(ex);
  }

  @Test
  void test_get_simple_root_cause() {
    Exception ex1 = new Exception();
    Exception ex2 = new Exception(ex1);
    assertThat(ThrowableUtils.getRootCause(ex2)).isEqualTo(ex1);
  }

  @Test
  void test_get_long_root_cause() {
    Exception ex1 = new Exception();
    Exception ex2 = new Exception(ex1);
    Exception ex3 = new Exception(ex2);
    Exception ex4 = new Exception(ex3);
    Exception ex5 = new Exception(ex4);
    assertThat(ThrowableUtils.getRootCause(ex5)).isEqualTo(ex1);
  }

  @Test
  void test_get_root_cause_with_cycle() {
    Exception ex1 = new Exception();
    Exception ex2 = new Exception(ex1);
    Exception ex3 = new Exception(ex2);
    Exception ex4 = new Exception(ex3);
    Exception ex5 = new Exception(ex4);
    ex1.initCause(ex3);

    IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> ThrowableUtils.getRootCause(ex5));
    assertThat(exception).hasMessage("Loop in causal chain detected.");
  }
}
