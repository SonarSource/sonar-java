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

import org.sonar.java.javac.source.tree.ClassTree;
import org.sonar.java.javac.source.tree.VariableTree;
import org.sonar.java.javac.source.util.TreePath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavacParserTest {

  @Test
  void parses_with_the_running_jdk_and_exposes_positions() throws IOException {
    String source = "class Sample { void run() {} }";
    try (JavacParser.Result result = JavacParser.parse("Sample.java", source)) {
      ClassTree declaration = (ClassTree) result.unit().getTypeDecls().get(0);
      assertThat(declaration.getSimpleName().toString()).isEqualTo("Sample");
      assertThat(result.sourcePositions().getStartPosition(result.unit(), declaration)).isZero();
      assertThat(result.sourcePositions().getEndPosition(result.unit(), declaration)).isEqualTo(source.length());
      assertThat(result.diagnostics()).noneMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR);
    }
  }

  @Test
  void preserves_syntax_diagnostics() throws IOException {
    try (JavacParser.Result result = JavacParser.parse("Broken.java", "class Broken { void m( }")) {
      assertThat(result.diagnostics()).anyMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR);
    }
  }

  @Test
  void parses_multiple_sources_and_resolves_symbols(@TempDir Path directory) throws IOException {
    Path first = directory.resolve("First.java");
    Path second = directory.resolve("Second.java");
    Files.writeString(first, "class First { Second value; }");
    Files.writeString(second, "class Second {}");

    try (JavacParser.Result result = JavacParser.parse(List.of(first, second),
      JavacParser.Options.defaults().withSourceVersion(26).withPreview(true).withAnalysis(true))) {
      assertThat(result.units()).hasSize(2);
      ClassTree firstClass = (ClassTree) result.units().get(0).getTypeDecls().get(0);
      VariableTree field = firstClass.getMembers().stream().filter(VariableTree.class::isInstance)
        .map(VariableTree.class::cast).findFirst().orElseThrow();
      assertThat(result.trees().getElement(TreePath.getPath(result.units().get(0), field.getType())).asType().toString())
        .isEqualTo("Second");
      assertThat(result.diagnostics()).noneMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR);
    }
  }

  @Test
  void honors_source_version() throws IOException {
    try (JavacParser.Result result = JavacParser.parse("Sample.java", "record Sample(int value) {}",
      JavacParser.Options.defaults().withSourceVersion(11))) {
      assertThat(result.diagnostics()).anyMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR);
    }
  }

  @Test
  void parses_java_27_preview_source_on_java_26() throws IOException {
    String source = "class Sample { boolean test(Object value) { return value instanceof int i && i > 0; } }";
    try (JavacParser.Result result = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withSourceVersion(27).withPreview(true).withAnalysis(true))) {
      assertThat(result.units()).hasSize(1);
      assertThat(result.diagnostics()).noneMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR);
    }
  }

  @Test
  void resolves_external_types_from_configured_classpath(@TempDir Path directory) throws IOException {
    Path external = directory.resolve("External.java");
    Files.writeString(external, "package dependency; public class External {}");
    assertThat(ToolProvider.getSystemJavaCompiler().run(null, null, null,
      "-d", directory.toString(), external.toString())).isZero();

    try (JavacParser.Result result = JavacParser.parse("Sample.java", "class Sample { dependency.External value; }",
      JavacParser.Options.defaults().withClasspath(List.of(directory)).withAnalysis(true))) {
      assertThat(result.diagnostics()).noneMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR);
      ClassTree declaration = (ClassTree) result.unit().getTypeDecls().get(0);
      VariableTree field = declaration.getMembers().stream().filter(VariableTree.class::isInstance)
        .map(VariableTree.class::cast).findFirst().orElseThrow();
      assertThat(result.trees().getElement(TreePath.getPath(result.unit(), field.getType())).asType().toString())
        .isEqualTo("dependency.External");
    }
  }
}
