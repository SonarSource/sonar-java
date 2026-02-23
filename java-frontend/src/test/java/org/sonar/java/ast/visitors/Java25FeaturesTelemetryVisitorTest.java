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
package org.sonar.java.ast.visitors;

import java.io.File;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.JavaFrontend;
import org.sonar.java.Measurer;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.telemetry.Telemetry;
import org.sonar.java.telemetry.TelemetryKey;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.java.TestUtils.mockSonarComponents;

class Java25FeaturesTelemetryVisitorTest {

  private File baseDir;

  @BeforeEach
  void setUp() {
    baseDir = new File("src/test/files/metrics");
  }

  @Test
  void verify_features_detected() {
    InputFile inputFile = TestUtils.inputFile(new File(baseDir, "Java25Features.java"));
    Telemetry telemetry = mock(Telemetry.class);


    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(25), mockSonarComponents(), mock(Measurer.class), telemetry, null, null);
    frontend.scan(Collections.singletonList(inputFile), Collections.emptyList(), Collections.emptyList());

    verify(telemetry).aggregateAsCounter(TelemetryKey.JAVA_FEATURE_MODULE_IMPORT, 1);
    verify(telemetry).aggregateAsCounter(TelemetryKey.JAVA_FEATURE_COMPACT_SOURCE_FILES, 1);
    verify(telemetry, times(2)).aggregateAsCounter(TelemetryKey.JAVA_FEATURE_FLEXIBLE_CONSTRUCTOR_BODY, 1);
  }

  @Test
  void verify_no_features_detected_java24() {
    InputFile inputFile = TestUtils.inputFile(new File(baseDir, "Java25Features.java"));
    Telemetry telemetry = mock(Telemetry.class);


    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(24), mockSonarComponents(), mock(Measurer.class), telemetry, null, null);
    frontend.scan(Collections.singletonList(inputFile), Collections.emptyList(), Collections.emptyList());

    verify(telemetry, times(0)).aggregateAsCounter(TelemetryKey.JAVA_FEATURE_MODULE_IMPORT, 1);
    verify(telemetry, times(0)).aggregateAsCounter(TelemetryKey.JAVA_FEATURE_COMPACT_SOURCE_FILES, 1);
    verify(telemetry, times(0)).aggregateAsCounter(TelemetryKey.JAVA_FEATURE_FLEXIBLE_CONSTRUCTOR_BODY, 1);
  }

}
