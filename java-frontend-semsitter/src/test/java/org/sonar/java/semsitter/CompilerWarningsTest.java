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
package org.sonar.java.semsitter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;

class CompilerWarningsTest {

  private static final String SOURCE = """
    class A {
      int plain;
      volatile int shared;
      void m(int local) {
        int inner = 0;
        plain = plain;
        shared = shared;
        local += local;
        inner = local;
      }
    }
    """;

  @TempDir
  Path baseDir;

  @Test
  void a_reference_names_its_type_and_the_parameter_types_of_a_method() {
    assertThat(CompilerWarnings.referenceNames("Foo#bar(List, Map) the label")).containsExactly("Foo", "List", "Map");
    assertThat(CompilerWarnings.referenceNames("#m(A)")).containsExactly("m", "A");
  }

  @Test
  void a_qualified_parameter_type_names_only_its_first_segment() {
    assertThat(CompilerWarnings.referenceNames("#m(java.util.List, Map)")).containsExactly("m", "java", "Map");
  }

  @Test
  void a_reference_wrapped_onto_a_continuation_line_is_read_without_its_star() {
    assertThat(CompilerWarnings.javadocReferenceNames("/**\n * {@link\n * Foo}\n * @see\n *   Bar\n */")).containsExactly("Foo", "Bar");
    assertThat(CompilerWarnings.javadocReferenceNames("/// [Baz]")).containsExactly("Baz");
  }

  @Test
  void the_description_after_a_reference_names_nothing() {
    assertThat(CompilerWarnings.referenceNames("IllegalStateException if the cache (Set by init) is stale")).containsExactly("IllegalStateException");
    assertThat(CompilerWarnings.referenceNames("#m(A)) label (B)")).containsExactly("m", "A");
  }

  @Test
  void only_a_plain_self_assignment_of_a_non_volatile_variable_has_no_effect() throws Exception {
    List<AssignmentExpressionTree> assignments = new ArrayList<>();
    parse().accept(new BaseTreeVisitor() {
      @Override
      public void visitAssignmentExpression(AssignmentExpressionTree tree) {
        assignments.add(tree);
        super.visitAssignmentExpression(tree);
      }
    });

    assertThat(assignments).extracting(CompilerWarnings::hasNoEffect).containsExactly(true, false, false, false);
  }

  @Test
  void only_the_comments_of_members_and_types_document_a_declaration() throws Exception {
    List<Tree> declarations = new ArrayList<>();
    parse().accept(new BaseTreeVisitor() {
      @Override
      public void visitClass(ClassTree tree) {
        declarations.add(tree);
        super.visitClass(tree);
      }

      @Override
      public void visitMethod(MethodTree tree) {
        declarations.add(tree);
        super.visitMethod(tree);
      }

      @Override
      public void visitVariable(VariableTree tree) {
        declarations.add(tree);
        super.visitVariable(tree);
      }
    });

    assertThat(declarations).extracting(CompilerWarnings::isDocumentedDeclaration).containsExactly(true, true, true, true, false, false);
  }

  private Tree parse() throws Exception {
    return Replays.parseWithEcj(baseDir, "A", SOURCE).get();
  }
}
