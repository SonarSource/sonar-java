/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.JParserConfig.effectiveJavaVersion;
import static org.sonar.java.model.JParserConfig.shouldEnablePreviewFlag;

class JParserConfigTest {

  @Test
  void effective_java_version() {
    assertThat(effectiveJavaVersion(null)).isEqualTo("17");
    assertThat(effectiveJavaVersion(new JavaVersionImpl())).isEqualTo("17");
    assertThat(effectiveJavaVersion(new JavaVersionImpl(10))).isEqualTo("10");
  }

  @Test
  void should_enable_preview() {
    assertThat(shouldEnablePreviewFlag("8")).isFalse();
    assertThat(shouldEnablePreviewFlag("11")).isFalse();
    assertThat(shouldEnablePreviewFlag("16")).isFalse();
    assertThat(shouldEnablePreviewFlag("17")).isTrue();
    assertThat(shouldEnablePreviewFlag("18")).isTrue();

    assertThat(shouldEnablePreviewFlag(JavaVersionImpl.fromString("1.8").toString())).isFalse();
    assertThat(shouldEnablePreviewFlag(new JavaVersionImpl(18).toString())).isTrue();
  }

}
