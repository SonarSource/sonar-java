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

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.java.test.TestResourceScope;
import org.sonar.plugins.java.api.tree.ClassTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestResourceScope.Scoped
class SemanticFixtureTest {

  @Test
  void fixtures_expose_declarations_types_metadata_defaults_and_recovery() {
    SemanticFixture fixture = SemanticFixture.scoped("@interface A { String value() default \"default\"; } @A class C { Missing missing; int field; void m() {} }");
    assertThat(fixture.method(0, 0).symbol().defaultValue()).isEqualTo("default");
    assertThat(fixture.annotations(fixture.declaration(1).symbol())).hasSize(1);
    assertThat(fixture.annotations(fixture.declaration(1).symbol()).get(0).symbol().name()).isEqualTo("A");
    assertThat(fixture.variable(1, 0).symbol().type().isUnknown()).isTrue();
    assertThat(fixture.variable(1, 1).symbol().type()).isSameAs(fixture.type("int"));
    assertThat(fixture.method(1, 2).symbol().declaration()).isSameAs(fixture.method(1, 2));
    assertThat(fixture.type("java.lang.String")).isSameAs(fixture.type("java.lang.String"));
  }

  @Test
  void injected_backend_failure_closes_resources_and_preserves_failure() {
    AtomicInteger cleaned = new AtomicInteger();
    IllegalStateException failure = new IllegalStateException("injected backend");
    FrontendBackend backend = configuration -> new FrontendSession() {
      {
        own(cleaned::incrementAndGet);
      }

      @Override
      JavaTree.CompilationUnitTreeImpl parse(String unitName, String source) {
        throw failure;
      }

      @Override
      void parseBatch(FrontendBatch inputs, org.sonar.java.ProgressMonitor monitor,
        java.util.function.BiConsumer<org.sonar.api.batch.fs.InputFile, ParsedUnit> action) {
        throw new AssertionError("unexpected batch parse");
      }
    };
    assertThatThrownBy(() -> SemanticFixture.open(backend,
      FrontendConfiguration.from(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of(), false), "A.java", "class A {}"))
      .isSameAs(failure);
    assertThat(cleaned).hasValue(1);
  }

  @Test
  void helper_preserves_string_input_and_existing_file_line_normalization(@TempDir java.nio.file.Path directory) throws Exception {
    String source = "class A {\r\n  String value = \"😀\";\r\n}\r\n";
    var direct = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
    assertThat(direct.eofToken().range().start().line()).isEqualTo(4);
    java.nio.file.Path file = directory.resolve("A.java");
    java.nio.file.Files.writeString(file, source);
    var fromFile = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(file.toFile());
    assertThat(fromFile.eofToken().range().start().line()).isEqualTo(3);
    assertThat(((ClassTree) direct.types().get(0)).symbol().name()).isEqualTo(((ClassTree) fromFile.types().get(0)).symbol().name());
  }

  @Test
  void effective_configuration_is_immutable_and_keeps_classpath_order() {
    var classpath = new java.util.ArrayList<>(List.of(new File("first"), new File("second")));
    FrontendConfiguration configuration = FrontendConfiguration.from(new JavaVersionImpl(17, true), classpath, true);
    classpath.clear();
    assertThat(configuration.classpath()).containsExactly(new File("first"), new File("second"));
    assertThatThrownBy(() -> configuration.classpath().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThat(configuration.sourceVersion()).isEqualTo("17");
    assertThat(configuration.previewEnabled()).isTrue();
    assertThat(configuration.ignoreUnnamedModuleForSplitPackage()).isTrue();
  }
}
