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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class JavacTreeConverterTest {

  @Test
  void converts_simple_declarations() throws IOException {
    String source = "package p; import java.util.List; public class A { int field = 1; A() {} int method() { return field; } void fail(RuntimeException error) { throw error; } }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);

      assertThat(unit.packageDeclaration().packageName().firstToken().text()).isEqualTo("p");
      assertThat(unit.imports()).hasSize(1);
      assertThat(((ImportTree) unit.imports().get(0)).importKeyword().text()).isEqualTo("import");
      assertThat(unit.types()).hasSize(1);
      ClassTree type = (ClassTree) unit.types().get(0);
      assertThat(type.kind()).isEqualTo(Tree.Kind.CLASS);
      assertThat(type.simpleName().name()).isEqualTo("A");
      assertThat(type.members()).hasSize(4);
      VariableTree field = (VariableTree) type.members().get(0);
      MethodTree constructor = (MethodTree) type.members().get(1);
      MethodTree method = (MethodTree) type.members().get(2);
      assertThat(field.simpleName().name()).isEqualTo("field");
      assertThat(field.initializer()).isNotNull();
      assertThat(constructor.kind()).isEqualTo(Tree.Kind.CONSTRUCTOR);
      assertThat(constructor.simpleName().name()).isEqualTo("A");
      assertThat(method.simpleName().name()).isEqualTo("method");
      assertThat(method.block().body()).hasSize(1);
      MethodTree failingMethod = (MethodTree) type.members().get(3);
      assertThat(failingMethod.parameters()).hasSize(1);
      assertThat(failingMethod.block().body()).hasSize(1);
      assertThat(failingMethod.block().body().get(0).kind()).isEqualTo(Tree.Kind.THROW_STATEMENT);
      assertThat(unit.eofToken().text()).isEmpty();
    }
  }

  @Test
  void converts_control_flow_statements() throws IOException {
    String source = "class A { void method() { if (true) {} else {} while (false) {} for (int i = 0; i < 3; i++) {} } }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      MethodTree method = (MethodTree) ((ClassTree) unit.types().get(0)).members().get(0);
      assertThat(method.block().body()).extracting(Tree::kind)
        .containsExactly(Tree.Kind.IF_STATEMENT, Tree.Kind.WHILE_STATEMENT, Tree.Kind.FOR_STATEMENT);
    }
  }

  @Test
  void converts_additional_statements() throws IOException {
    String source = "class A { void method(String[] values) { outer: for (String value : values) { assert value != null : value; synchronized (this) { break outer; } } do { continue; } while (false); } }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      MethodTree method = (MethodTree) ((ClassTree) unit.types().get(0)).members().get(0);
      assertThat(method.block().body()).extracting(Tree::kind)
        .containsExactly(Tree.Kind.LABELED_STATEMENT, Tree.Kind.DO_STATEMENT);
      assertThat(((org.sonar.plugins.java.api.tree.LabeledStatementTree) method.block().body().get(0)).statement().kind())
        .isEqualTo(Tree.Kind.FOR_EACH_STATEMENT);
    }
  }

  @Test
  void converts_generic_declarations_and_annotations() throws IOException {
    String source = "@Deprecated class A<T extends Number> { @Deprecated java.util.List<String> values; <U> U identity(U value) { return value; } }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      ClassTree type = (ClassTree) unit.types().get(0);
      assertThat(type.modifiers().annotations()).hasSize(1);
      assertThat(type.typeParameters()).hasSize(1);
      assertThat(((VariableTree) type.members().get(0)).type().kind()).isEqualTo(Tree.Kind.PARAMETERIZED_TYPE);
      assertThat(((MethodTree) type.members().get(1)).typeParameters()).hasSize(1);
    }
  }

  @Test
  void locates_declaration_tokens_after_annotations() throws IOException {
    String source = """
      @interface Tag { Class<?> value(); }
      class Outer {
        @Tag(Outer.class) // class marker
        class Nested {}
        @Deprecated // interface marker
        @interface NestedAnnotation {}
      }
      """;
    try (JavacParser.Result parsed = JavacParser.parse("Outer.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      ClassTree outer = (ClassTree) unit.types().get(1);
      ClassTree nested = (ClassTree) outer.members().get(0);
      ClassTreeImpl annotation = (ClassTreeImpl) outer.members().get(1);

      assertThat(nested.declarationKeyword().trivias()).extracting(trivia -> trivia.comment()).containsExactly("// class marker");
      assertThat(annotation.atToken().trivias()).extracting(trivia -> trivia.comment()).containsExactly("// interface marker");
    }
  }

  @Test
  void converts_arrays_and_method_references() throws IOException {
    String source = "class A { int[] values = new int[] {1, 2}; java.util.function.Supplier<String> supplier = String::new; }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      ClassTree type = (ClassTree) unit.types().get(0);
      assertThat(((VariableTree) type.members().get(0)).initializer().kind()).isEqualTo(Tree.Kind.NEW_ARRAY);
      assertThat(((VariableTree) type.members().get(1)).initializer().kind()).isEqualTo(Tree.Kind.METHOD_REFERENCE);
    }
  }

  @Test
  void converts_expression_lambda() throws IOException {
    String source = "class A { java.util.function.Function<String,Integer> f = s -> s.length(); }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      VariableTree field = (VariableTree) ((ClassTree) unit.types().get(0)).members().get(0);
      assertThat(field.initializer().kind()).isEqualTo(Tree.Kind.LAMBDA_EXPRESSION);
    }
  }

  @Test
  void converts_try_resources_catch_and_finally() throws IOException {
    String source = "class A { void m() { try (java.io.InputStream input = null) { input.read(); } catch (java.io.IOException | RuntimeException e) { throw new IllegalStateException(e); } finally { System.out.println(1); } } }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      MethodTree method = (MethodTree) ((ClassTree) unit.types().get(0)).members().get(0);
      assertThat(method.block().body()).extracting(Tree::kind).containsExactly(Tree.Kind.TRY_STATEMENT);
    }
  }

  @Test
  void converts_switch_statement() throws IOException {
    String source = "class A { void m(int n) { switch (n) { case 1: break; default: return; } } }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      MethodTree method = (MethodTree) ((ClassTree) unit.types().get(0)).members().get(0);
      assertThat(method.block().body()).extracting(Tree::kind).containsExactly(Tree.Kind.SWITCH_STATEMENT);
    }
  }

  @Test
  void converts_record_components() throws IOException {
    String source = "record R(int x, String y) {}";
    try (JavacParser.Result parsed = JavacParser.parse("R.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      ClassTree record = (ClassTree) unit.types().get(0);
      assertThat(record.kind()).isEqualTo(Tree.Kind.RECORD);
      assertThat(record.recordComponents()).hasSize(2);
      assertThat(record.members()).isEmpty();
    }
  }

  @Test
  void converts_var_and_nested_generic_type() throws IOException {
    String source = "class A { java.util.Map<String,java.util.List<Integer>> values; void m() { var n = 1; } }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      ClassTree type = (ClassTree) unit.types().get(0);
      assertThat(((VariableTree) type.members().get(0)).type().kind()).isEqualTo(Tree.Kind.PARAMETERIZED_TYPE);
      MethodTree method = (MethodTree) type.members().get(1);
      assertThat(((VariableTree) method.block().body().get(0)).type().kind()).isEqualTo(Tree.Kind.VAR_TYPE);
    }
  }

  @Test
  void converts_anonymous_class_and_lambda_block() throws IOException {
    String source = "class A { Runnable anonymous = new Runnable() { public void run() {} }; Runnable lambda = () -> { System.out.println(1); }; }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      ClassTree type = (ClassTree) unit.types().get(0);
      assertThat(((VariableTree) type.members().get(0)).initializer().kind()).isEqualTo(Tree.Kind.NEW_CLASS);
      assertThat(((VariableTree) type.members().get(1)).initializer().kind()).isEqualTo(Tree.Kind.LAMBDA_EXPRESSION);
    }
  }

  @Test
  void converts_module_directives() throws IOException {
    String source = "open module a.b { requires static transitive java.sql; exports a.b to c.d; opens a.c; uses java.lang.Runnable; provides java.lang.Runnable with a.b.Runner; }";
    try (JavacParser.Result parsed = JavacParser.parse("module-info.java", source)) {
      JavaTree.CompilationUnitTreeImpl unit = JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source);
      assertThat(unit.moduleDeclaration().moduleDirectives()).hasSize(5);
      assertThat(unit.moduleDeclaration().moduleName().toString()).isNotEmpty();
    }
  }

  @Test
  void records_declarations_and_identifier_usages() throws IOException {
    String source = "class A { int field; int method() { return field + 1; } }";
    try (JavacParser.Result parsed = JavacParser.parse("A.java", source)) {
      List<Map.Entry<com.sun.source.tree.Tree, Tree>> registrations = new ArrayList<>();
      JavacTreeConverter.convert(parsed.unit(), parsed.trees(), source,
        (javacTree, sonarTree) -> registrations.add(Map.entry(javacTree, sonarTree)));
      assertThat(registrations).extracting(entry -> entry.getValue().kind())
        .contains(Tree.Kind.CLASS, Tree.Kind.VARIABLE, Tree.Kind.METHOD, Tree.Kind.IDENTIFIER,
          Tree.Kind.PLUS, Tree.Kind.INT_LITERAL, Tree.Kind.RETURN_STATEMENT);
    }
  }
}
