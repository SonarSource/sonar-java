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
import org.sonar.java.javac.source.util.TreePathScanner;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavacSemaTest {

  @Test
  void resolves_named_array_and_tree_types() throws IOException {
    String source = "class Sample { java.util.List<String> values; }";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSema sema = new JavacSema(parsed);
      assertThat(sema.getClassType("java.lang.String[][]").isArray()).isTrue();
      assertThat(sema.getClassType("java.lang.String[][]").fullyQualifiedName()).isEqualTo("java.lang.String[][]");
      assertThat(sema.getClassType("int").isPrimitive()).isTrue();
      assertThat(sema.getClassType("not.present.Type")).isSameAs(Type.UNKNOWN);

      ClassTree declaration = (ClassTree) parsed.unit().getTypeDecls().get(0);
      VariableTree field = declaration.getMembers().stream().filter(VariableTree.class::isInstance)
        .map(VariableTree.class::cast).findFirst().orElseThrow();
      assertThat(sema.type(TreePath.getPath(parsed.unit(), field)).fullyQualifiedName())
        .isEqualTo("java.util.List");
      assertThat(sema.symbol(TreePath.getPath(parsed.unit(), field)).name()).isEqualTo("values");
    }
  }

  @Test
  void records_declaration_and_usages_for_resolved_symbols() throws IOException {
    String source = "class Sample { int value; int get() { return value; } }";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSema sema = new JavacSema(parsed);
      ClassTree declaration = (ClassTree) parsed.unit().getTypeDecls().get(0);
      VariableTree field = declaration.getMembers().stream().filter(VariableTree.class::isInstance)
        .map(VariableTree.class::cast).findFirst().orElseThrow();
      TreePath fieldPath = TreePath.getPath(parsed.unit(), field);
      Symbol fieldSymbol = sema.symbol(fieldPath);
      org.sonar.plugins.java.api.tree.VariableTree sonarDeclaration = mock(org.sonar.plugins.java.api.tree.VariableTree.class);
      IdentifierTree sonarUsage = mock(IdentifierTree.class);

      sema.register(parsed.unit(), field, sonarDeclaration);
      new TreePathScanner<Void, Void>() {
        @Override
        public Void visitIdentifier(org.sonar.java.javac.source.tree.IdentifierTree identifier, Void unused) {
          if (identifier.getName().contentEquals("value")) {
            sema.register(parsed.unit(), identifier, sonarUsage);
          }
          return super.visitIdentifier(identifier, unused);
        }
      }.scan(parsed.unit(), null);

      assertThat(fieldSymbol.declaration()).isSameAs(sonarDeclaration);
      assertThat(fieldSymbol.usages()).containsExactly(sonarUsage);
      assertThat(sema.symbolFor(sonarDeclaration)).isSameAs(fieldSymbol);
      assertThat(sema.typeFor(sonarDeclaration).is("int")).isTrue();
      assertThat(sema.symbolFor(sonarUsage)).isSameAs(fieldSymbol);
      assertThat(sema.symbolFor(mock(IdentifierTree.class))).isSameAs(Symbol.UNKNOWN_SYMBOL);
      assertThat(sema.typeFor(mock(IdentifierTree.class))).isSameAs(Type.UNKNOWN);
    }
  }

  @Test
  void declaration_name_is_not_a_symbol_usage() throws IOException {
    String source = "class Sample { int value; int get() { return value; } }";
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", source,
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSema sema = new JavacSema(parsed);
      ClassTree type = (ClassTree) parsed.unit().getTypeDecls().get(0);
      VariableTree field = type.getMembers().stream().filter(VariableTree.class::isInstance)
        .map(VariableTree.class::cast).findFirst().orElseThrow();
      Symbol fieldSymbol = sema.symbol(TreePath.getPath(parsed.unit(), field));
      org.sonar.plugins.java.api.tree.VariableTree declaration = mock(org.sonar.plugins.java.api.tree.VariableTree.class);
      IdentifierTree name = mock(IdentifierTree.class);
      when(declaration.simpleName()).thenReturn(name);

      sema.registerUsage(TreePath.getPath(parsed.unit(), field), name);
      sema.registerDeclaration(TreePath.getPath(parsed.unit(), field), declaration);
      sema.registerUsage(TreePath.getPath(parsed.unit(), field), name);

      assertThat(fieldSymbol.usages()).isEmpty();
      assertThat(sema.symbolFor(name)).isSameAs(fieldSymbol);
      assertThat(sema.typeFor(name).is("int")).isTrue();
    }
  }

  @Test
  void reports_undefined_types_from_javac_diagnostics() throws IOException {
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java", "class Sample { Missing field; }",
      JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSema sema = new JavacSema(parsed);
      assertThat(sema.undefinedTypes()).hasSize(1);
      assertThat(sema.undefinedTypes().iterator().next().message()).contains("Missing");
    }
  }

  @Test
  void resolves_import_symbol_from_qualified_identifier() throws IOException {
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java",
      "import java.util.List; class Sample {}", JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSema sema = new JavacSema(parsed);
      org.sonar.plugins.java.api.tree.ImportTree imported = mock(org.sonar.plugins.java.api.tree.ImportTree.class);
      sema.register(parsed.unit(), parsed.unit().getImports().get(0), imported);
      assertThat(sema.symbolFor(imported).isTypeSymbol()).isTrue();
      assertThat(sema.symbolFor(imported).type().is("java.util.List")).isTrue();
    }
  }

  @Test
  void detects_duplicate_method_declarations() throws IOException {
    try (JavacParser.Result parsed = JavacParser.parse("Sample.java",
      "class Sample { void method() {} void method() {} }", JavacParser.Options.defaults().withAnalysis(true))) {
      JavacSema sema = new JavacSema(parsed);
      ClassTree type = (ClassTree) parsed.unit().getTypeDecls().get(0);
      org.sonar.java.javac.source.tree.MethodTree second = type.getMembers().stream()
        .filter(org.sonar.java.javac.source.tree.MethodTree.class::isInstance)
        .map(org.sonar.java.javac.source.tree.MethodTree.class::cast)
        .filter(method -> method.getName().contentEquals("method"))
        .skip(1).findFirst().orElseThrow();
      org.sonar.plugins.java.api.tree.MethodTree duplicate = mock(org.sonar.plugins.java.api.tree.MethodTree.class);
      sema.register(parsed.unit(), second, duplicate);
      assertThat(sema.hasDuplicateDeclarationError(duplicate)).isTrue();
    }
  }
}
