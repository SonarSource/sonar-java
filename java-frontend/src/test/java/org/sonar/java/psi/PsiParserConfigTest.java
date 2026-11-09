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
package org.sonar.java.psi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sonar.java.AnalysisProgress;
import org.sonar.java.TestUtils;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ClassTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class PsiParserConfigTest {
  @ParameterizedTest
  @EnumSource(JParserConfig.Mode.class)
  void returned_roots_retain_semantics_until_each_lease_is_released(JParserConfig.Mode mode) {
    var inputs = List.of(TestUtils.inputFile("src/test/files/metrics/Classes.java"), TestUtils.inputFile("src/test/files/metrics/Methods.java"));
    var trees = new ArrayList<JavaTree.CompilationUnitTreeImpl>();
    config(mode).parse(inputs, () -> false, new AnalysisProgress(2), (input, result) -> {
      try {
        trees.add(result.get());
      } catch (Exception failure) {
        throw new AssertionError(failure);
      }
    });
    try {
      assertThat(trees).hasSize(2);
      var first = (ClassTree) trees.getFirst().types().getFirst();
      var second = (ClassTree) trees.getLast().types().getFirst();
      assertThat(first.symbol().name()).isEqualTo(first.simpleName().name());
      trees.getFirst().getEnvironmentCleaner().run();
      trees.getFirst().getEnvironmentCleaner().run();
      assertThat(second.symbol().name()).isEqualTo(second.simpleName().name());
      trees.getLast().getEnvironmentCleaner().run();
      assertThatThrownBy(() -> second.symbol().name()).isInstanceOf(IllegalStateException.class).hasMessage("PSI environment is closed");
    } finally {
      trees.forEach(tree -> tree.getEnvironmentCleaner().run());
    }
  }

  @ParameterizedTest
  @EnumSource(JParserConfig.Mode.class)
  void callback_failure_releases_resources_and_preserves_the_original_exception(JParserConfig.Mode mode) {
    var input = TestUtils.inputFile("src/test/files/metrics/Classes.java");
    var trees = new ArrayList<JavaTree.CompilationUnitTreeImpl>();
    var failure = new IllegalStateException("callback failed");
    assertThatThrownBy(() -> config(mode).parse(List.of(input), () -> false, new AnalysisProgress(1), (file, result) -> {
      try {
        trees.add(result.get());
      } catch (Exception exception) {
        throw new AssertionError(exception);
      }
      throw failure;
    })).isSameAs(failure);
    var type = (ClassTree) trees.getFirst().types().getFirst();
    assertThatThrownBy(() -> type.symbol().name()).hasMessage("PSI environment is closed");
    trees.getFirst().getEnvironmentCleaner().run();
    var recovery = config(mode).parse("Recovery.java", "class Recovery {}");
    try {
      assertThat(((ClassTree) recovery.types().getFirst()).symbol().name()).isEqualTo("Recovery");
    } finally {
      recovery.getEnvironmentCleaner().run();
    }
  }

  @ParameterizedTest
  @EnumSource(JParserConfig.Mode.class)
  void unreadable_inputs_are_delivered_as_failures_once_and_other_files_are_analyzed(JParserConfig.Mode mode) throws IOException {
    var unreadable = spy(TestUtils.inputFile("src/test/files/metrics/Classes.java"));
    when(unreadable.contents()).thenThrow(new IOException("unreadable"));
    var readable = TestUtils.inputFile("src/test/files/metrics/Methods.java");
    var processed = new ArrayList<String>();
    config(mode).parse(List.of(unreadable, readable), () -> false, new AnalysisProgress(2), (input, result) -> {
      processed.add(input.filename());
      if (input == unreadable) {
        assertThatThrownBy(result::get).isInstanceOf(IOException.class).hasMessage("unreadable");
      } else {
        try {
          var tree = result.get();
          assertThat(tree.types()).isNotEmpty();
          tree.getEnvironmentCleaner().run();
        } catch (Exception failure) {
          throw new AssertionError(failure);
        }
      }
    });
    assertThat(processed).containsExactly(unreadable.filename(), readable.filename());
  }

  private static JParserConfig config(JParserConfig.Mode mode) {
    return PsiParserConfig.create(mode, JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false);
  }
}
