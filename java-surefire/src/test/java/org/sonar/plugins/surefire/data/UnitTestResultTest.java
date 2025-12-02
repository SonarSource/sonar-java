/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.plugins.surefire.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnitTestResultTest {

  @Test
  void shouldBeError() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR);
    assertThat(result.getStatus()).isEqualTo(UnitTestResult.STATUS_ERROR);
    assertThat(result.isError()).isTrue();
    assertThat(result.isErrorOrFailure()).isTrue();
  }

  @Test
  void shouldBeFailure() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_FAILURE);
    assertThat(result.getStatus()).isEqualTo(UnitTestResult.STATUS_FAILURE);
    assertThat(result.isError()).isFalse();
    assertThat(result.isErrorOrFailure()).isTrue();
  }

  @Test
  void shouldBeSuccess() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_OK);
    assertThat(result.getStatus()).isEqualTo(UnitTestResult.STATUS_OK);
    assertThat(result.isError()).isFalse();
    assertThat(result.isErrorOrFailure()).isFalse();
  }
}
