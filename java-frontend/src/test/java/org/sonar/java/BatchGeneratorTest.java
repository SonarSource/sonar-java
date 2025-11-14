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
package org.sonar.java;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.InputFileUtils.addFile;

@EnableRuleMigrationSupport
class BatchGeneratorTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  void batch_generator_returns_an_empty_list_when_no_input_files() {
    List<InputFile> emptyList = Collections.emptyList();
    BatchGenerator generator = new BatchGenerator(emptyList.iterator(), 0);
    assertThat(generator.hasNext()).isFalse();
    assertThat(generator.next()).isEmpty();
  }

  @Test
  void batch_generator_returns_at_most_one_item_per_batch_when_size_is_zero() throws IOException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    SensorContextTester sensorContext = SensorContextTester.create(baseDir);
    sensorContext.setSettings(new MapSettings());
    List<InputFile> inputFiles = new ArrayList<>();
    inputFiles.add(addFile(temp, "class A {}", sensorContext));
    inputFiles.add(addFile(temp, "class B extends A {}", sensorContext));
    BatchGenerator generator = new BatchGenerator(inputFiles.iterator(), 0);
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next())
      .hasSize(1)
      .contains(inputFiles.get(0));
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next())
      .hasSize(1)
      .contains(inputFiles.get(1));
    assertThat(generator.hasNext()).isFalse();
    assertThat(generator.next()).isEmpty();
  }

  @Test
  void batch_generator_returns_batches_with_multiple_files_that_are_smaller_than_batch_size() throws IOException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    SensorContextTester sensorContext = SensorContextTester.create(baseDir);
    sensorContext.setSettings(new MapSettings());
    InputFile fileA = addFile(temp, "class A { public void doSomething() {} }", sensorContext);
    InputFile fileB = addFile(temp, "class B extends A {}", sensorContext);
    InputFile fileC = addFile(temp, "class C {}", sensorContext);

    long sizeofA = fileA.file().length() + 1;
    BatchGenerator generator = new BatchGenerator(
      Arrays.asList(fileA, fileB, fileC).iterator(), sizeofA
    );
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next()).hasSize(1).contains(fileA);
    assertThat(generator.hasNext()).isTrue();
    List<InputFile> batchWithMultipleFiles = generator.next();
    assertThat(batchWithMultipleFiles).hasSize(2).contains(fileB).contains(fileC);
    long batchSize = batchWithMultipleFiles.stream().map(i -> i.file().length()).reduce(0L, Long::sum);
    assertThat(batchSize).isLessThanOrEqualTo(sizeofA);
    assertThat(generator.hasNext()).isFalse();
    assertThat(generator.next()).isEmpty();

    long sizeOfAPlusB = fileA.file().length() + fileB.file().length();
    generator = new BatchGenerator(
      Arrays.asList(fileA, fileB, fileC).iterator(), sizeOfAPlusB
    );
    assertThat(generator.hasNext()).isTrue();
    batchWithMultipleFiles = generator.next();
    assertThat(batchWithMultipleFiles).hasSize(2).contains(fileA).contains(fileB);
    batchSize = batchWithMultipleFiles.stream().map(i -> i.file().length()).reduce(0L, Long::sum);
    assertThat(batchSize).isLessThanOrEqualTo(sizeOfAPlusB);
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next()).hasSize(1).contains(fileC);
    assertThat(generator.hasNext()).isFalse();
    assertThat(generator.next()).isEmpty();
  }

  @Test
  void batch_generator_includes_file_excluded_from_previous_batch_into_next_batch() throws IOException {
    File baseDir = temp.getRoot().getAbsoluteFile();
    SensorContextTester sensorContext = SensorContextTester.create(baseDir);
    sensorContext.setSettings(new MapSettings());
    InputFile fileA = addFile(temp, "class A { public void doSomething() {} }", sensorContext);
    InputFile fileB = addFile(temp, "class B extends A {}", sensorContext);
    InputFile fileC = addFile(temp, "class C {}", sensorContext);
    BatchGenerator generator = new BatchGenerator(
      Arrays.asList(fileA, fileC, fileB).iterator(), fileC.file().length()
    );
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next()).hasSize(1).contains(fileA);
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next()).hasSize(1).contains(fileC);
    assertThat(generator.hasNext()).isTrue();
    assertThat(generator.next()).hasSize(1).contains(fileB);
    assertThat(generator.hasNext()).isFalse();
    assertThat(generator.next()).isEmpty();
  }
}
