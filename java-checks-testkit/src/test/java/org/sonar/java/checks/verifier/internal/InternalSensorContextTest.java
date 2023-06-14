/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.verifier.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalSensorContextTest {

  @Test
  void methods() {
    SensorContext context = new InternalSensorContext();

    assertThat(context.config())
      .isNotNull()
      .isInstanceOf(InternalConfiguration.class);
    assertThat(context.fileSystem())
      .isNotNull()
      .isInstanceOf(InternalFileSystem.class);
    assertThat(context.getSonarQubeVersion())
      .isNotNull()
      .isInstanceOf(Version.class);
    assertThat(context.runtime())
      .isNotNull()
      .isInstanceOf(InternalSonarRuntime.class);

    assertThat(context.isCancelled()).isFalse();
    assertThat(context.module()).isNotNull();
    assertThat(context.module().isFile()).isFalse();
    assertThat(context.module().key()).isEqualTo("module");
    assertThat(context.project()).isNotNull();
    assertThat(context.project().isFile()).isFalse();
    assertThat(context.project().key()).isEqualTo("project");
    assertThat(context.isCacheEnabled()).isFalse();
    assertThat(context.previousCache()).isNull();
    assertThat(context.nextCache()).isNull();
  }

  @Test
  void methods_not_supported() {
    SensorContext context = new InternalSensorContext();

    assertMethodNotSupported(() -> context.activeRules(), "InternalSensorContext::activeRules()");
    assertMethodNotSupported(() -> context.addContextProperty(null, null), "InternalSensorContext::addContextProperty(String,String)");
    assertMethodNotSupported(() -> context.markForPublishing(null), "InternalSensorContext::markForPublishing(InputFile)");
    assertMethodNotSupported(() -> context.newAdHocRule(), "InternalSensorContext::newAdHocRule()");
    assertMethodNotSupported(() -> context.newAnalysisError(), "InternalSensorContext::newAnalysisError()");
    assertMethodNotSupported(() -> context.newCoverage(), "InternalSensorContext::newCoverage()");
    assertMethodNotSupported(() -> context.newCpdTokens(), "InternalSensorContext::newCpdTokens()");
    assertMethodNotSupported(() -> context.newExternalIssue(), "InternalSensorContext::newExternalIssue()");
    assertMethodNotSupported(() -> context.newHighlighting(), "InternalSensorContext::newHighlighting()");
    assertMethodNotSupported(() -> context.newIssue(), "InternalSensorContext::newIssue()");
    assertMethodNotSupported(() -> context.newMeasure(), "InternalSensorContext::newMeasure()");
    assertMethodNotSupported(() -> context.newSignificantCode(), "InternalSensorContext::newSignificantCode()");
    assertMethodNotSupported(() -> context.newSymbolTable(), "InternalSensorContext::newSymbolTable()");
    assertMethodNotSupported(() -> context.settings(), "InternalSensorContext::settings()");
    assertMethodNotSupported(() -> context.markAsUnchanged(null), "InternalSensorContext::markAsUnchanged(InputFile)");
  }

  @Test
  void canSkipUnchangedFiles_returns_false_by_default() {
    SensorContext context = new InternalSensorContext();
    assertThat(context.canSkipUnchangedFiles()).isFalse();
  }

  private static void assertMethodNotSupported(Executable executable, String expectedMessage) {
    InternalMockedSonarAPI.NotSupportedException e = assertThrows(InternalMockedSonarAPI.NotSupportedException.class, executable);
    assertThat(e).hasMessage(String.format("Method unsuported by the rule verifier framework: '%s'", expectedMessage));
  }
}
