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
package org.sonar.java.model;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.java.testing.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.JParserConfig.shouldEnablePreviewFlag;

class JParserConfigTest {
  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester().setLevel(Level.INFO);

  @Test
  void should_enable_preview() {
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl())).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(8))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(11))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(16))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(17))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(18))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(19))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(20))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(42))).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(42, true))).isTrue();

    assertThat(shouldEnablePreviewFlag(JavaVersionImpl.fromString("1.8"))).isFalse();
    assertThat(shouldEnablePreviewFlag(JavaVersionImpl.fromStrings("1.8", "True"))).isTrue();
  }

  @Test
  void a_debug_message_is_logged_when_shouldIgnoreUnnamedModuleForSplitPackage_is_set() {
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList());
    assertThat(logTester.logs()).isEmpty();
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList(), false);
    assertThat(logTester.logs()).isEmpty();
    JParserConfig.Mode.BATCH.create(new JavaVersionImpl(17), Collections.emptyList(), true);
    assertThat(logTester.logs()).containsExactly("The Java analyzer will ignore the unnamed module for split packages.");
  }
}
