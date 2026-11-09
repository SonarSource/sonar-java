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

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.java.model.JParserConfig;
import org.sonar.plugins.java.api.diagnostics.Problem;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;
import org.sonar.plugins.java.api.diagnostics.Warning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PsiDiagnosticProviderTest {
  @ParameterizedTest
  @MethodSource("warnings")
  void warning_kinds_preserve_import_cast_assignment_and_catch_behavior(String source, List<ProblemKind> expected) {
    assertThat(warnings("C.java", source, List.of())).extracting(Problem::type).containsExactlyInAnyOrderElementsOf(expected);
  }

  static Stream<Arguments> warnings() {
    return Stream.of(
      Arguments.of("package test; import java.util.List; import test.C; import java.lang.Object; class C { void f(String s) { String o = (String) ((String) s); } }", List.of(ProblemKind.UNUSED_IMPORT, ProblemKind.UNUSED_IMPORT, ProblemKind.REDUNDANT_CAST, ProblemKind.REDUNDANT_CAST)),
      Arguments.of("import java.util.*; import static java.util.Collections.*; class C { Object f() { return emptyList(); } }", List.of(ProblemKind.UNUSED_IMPORT)),
      Arguments.of("import java.util.List; import java.util.List; class C { List<String> x; }", List.of(ProblemKind.UNUSED_IMPORT)),
      Arguments.of("import java.util.List; import java.util.Set; /** {@link List} */ class C {}", List.of(ProblemKind.UNUSED_IMPORT)),
      Arguments.of("import missing.Type; import java.util.List; class C { Type x; }", List.of()),
      Arguments.of("import java.util.List; import java.util.List; class C {}", List.of(ProblemKind.UNUSED_IMPORT, ProblemKind.UNUSED_IMPORT)),
      Arguments.of("import java.util.*; import java.util.List; class C { List<?> x; }", List.of(ProblemKind.UNUSED_IMPORT)),
      Arguments.of("import static java.util.Collections.emptyList; import static java.util.Collections.emptySet; class C { Object x=emptyList(); }", List.of(ProblemKind.UNUSED_IMPORT)),
      Arguments.of("import java.util.List; /** @see List#size() */ class C {}", List.of()),
      Arguments.of("import java.util.List; /** Just List text */ class C {}", List.of(ProblemKind.UNUSED_IMPORT)),
      Arguments.of("import module java.sql; class C {}", List.of(ProblemKind.UNUSED_IMPORT)),
      Arguments.of("class C { <T> T f(T t) { return (T)t; } }", List.of(ProblemKind.REDUNDANT_CAST)),
      Arguments.of("class C { static class R<E extends Exception> implements AutoCloseable { public void close() throws E {} } void f() { try (R<java.io.FileNotFoundException> r = new R<>()) {} catch (java.io.FileNotFoundException e) {} catch (java.io.IOException e) {} } }", List.of(ProblemKind.MASKED_CATCH)),
      Arguments.of("class C { void f() { try { try { throw new java.io.FileNotFoundException(); } finally {} } catch (java.io.FileNotFoundException e) {} catch (java.io.IOException e) {} } }", List.of(ProblemKind.MASKED_CATCH)),
      Arguments.of("class C { <T> T id(T t) { return t; } Object f(String s) { return id((Object)s); } }", List.of()),
      Arguments.of("class C { <T> T id(T t) { return t; } String f(String s) { return id((String)s); } }", List.of(ProblemKind.REDUNDANT_CAST)),
      Arguments.of("class C { void f(Object o, int i) {} void g(Integer i) { f((Object)i, (int)i); } }", List.of(ProblemKind.REDUNDANT_CAST, ProblemKind.REDUNDANT_CAST)),
      Arguments.of("class C { static int s; int x; C f() { return this; } void g(C c) { c.s=c.s; f().s=s; this.s=s; C.s=C.s; f().x=f().x; } }", List.of(ProblemKind.ASSIGNMENT_HAS_NO_EFFECT, ProblemKind.ASSIGNMENT_HAS_NO_EFFECT)),
      Arguments.of("class C { int x; class D extends C { void f() { C.this.x=this.x; this.x=C.this.x; C.this.x=C.this.x; } } }", List.of()),
      Arguments.of("class Élève {\r\n void f(String s) { String o = (Str\\u0069ng)s; o = (String)s; } }", List.of(ProblemKind.REDUNDANT_CAST, ProblemKind.REDUNDANT_CAST)),
      Arguments.of("class C { volatile int x; void f() { x=x; } }", List.of()),
      Arguments.of("class C { void f(Object... values) {} void g(String[] s) { f((Object)s); f((Object[])s); } }", List.of()),
      Arguments.of("class C { Missing f(Missing a) { return (Missing)a; } }", List.of()),
      Arguments.of("class C { Object f(String s) { return (Object) s; } void f(Object o) {} void f(String s) {} void g(String s) { f((Object)s); } }", List.of(ProblemKind.REDUNDANT_CAST)),
      Arguments.of("class C { int f(int i, long l) { return (int)i + (int)l; } long g(int i,long l) { return (long)i + l; } }", List.of(ProblemKind.REDUNDANT_CAST, ProblemKind.REDUNDANT_CAST)),
      Arguments.of("class C { java.util.function.Supplier<String> s = (java.util.function.Supplier<String>) () -> \"a\"; java.util.function.Supplier<Object> o=(java.util.function.Supplier<Object>) Object::new; }", List.of()),
      Arguments.of("class C { void f() { try { throw new java.io.IOException(); } catch (Exception e) {} catch (java.io.IOException e) {} } }", List.of()),
      Arguments.of("class C { int x; static int s; void f(C c, int x, int[] a) { x=x; this.x=x; this.x=this.x; c.x=c.x; s=C.s; a[0]=a[0]; } }", List.of(ProblemKind.ASSIGNMENT_HAS_NO_EFFECT, ProblemKind.ASSIGNMENT_HAS_NO_EFFECT, ProblemKind.ASSIGNMENT_HAS_NO_EFFECT)),
      Arguments.of("class C { void f(Object o, String s, int i, Integer n) { Object a=(Object)s; String b=(String)s; int j=(int)i; long k=(long)i; Integer m=(Integer)i; int p=(int)n; } }", List.of(ProblemKind.REDUNDANT_CAST, ProblemKind.REDUNDANT_CAST, ProblemKind.REDUNDANT_CAST, ProblemKind.REDUNDANT_CAST, ProblemKind.REDUNDANT_CAST, ProblemKind.REDUNDANT_CAST)),
      Arguments.of("class C { void f(Object o) {} void f(String s) {} void g(String s) { f((Object)s); f((String)s); } }", List.of(ProblemKind.REDUNDANT_CAST)),
      Arguments.of("class C { void f() { try { throw new java.io.FileNotFoundException(); } catch (java.io.FileNotFoundException e) {} catch (java.io.IOException e) {} } }", List.of(ProblemKind.MASKED_CATCH)),
      Arguments.of("class C { void f() { try { throw new java.io.IOException(); } catch (java.io.IOException e) {} catch (Exception e) {} } }", List.of())
    );
  }

  @Test
  void warning_messages_and_positions_refer_to_the_original_source() {
    String source = "class C {\n void f(String s) { String o = (String)s; }\n}";
    var warnings = warnings("C.java", source, List.of());
    assertThat(warnings).hasSize(1);
    var warning = warnings.getFirst();
    assertThat(warning.type()).isEqualTo(ProblemKind.REDUNDANT_CAST);
    assertThat(warning.message()).isEqualTo("Unnecessary cast from String to String");
    assertThat(warning.syntaxTree().firstToken().range().start().line()).isEqualTo(2);
    assertThat(warning.syntaxTree().firstToken().range().start().columnOffset()).isEqualTo(31);
    assertThat(warning.syntaxTree().kind()).isEqualTo(org.sonar.plugins.java.api.tree.Tree.Kind.TYPE_CAST);
    assertThat(warning.syntaxTree().firstToken().text()).isEqualTo("(");
  }

  @ParameterizedTest
  @MethodSource("malformed")
  void malformed_input_keeps_error_location_and_message(String source, int line, String message) {
    assertThatThrownBy(() -> problems(source, 26, false)).isInstanceOfSatisfying(RecognitionException.class, failure -> {
      assertThat(failure.getLine()).isEqualTo(line);
      assertThat(failure.getMessage()).isEqualTo(message);
    });
  }

  static Stream<Arguments> malformed() {
    return Stream.of(
      Arguments.of("class C", 1, "Parse error at line 1 column 7: Syntax error, insert \"ClassBody\" to complete CompilationUnit"),
      Arguments.of("class C {", 1, "Parse error at line 1 column 9: Syntax error, insert \"}\" to complete ClassBody"),
      Arguments.of("class C { void f() { }", 1, "Parse error at line 1 column 22: Syntax error, insert \"}\" to complete ClassBody"),
      Arguments.of("class C { int x }", 1, "Parse error at line 1 column 15: Syntax error, insert \";\" to complete ClassBodyDeclarations"),
      Arguments.of("class C { void f() { int x = ; } }", 1, "Parse error at line 1 column 28: Syntax error on token \"=\", VariableInitializer expected after this token"),
      Arguments.of("class C { void f() { return } }", 1, "Parse error at line 1 column 22: Syntax error, insert \";\" to complete BlockStatements"),
      Arguments.of("import a; ; import b;", 1, "Parse error at line 1 column 13: Syntax error on token \"import\", int expected"),
      Arguments.of("class C { char c = ''; }", 1, "Parse error at line 1 column 20: Invalid character constant"),
      Arguments.of("class C { void m( }", 1, "Parse error at line 1 column 9: Syntax error, insert \"}\" to complete ClassBody"),
      Arguments.of("class C { void good() {} void bad( }", 1, "Parse error at line 1 column 24: Syntax error, insert \"}\" to complete ClassBody"),
      Arguments.of("class C { void m(int x }", 1, "Parse error at line 1 column 9: Syntax error, insert \"}\" to complete ClassBody"),
      Arguments.of("class Élève {\r\n void méthode( }", 1, "Parse error at line 1 column 13: Syntax error, insert \"}\" to complete ClassBody"),
      Arguments.of("class C { void m(", 1, "Parse error at line 1 column 9: Syntax error, insert \"}\" to complete ClassBody")
    );
  }

  @ParameterizedTest
  @MethodSource("missingTypes")
  void unresolved_code_keeps_recoverable_problem_kinds_and_messages(String source, List<Problem> expected) {
    assertThat(problems(source, 26, false)).containsExactlyInAnyOrderElementsOf(expected);
  }

  static Stream<Arguments> missingTypes() {
    return Stream.of(
      Arguments.of("interface Foo { public foo(); }", List.of(new PsiProblem("Return type for the method is missing", ProblemKind.PREVIEW_FEATURE_USED))),
      Arguments.of("import org.foo.Bar; class C { void f(Bar b) {} }", List.of(new PsiProblem("Bar cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE), new PsiProblem("The import org.foo cannot be resolved", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C { Missing x; Missing y; }", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("import java.util.List; class C { Missing x; }", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("import java.util.Missing; class C {}", List.of(new PsiProblem("The import java.util.Missing cannot be resolved", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C { Missing.Type x; }", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C extends Missing {}", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C implements Missing {}", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("@Missing class C {}", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C<T extends Missing> {}", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C { void f() throws Missing {} }", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C { Missing<String> x; }", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C { void f() { Missing.m(); } }", List.of(new PsiProblem("Missing cannot be resolved", ProblemKind.PREVIEW_FEATURE_USED))),
      Arguments.of("class C { Object x=new Missing(); }", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C { class D implements Missing {} }", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("package p; class C implements Missing {}", List.of(new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C { interface I extends Missing {} class D implements I {} }", List.of(new PsiProblem("The hierarchy of the type D is inconsistent", ProblemKind.PREVIEW_FEATURE_USED), new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C { Missing[] values = {new C()}; }", List.of(new PsiProblem("Type mismatch: cannot convert from C to Missing", ProblemKind.PREVIEW_FEATURE_USED), new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE))),
      Arguments.of("class C { Object[] values = java.util.stream.Stream.of(1).map(x -> new Missing()).toArray(); }", List.of(new PsiProblem("Cannot infer type argument(s) for <R> map(Function<? super T,? extends R>)", ProblemKind.PREVIEW_FEATURE_USED), new PsiProblem("Missing cannot be resolved to a type", ProblemKind.UNDEFINED_TYPE)))
    );
  }

  @Test
  void missing_interface_return_type_keeps_its_recovery_failure() {
    assertThatThrownBy(() -> problems("interface Foo { public foo(); // comment\n }", 26, false))
      .isInstanceOf(IndexOutOfBoundsException.class).hasMessage("Missing method return type");
  }

  @ParameterizedTest
  @MethodSource("features")
  void source_levels_gate_language_features(String source, int minimum, List<Integer> rejectedLevels, ProblemKind kind) {
    for (int version : List.of(9, 10, 11, 14, 15, 16, 17, 20, 21, 24, 25, 26)) {
      if (rejectedLevels.contains(version)) {
        assertThatThrownBy(() -> problems(source, version, false)).isInstanceOf(RecognitionException.class);
      } else if (version < minimum) {
        assertThat(problems(source, version, false)).extracting(Problem::type).contains(kind);
      } else {
        assertThat(problems(source, version, false)).as("Java %s: %s", version, source).isEmpty();
      }
    }
  }

  static Stream<Arguments> features() {
    return Stream.of(
      Arguments.of("class C { void f() { var x = 1; } }", 10, List.of(), ProblemKind.UNDEFINED_TYPE),
      Arguments.of("class C { java.util.function.IntUnaryOperator f = (var x) -> x; }", 11, List.of(10), ProblemKind.UNDEFINED_TYPE),
      Arguments.of("class C { String s = \"\"\"\ntext\n\"\"\"; }", 15, List.of(), ProblemKind.PREVIEW_FEATURE_USED),
      Arguments.of("class C { boolean f(Object o) { return o instanceof String s; } }", 16, List.of(), ProblemKind.PREVIEW_FEATURE_USED),
      Arguments.of("class C { record R(int i) {} boolean f(Object o) { return o instanceof R(int i); } }", 21, List.of(), ProblemKind.PREVIEW_FEATURE_USED),
      Arguments.of("class C { int f(Object o) { return switch(o) { case String s -> 1; default -> 0; }; } }", 21, List.of(9, 10, 11), ProblemKind.PREVIEW_FEATURE_USED),
      Arguments.of("sealed class C permits D {} final class D extends C {}", 17, List.of(9, 10, 11, 14, 15, 16), ProblemKind.PREVIEW_FEATURE_USED),
      Arguments.of("class C { void f() { int _ = 1; } }", 22, List.of(9, 10, 11, 14, 15, 16, 17, 20, 21), ProblemKind.PREVIEW_FEATURE_USED),
      Arguments.of("import module java.sql; class C {}", 25, List.of(), ProblemKind.PREVIEW_FEATURE_USED),
      Arguments.of("class C { C() { int i = 1; super(); } }", 25, List.of(), ProblemKind.PREVIEW_FEATURE_USED),
      Arguments.of("void main() {}", 25, List.of(), ProblemKind.PREVIEW_FEATURE_USED),
      Arguments.of("record C(int x) {}", 16, List.of(), ProblemKind.PREVIEW_FEATURE_USED));
  }

  @ParameterizedTest
  @ValueSource(ints = {9, 10, 11, 14, 15, 16, 17, 20, 21, 24, 25, 26})
  void primitive_patterns_require_preview(int version) {
    String source = "class C { boolean f(Object o) { return o instanceof int i; } }";
    assertThat(problems(source, version, false)).extracting(Problem::type).contains(ProblemKind.PREVIEW_FEATURE_USED);
    assertThat(problems(source, 26, true)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = {7, 22, 26})
  void repository_sensor_source_reports_missing_dependencies_without_failing(int version) throws IOException {
    String source = Files.readString(Path.of("../sonar-java-plugin/src/test/java/org/sonar/plugins/java/JavaSensorTest.java"));
    assertThat(problems(source, version, false)).extracting(Problem::type).contains(ProblemKind.UNDEFINED_TYPE);
  }

  private static List<Problem> problems(String source, int version, boolean preview) {
    var configuration = new JavaPsiConfiguration(null, List.of(), version, preview, false);
    var input = new JavaPsiEnvironment.SourceSnapshot(Path.of("C.java"), PsiTokenIndex.unicodeInput(source).text(), StandardCharsets.UTF_8);
    try (var environment = JavaPsiEnvironment.open(configuration, List.of(input))) {
      return environment.read((project, files) -> {
        var tree = PsiParser.convert(environment, project, files.getFirst(), configuration, source, () -> {});
        return List.copyOf(tree.semanticProblems());
      });
    }
  }

  private static List<Warning> warnings(String name, String source, List<File> classpath) {
    try (var parsed = PsiSemanticTestSupport.open(name, source, classpath, JParserConfig.MAXIMUM_SUPPORTED_JAVA_VERSION)) {
      List<Warning> result = new ArrayList<>();
      for (var kind : List.of(ProblemKind.UNUSED_IMPORT, ProblemKind.REDUNDANT_CAST, ProblemKind.ASSIGNMENT_HAS_NO_EFFECT, ProblemKind.MASKED_CATCH)) {
        result.addAll(parsed.tree().warnings(kind));
      }
      return result;
    }
  }
}
