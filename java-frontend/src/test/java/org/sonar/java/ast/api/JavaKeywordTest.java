/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.ast.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaKeywordTest {

  @Test
  void test() {
    assertThat(JavaKeyword.values()).hasSize(51);
    assertThat(JavaKeyword.keywordValues()).hasSize(JavaKeyword.values().length);

    for (JavaKeyword keyword : JavaKeyword.values()) {
      assertThat(keyword.getName()).isEqualTo(keyword.name());
      assertThat(keyword.getValue()).isNotNull();
    }
  }

}
