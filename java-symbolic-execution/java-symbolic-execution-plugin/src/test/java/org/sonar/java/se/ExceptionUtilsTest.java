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
package org.sonar.java.se;

import org.junit.jupiter.api.Test;
import org.sonar.java.se.utils.SETestUtils;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Type;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ExceptionUtilsTest {

  @Test
  void test_is_unchecked_exception() {
    assertThat(ExceptionUtils.isUncheckedException(null)).isFalse();
    assertThat(ExceptionUtils.isUncheckedException(Type.UNKNOWN)).isFalse();
    Sema semanticModel = SETestUtils.getSemanticModel("src/test/java/org/sonar/java/se/ExceptionUtilsTest.java");
    Type ex = semanticModel.getClassType("java.lang.IllegalArgumentException");
    assertThat(ExceptionUtils.isUncheckedException(ex)).isTrue();
    ex = semanticModel.getClassType("java.lang.Exception");
    assertThat(ExceptionUtils.isUncheckedException(ex)).isTrue();
    ex = semanticModel.getClassType("java.lang.Throwable");
    assertThat(ExceptionUtils.isUncheckedException(ex)).isTrue();
  }
}
