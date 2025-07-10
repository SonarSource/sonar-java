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
package org.sonar.java.telemetry;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlphaNumericComparatorTest {

  private static final AlphaNumericComparator COMP = new AlphaNumericComparator();

  @Test
  void test_alpha_numeric_comparator() {
    assertThat(COMP.compare(null, null)).isZero();
    assertThat(COMP.compare(null, "")).isLessThan(0);

    assertComp("").isGreaterThan(null).isEqualTo("").isLessThan("a");
    assertComp("a").isGreaterThan("").isEqualTo("a").isLessThan("b");
    assertComp("22").isGreaterThan("3").isEqualTo("22").isLessThan("100");
    assertComp("2").isLessThan("a").isEqualTo("2").isLessThan("10");
    assertComp("22abc22abc").isGreaterThan("3abc22abc").isEqualTo("22abc22abc").isLessThan("abc22abc");
    assertComp("aaa").isGreaterThan("A").isEqualTo("aaa").isLessThan("bb");
    assertComp("b").isGreaterThan("a").isEqualTo("b").isLessThan("c");
    assertComp("cc").isGreaterThan("bbb").isEqualTo("cc").isLessThan("d22");
    assertComp("abc123def").isGreaterThan("abc22def").isEqualTo("abc123def").isLessThan("abc99999999def");
    assertComp("abc22abc").isGreaterThan("22abc22abc");
    assertComp("abc22def").isLessThan("abc123def");
    assertComp("abc22v1").isLessThan("abc22v1.12");
    assertComp("abc22v1.6").isLessThan("abc22v1.12");
    assertComp("abc22v2").isGreaterThan("abc22v1.12");
    assertComp("abc22v1.12")
      .isGreaterThan("abc22v1")
      .isGreaterThan("abc22v1.6")
      .isEqualTo("abc22v1.12")
      .isLessThan("abc22v1.20")
      .isLessThan("abc22v1.12beta")
      .isLessThan("abc22v2");
  }

  private AbstractStringAssert<?> assertComp(String str) {
    return assertThat(str).usingComparator(COMP);
  }

}
