/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.filters;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.filters.LombokFilter.localName;

class LombokFilterTest {

  @Test
  void test() {
    FilterVerifier.newInstance()
      .verify("src/test/files/filters/LombokFilter.java", new LombokFilter());
  }

  @Test
  void testWithoutSemantic() {
    FilterVerifier.newInstance()
      .withoutSemantic()
      .verify("src/test/files/filters/LombokFilterWithoutSemantic.java", new LombokFilter());
  }

  @Test
  void testLocalName() {
    assertThat(localName("noDot")).isEqualTo("noDot");
    assertThat(localName("one.dot")).isEqualTo("dot");
    assertThat(localName("many.many.dots")).isEqualTo("dots");
  }
}
