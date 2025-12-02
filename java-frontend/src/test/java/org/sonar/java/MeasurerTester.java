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
package org.sonar.java;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.telemetry.NoOpTelemetry;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.mockito.Mockito.mock;
import static org.sonar.java.TestUtils.mockSonarComponents;

public abstract class MeasurerTester {

  private SensorContextTester context;

  @BeforeEach
  public void setUp() {
    context = SensorContextTester.create(projectDir());

    DefaultFileSystem fs = context.fileSystem();

    FileUtils.listFiles(sourceDir(), new String[] {"java"}, true).stream()
      .map(file -> TestUtils.inputFile("", sourceDir(), file, InputFile.Type.MAIN))
      .forEach(fs::add);

    Measurer measurer = new Measurer(context, mock(NoSonarFilter.class));
    JavaFrontend frontend = new JavaFrontend(new JavaVersionImpl(), mockSonarComponents(), measurer, new NoOpTelemetry(), mock(JavaResourceLocator.class), null);
    List<InputFile> files = StreamSupport.stream(fs.inputFiles().spliterator(), false).toList();
    frontend.scan(files, Collections.emptyList(), Collections.emptyList());
  }

  public abstract File projectDir();

  public abstract File sourceDir();

  public Map<String, Double> getMetrics() {
    Map<String, Double> metrics = new HashMap<>();
    for (InputFile inputFile : context.fileSystem().inputFiles()) {
      for (Measure measure : context.measures(inputFile.key())) {
        if (measure.value() != null) {
          String key = measure.metric().key();
          double value = 0;
          try {
            value = Double.parseDouble("" + measure.value());
          } catch (NumberFormatException nfe) {
            // do nothing
          }
          if (metrics.get(key) == null) {
            metrics.put(key, value);
          } else {
            metrics.put(key, metrics.get(key) + value);
          }
        }
      }
    }
    return metrics;
  }

}
