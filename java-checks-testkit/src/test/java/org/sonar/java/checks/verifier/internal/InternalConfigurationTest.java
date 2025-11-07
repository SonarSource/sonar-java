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
package org.sonar.java.checks.verifier.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.sonar.api.config.Configuration;
import org.sonar.java.SonarComponents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalConfigurationTest {

  @Test
  void methods() {
    Configuration config = new InternalConfiguration();

    assertThat(config.hasKey(SonarComponents.FAIL_ON_EXCEPTION_KEY)).isTrue();
    assertThat(config.hasKey("any")).isFalse();

    assertThat(config.get(SonarComponents.FAIL_ON_EXCEPTION_KEY)).hasValue("true");
    assertThat(config.get("any")).isEmpty();

    assertMethodNotSupported(() -> config.getStringArray("any"), "InternalConfiguration::getStringArray(String)");
  }

  private static void assertMethodNotSupported(Executable executable, String expectedMessage) {
    InternalMockedSonarAPI.NotSupportedException e = assertThrows(InternalMockedSonarAPI.NotSupportedException.class, executable);
    assertThat(e).hasMessage(String.format("Method unsuported by the rule verifier framework: '%s'", expectedMessage));
  }
}
