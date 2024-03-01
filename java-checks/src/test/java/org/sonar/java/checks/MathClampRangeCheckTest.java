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
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.MathClampRangeCheck.isLessTan;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class MathClampRangeCheckTest {

  @Test
  void test_java_21() {
    InternalCheckVerifier
      .newInstance()
      .onFile(mainCodeSourcesPath("checks/MathClampRangeCheckSample.java"))
      .withCheck(new MathClampRangeCheck())
      .withQuickFixes()
      .withJavaVersion(21)
      .verifyIssues();
  }

  @Test
  void test_before_java_21() {
    InternalCheckVerifier
      .newInstance()
      .onFile(mainCodeSourcesPath("checks/MathClampRangeCheckSample.java"))
      .withCheck(new MathClampRangeCheck())
      .withQuickFixes()
      .withJavaVersion(20)
      .verifyNoIssues();
  }

  @Test
  void test_less_than() {
    // conversion to integer
    assertThat(isLessTan(Integer.MIN_VALUE,Integer.MAX_VALUE)).isTrue();
    assertThat(isLessTan(Integer.MAX_VALUE,Integer.MIN_VALUE)).isFalse();
    assertThat(isLessTan(20,(byte)'A')).isTrue();

    // conversion to long
    assertThat(isLessTan(Long.MIN_VALUE,Long.MAX_VALUE)).isTrue();
    assertThat(isLessTan(Long.MAX_VALUE,Long.MIN_VALUE)).isFalse();
    assertThat(isLessTan(Integer.MIN_VALUE,Long.MIN_VALUE)).isFalse();
    assertThat(isLessTan(Long.MAX_VALUE,Integer.MAX_VALUE)).isFalse();

    // conversion to float
    assertThat(isLessTan(1.0f, 2.0f)).isTrue();
    assertThat(isLessTan(1.0f, -2.0f)).isFalse();
    assertThat(isLessTan(9_223_372_036_854_775_806L, 9_223_372_036_854_775_805f)).isFalse();
    assertThat(isLessTan(9_223_372_036_854_775_805f, 9_223_372_036_854_775_806L)).isFalse(); // a == b because of float low precision

    assertThat(isLessTan(Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY)).isTrue();
    assertThat(isLessTan(Float.NaN,Float.POSITIVE_INFINITY)).isFalse();
    assertThat(isLessTan(Float.NaN,Float.NaN)).isFalse();

    // conversion to double
    assertThat(isLessTan(1.0d, 2.0d)).isTrue();
    assertThat(isLessTan(1, 2.0d)).isTrue();
    assertThat(isLessTan(1.0d, -2.0d)).isFalse();
    assertThat(isLessTan(9_223_372_036_854_775_807L, 9_223_372_036_854_775_806d)).isFalse();
    assertThat(isLessTan(9_223_372_036_854_775_806d, 9_223_372_036_854_775_807L)).isFalse(); // a == b because of double low precision
  }
}
