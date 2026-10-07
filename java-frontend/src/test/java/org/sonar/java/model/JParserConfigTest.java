/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.JParserConfig.shouldEnablePreviewFlag;

class JParserConfigTest {
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
    assertThat(shouldEnablePreviewFlag(JavaVersionImpl.fromString("1.8", "True"))).isTrue();
  }

}
