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
package org.sonar.java.checks.helpers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialMethodsLoaderTest {
  @Test
  void testSuccessfulLoad() throws IOException {
    Map<String, List<CredentialMethod>> loadedMethods = CredentialMethodsLoader.load("/test-methods.json");

    assertThat(loadedMethods).hasSize(2);
  }

  @Test
  void testFailedToLoad() {
    assertThatThrownBy(() -> CredentialMethodsLoader.load("/non-existing-test-methods.json"))
      .isInstanceOf(IOException.class);
  }

}
