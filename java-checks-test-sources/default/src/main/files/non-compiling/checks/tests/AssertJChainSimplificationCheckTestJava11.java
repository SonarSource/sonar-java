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
 * long with this program; if not, see https://sonarsource.com/license/ssal/
 */
package checks.tests;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AssertJChainSimplificationCheckTestJava11 {

  private Optional<Object> getOptional() {
    return null;
  }

  void optionalRelatedAssertionChains() {
    assertThat(getOptional().isEmpty()).isTrue(); // Noncompliant {{Use assertThat(actual).isNotPresent() or assertThat(actual).isEmpty() instead.}}
    assertThat(getOptional().isEmpty()).isFalse(); // Noncompliant {{Use assertThat(actual).isPresent() instead.}}
  }
}
