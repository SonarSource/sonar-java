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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.javac.JavacParser;
import org.sonar.java.model.javac.JavacSourceAnalysis;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class JParserSemanticTest {

  @Test
  void declaration_and_usage_share_symbol() throws IOException {
    try (JavacSourceAnalysis analysis = analyze("class C { int field; int method() { return field; } }")) {
      ClassTree type = (ClassTree) analysis.tree().types().get(0);
      VariableTree field = (VariableTree) type.members().get(0);
      MethodTree method = (MethodTree) type.members().get(1);
      ReturnStatementTree statement = (ReturnStatementTree) method.block().body().get(0);
      IdentifierTree usage = (IdentifierTree) statement.expression();
      assertThat(type.symbol().name()).isEqualTo("C");
      assertThat(field.symbol()).isSameAs(usage.symbol());
      assertThat(usage.symbolType().is("int")).isTrue();
      assertThat(usage.symbol().declaration()).isSameAs(field);
      assertThat(usage.symbol().usages()).contains(usage);
    }
  }

  @Test
  void method_call_resolves_to_declaration() throws IOException {
    try (JavacSourceAnalysis analysis = analyze("class C { int value() { return 1; } int other() { return value(); } }")) {
      ClassTree type = (ClassTree) analysis.tree().types().get(0);
      MethodTree declaration = (MethodTree) type.members().get(0);
      MethodTree caller = (MethodTree) type.members().get(1);
      ReturnStatementTree statement = (ReturnStatementTree) caller.block().body().get(0);
      assertThat(statement.expression().symbolType().is("int")).isTrue();
      assertThat(analysis.sema().symbolFor(statement.expression()).declaration()).isSameAs(declaration);
    }
  }

  @Test
  void generic_type_resolution_uses_javac() throws IOException {
    try (JavacSourceAnalysis analysis = analyze("class C<T> { java.util.List<String> values; }")) {
      ClassTree type = (ClassTree) analysis.tree().types().get(0);
      VariableTree field = (VariableTree) type.members().get(0);
      assertThat(type.symbol().name()).isEqualTo("C");
      assertThat(field.symbol().type().is("java.util.List")).isTrue();
      assertThat(field.symbol().type().typeArguments()).hasSize(1);
      assertThat(field.symbol().type().typeArguments().get(0).is("java.lang.String")).isTrue();
    }
  }

  @Test
  void class_lookup_uses_compiler_symbols() throws IOException {
    try (JavacSourceAnalysis analysis = analyze("class C {}")) {
      assertThat(analysis.sema().getClassType("java.lang.String").is("java.lang.String")).isTrue();
      assertThat(analysis.sema().getClassType("int[]").isArray()).isTrue();
      assertThat(analysis.sema().getClassType("does.not.Exist").isUnknown()).isTrue();
    }
  }

  private static JavacSourceAnalysis analyze(String source) throws IOException {
    return JavacSourceAnalysis.analyze("C.java", source,
      JavacParser.Options.defaults().withClasspath(List.of()));
  }
}
