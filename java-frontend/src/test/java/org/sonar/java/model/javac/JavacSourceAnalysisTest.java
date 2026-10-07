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
import javax.tools.Diagnostic;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class JavacSourceAnalysisTest {

  @Test
  void converts_and_resolves_a_simple_source() throws IOException {
    String source = "class Sample { int field = 1; int value() { return field; } }";
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("Sample.java", source, JavacParser.Options.defaults())) {
      ClassTree declaration = (ClassTree) analysis.tree().types().get(0);
      assertThat(analysis.tree().semanticModel()).isSameAs(analysis.sema());
      assertThat(declaration.simpleName().name()).isEqualTo("Sample");
      assertThat(analysis.sema().getClassType("Sample").symbol().declaration()).isSameAs(declaration);
      assertThat(declaration.symbol().name()).isEqualTo("Sample");
      VariableTree field = (VariableTree) declaration.members().get(0);
      assertThat(field.symbol().name()).isEqualTo("field");
      MethodTree method = (MethodTree) declaration.members().get(1);
      assertThat(method.symbol().name()).isEqualTo("value");
      ReturnStatementTree returnStatement = (ReturnStatementTree) method.block().body().get(0);
      assertThat(returnStatement.expression().symbolType().is("int")).isTrue();
      assertThat(analysis.sema().getClassType("java.lang.String").is("java.lang.String")).isTrue();
      assertThat(analysis.diagnostics()).noneMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR);
    }
  }
}
