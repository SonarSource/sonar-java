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
package org.sonar.java.model.javac;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class JavacBatchAnalysisTest {

  @Test
  void resolves_types_across_source_files() throws IOException {
    String first = Path.of("A.java").toAbsolutePath().toString();
    String second = Path.of("B.java").toAbsolutePath().toString();
    Map<String, String> sources = new LinkedHashMap<>();
    sources.put(first, "class A {}");
    sources.put(second, "class B { A field; }");
    try (JavacBatchAnalysis analysis = JavacBatchAnalysis.analyze(sources, JavacParser.Options.defaults())) {
      ClassTree firstType = (ClassTree) analysis.tree(first).types().get(0);
      ClassTree secondType = (ClassTree) analysis.tree(second).types().get(0);
      VariableTree field = (VariableTree) secondType.members().get(0);
      assertThat(field.symbol().type().symbol()).isSameAs(firstType.symbol());
    }
  }

  @Test
  void continues_after_a_syntax_error() throws IOException {
    String first = Path.of("Broken.java").toAbsolutePath().toString();
    String second = Path.of("Valid.java").toAbsolutePath().toString();
    Map<String, String> sources = new LinkedHashMap<>();
    sources.put(first, "class Broken {");
    sources.put(second, "class Valid {}");
    try (JavacBatchAnalysis analysis = JavacBatchAnalysis.analyze(sources, JavacParser.Options.defaults())) {
      assertThat(analysis.error(first)).isNotNull();
      assertThat(analysis.tree(first)).isNull();
      assertThat(analysis.error(second)).isNull();
      assertThat(analysis.tree(second).types()).hasSize(1);
    }
  }
}
