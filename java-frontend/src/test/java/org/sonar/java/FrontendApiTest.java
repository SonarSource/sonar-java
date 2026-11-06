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
package org.sonar.java;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.JNullabilityData;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.JParserConfig.Result;
import org.sonar.java.model.JSymbolMetadataNullabilityHelper;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.diagnostics.Problem;
import org.sonar.plugins.java.api.diagnostics.Warning;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityTarget;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrontendApiTest {

  @Test
  void original_single_file_call_resolves_semantics_through_frontend_interfaces() {
    var version = JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION;
    JParserConfig.ParserConfigConstructor constructor = JParserConfig.Mode.FILE_BY_FILE::create;
    JParserConfig config = constructor.apply(version, List.of(), false);
    JParser parser = config.astParser();
    JavaTree.CompilationUnitTreeImpl tree = JParser.parse(parser, version.toString(), "Sample.java", "class Sample { String value; }");
    try {
      ClassTree declaration = (ClassTree) tree.types().get(0);
      Symbol.TypeSymbol symbol = declaration.symbol();
      Type type = symbol.type();
      VariableTree field = (VariableTree) declaration.members().get(0);
      assertThat(symbol.name()).isEqualTo("Sample");
      assertThat(type.isSubtypeOf("java.lang.Object")).isTrue();
      assertThat(field.symbol().type().is("java.lang.String")).isTrue();
      assertThat(tree.sema.getClassType("java.lang.String").isUnknown()).isFalse();
    } finally {
      tree.getEnvironmentCleaner().run();
    }
  }

  @ParameterizedTest
  @EnumSource(JParserConfig.Mode.class)
  void original_batch_callback_uses_nested_result(JParserConfig.Mode mode) {
    InputFile input = TestUtils.inputFile("src/test/files/metrics/Classes.java");
    AtomicInteger callbacks = new AtomicInteger();
    mode.create(JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION, List.of())
      .parse(List.of(input), () -> false, new AnalysisProgress(1), (file, result) -> {
        assertThat(file).isSameAs(input);
        try {
          JavaTree.CompilationUnitTreeImpl tree = result.get();
          assertThat(tree.types()).isNotEmpty();
          tree.getEnvironmentCleaner().run();
        } catch (Exception e) {
          throw new AssertionError(e);
        }
        callbacks.incrementAndGet();
      });
    assertThat(callbacks.get()).isEqualTo(1);
  }

  @Test
  void nested_result_rethrows_original_exception() {
    Exception exception = new IOException("failed to read source");
    Result result = Result.failure(exception);
    assertThatThrownBy(result::get).isSameAs(exception);
  }

  @Test
  void original_utility_names_remain_available() {
    assertThat(JUtils.defaultValue(Symbol.MethodSymbol.UNKNOWN_METHOD)).isNull();
    assertThat(JUtils.wrapTypeIfPrimitive(Type.UNKNOWN)).isSameAs(Type.UNKNOWN);
    assertThat(JNullabilityData.noNullabilityAnnotationAt(NullabilityLevel.CLASS))
      .isSameAs(JSymbolMetadataNullabilityHelper.getNullabilityDataAtLevel(Symbols.EMPTY_METADATA, NullabilityTarget.METHOD, NullabilityLevel.CLASS));
    assertThat(JNullabilityData.unknownNullabilityAt(NullabilityLevel.PACKAGE).level()).isEqualTo(NullabilityLevel.PACKAGE);
  }

  @Test
  void frontend_contracts_are_interfaces() {
    assertThat(List.of(JParser.class, JParserConfig.class, Sema.class, Type.class, Symbol.class,
      Symbol.TypeSymbol.class, Symbol.MethodSymbol.class, Symbol.VariableSymbol.class,
      SymbolMetadata.class, Problem.class, Warning.class)).allMatch(Class::isInterface);
  }

  @Test
  void shared_frontend_classes_do_not_depend_on_jdt() throws Exception {
    Path sonarClasses = Path.of(JParser.class.getProtectionDomain().getCodeSource().getLocation().toURI()).resolve("org/sonar");
    try (var classes = Files.walk(sonarClasses)) {
      for (Path path : classes.filter(file -> file.toString().endsWith(".class")).toList()) {
        Path relative = sonarClasses.relativize(path);
        if (relative.startsWith(Path.of("java", "jdt"))) {
          continue;
        }
        String bytecode = Files.readString(path, StandardCharsets.ISO_8859_1);
        assertThat(bytecode).as("%s", relative).doesNotContain("org/eclipse/jdt/", "org/eclipse/core/");
        if (!relative.equals(Path.of("java", "model", "JParserConfig$Mode.class"))) {
          assertThat(bytecode).as("%s", relative).doesNotContain("org/sonar/java/jdt/");
        }
      }
    }
  }
}
