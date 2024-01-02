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
package org.sonar.java.checks.helpers;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ShannonEntropyTest {

  private static final Offset<Double> WITHIN_5 = within(0.00001);

  @Test
  void test_calculate() {
    assertThat(ShannonEntropy.calculate(null)).isEqualTo(0.000000, WITHIN_5);
    assertThat(ShannonEntropy.calculate("")).isEqualTo(0.000000, WITHIN_5);
    assertThat(ShannonEntropy.calculate("0000000000000000000000000000000000000000")).isEqualTo(0.000000, WITHIN_5);
    assertThat(ShannonEntropy.calculate("0000000000000000000011111111111111111111")).isEqualTo(1.000000, WITHIN_5);
    assertThat(ShannonEntropy.calculate("0000000000111111111122222222223333333333")).isEqualTo(2.000000, WITHIN_5);
    assertThat(ShannonEntropy.calculate("0000011111222223333344444555556666677777")).isEqualTo(3.000000, WITHIN_5);
    assertThat(ShannonEntropy.calculate("0123456789abcdef0123456789abcdef01234567")).isEqualTo(3.970950, WITHIN_5);
    assertThat(ShannonEntropy.calculate("0123456789ABCDabcdefghijklmnopqrstuvwxyz")).isEqualTo(5.321928, WITHIN_5);
    assertThat(ShannonEntropy.calculate("0040878d3579659158d09ad09b6a9849d18e0e22")).isEqualTo(3.587326, WITHIN_5);
    assertThat(ShannonEntropy.calculate("06c6d5715a1ede6c51fc39ff67fd647f740b656d")).isEqualTo(3.552655, WITHIN_5);
    assertThat(ShannonEntropy.calculate("qAhEMdXy/MPwEuDlhh7O0AFBuzGvNy7AxpL3sX3q")).isEqualTo(4.684183, WITHIN_5);
  }

}
