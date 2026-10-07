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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalysisProgress;
import org.sonar.plugins.java.api.tree.ClassTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.model.JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;

class JParserTest {

  @Test
  void parses_sources_in_both_modes() throws Exception {
    for (JParserConfig.Mode mode : JParserConfig.Mode.values()) {
      List<InputFile> files = List.of(source("First.java", "class First {}"), source("Second.java", "class Second {}"));
      List<String> parsed = new ArrayList<>();
      mode.create(MAXIMUM_SUPPORTED_JAVA_VERSION, List.of())
        .parse(files, () -> false, new AnalysisProgress(files.size()), (file, result) -> {
          try {
            var tree = result.get();
            parsed.add(((ClassTree) tree.types().get(0)).simpleName().name());
            assertThat(tree.javacSema).isNotNull();
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        });
      assertThat(parsed).containsExactly("First", "Second");
    }
  }

  @Test
  void stops_before_parsing_when_cancelled() {
    for (JParserConfig.Mode mode : JParserConfig.Mode.values()) {
      AtomicInteger callbacks = new AtomicInteger();
      mode.create(MAXIMUM_SUPPORTED_JAVA_VERSION, List.of())
        .parse(List.of(source("First.java", "class First {}")), () -> true, new AnalysisProgress(1),
          (file, result) -> callbacks.incrementAndGet());
      assertThat(callbacks).hasValue(0);
    }
  }

  @Test
  void passes_source_read_errors_to_consumer() throws Exception {
    InputFile file = source("Broken.java", "class Broken {}");
    IOException failure = new IOException("Unable to read source");
    when(file.contents()).thenThrow(failure);
    for (JParserConfig.Mode mode : JParserConfig.Mode.values()) {
      List<JParserConfig.Result> results = new ArrayList<>();
      mode.create(MAXIMUM_SUPPORTED_JAVA_VERSION, List.of())
        .parse(List.of(file), () -> false, new AnalysisProgress(1), (ignored, result) -> results.add(result));
      assertThat(results).hasSize(1);
      assertThatThrownBy(results.get(0)::get).isSameAs(failure);
    }
  }

  @Test
  void propagates_consumer_failures() {
    RuntimeException failure = new RuntimeException("consumer failed");
    for (JParserConfig.Mode mode : JParserConfig.Mode.values()) {
      var config = mode.create(MAXIMUM_SUPPORTED_JAVA_VERSION, List.of());
      assertThatThrownBy(() -> config.parse(List.of(source("First.java", "class First {}")), () -> false,
        new AnalysisProgress(1), (file, result) -> { throw failure; })).isSameAs(failure);
    }
  }

  private static InputFile source(String name, String contents) {
    InputFile file = mock(InputFile.class);
    when(file.filename()).thenReturn(name);
    when(file.absolutePath()).thenReturn("/tmp/" + name);
    try {
      when(file.contents()).thenReturn(contents);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return file;
  }
}
