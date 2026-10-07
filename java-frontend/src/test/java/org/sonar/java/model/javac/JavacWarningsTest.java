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
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JProblem;

import static org.assertj.core.api.Assertions.assertThat;

class JavacWarningsTest {

  @Test
  void maps_unused_import_to_sonar_import_tree() throws IOException {
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("C.java", "import java.util.List; class C {}",
      JavacParser.Options.defaults())) {
      var warnings = analysis.tree().warnings(JProblem.Type.UNUSED_IMPORT);
      assertThat(warnings).hasSize(1);
      assertThat(warnings.get(0).syntaxTree()).isSameAs(analysis.tree().imports().get(0));
      assertThat(warnings.get(0).message()).isEqualTo("The import java.util.List is never used");
    }
  }

  @Test
  void keeps_used_imports() throws IOException {
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("C.java",
      "import java.util.List; class C { List<String> values; }", JavacParser.Options.defaults())) {
      assertThat(analysis.tree().warnings(JProblem.Type.UNUSED_IMPORT)).isEmpty();
    }
  }

  @Test
  void maps_redundant_cast_warning() throws IOException {
    try (JavacSourceAnalysis analysis = JavacSourceAnalysis.analyze("C.java",
      "class C { Object f(Object value) { return (Object) value; } }", JavacParser.Options.defaults())) {
      assertThat(analysis.tree().warnings(JProblem.Type.REDUNDANT_CAST)).hasSize(1);
    }
  }
}
