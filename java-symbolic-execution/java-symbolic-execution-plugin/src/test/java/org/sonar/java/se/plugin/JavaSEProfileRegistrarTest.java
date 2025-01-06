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
package org.sonar.java.se.plugin;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.TestProfileRegistrarContext;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSEProfileRegistrarTest {

  @Test
  void constructor() {
    JavaSEProfileRegistrar registrar = new JavaSEProfileRegistrar();
    TestProfileRegistrarContext context = new TestProfileRegistrarContext();
    registrar.register(context);
    assertThat(context.defaultQualityProfileRules).hasSize(21); // 2 are not in the default profile
  }

}
