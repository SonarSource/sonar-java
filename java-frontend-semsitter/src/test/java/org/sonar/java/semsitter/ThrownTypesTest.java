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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import static org.assertj.core.api.Assertions.assertThat;

/** Collects thrown types on trees the ECJ frontend gives, as those of the SemSitter frontend have the same shape. */
class ThrownTypesTest {

  private static final String SOURCE = """
    import java.io.FileReader;
    import java.io.IOException;
    import java.util.function.Supplier;

    class T {
      void resource(String name) throws IOException {
        try (FileReader reader = new FileReader(name)) {
        }
      }

      void existingResource(FileReader reader) throws IOException {
        try (reader) {
        }
      }

      void notCloseable() {
        try (Object object = new Object()) {
        }
      }

      void unknownResource() {
        try (Missing missing = new Missing()) {
        }
      }

      <E extends Exception> void typeVariable(E e) throws E {
        try {
          throw e;
        } finally {
        }
      }

      void nested() throws Exception {
        try {
          try {
            throw new IOException();
          } catch (IllegalStateException | IllegalArgumentException caught) {
          }
          Supplier<String> lazy = this::toString;
          Runnable run = () -> { throw new RuntimeException(); };
          Object local = new Object() {
            void inner() throws Exception {
              throw new Exception();
            }
          };
        } finally {
        }
      }

      void caughtByNested() {
        try {
          try {
            throw new IOException();
          } catch (IOException caught) {
          }
        } finally {
        }
      }

      void unknownCall() {
        try {
          missing();
        } finally {
        }
      }
    }
    """;

  @TempDir
  Path baseDir;

  private final Map<String, TryStatementTree> tries = new HashMap<>();

  @BeforeEach
  void parse() throws Exception {
    Replays.parseWithEcj(baseDir, "ThrownTypesSample", SOURCE).get().accept(new BaseTreeVisitor() {
      private String method;

      @Override
      public void visitMethod(MethodTree tree) {
        method = tree.simpleName().name();
        super.visitMethod(tree);
      }

      @Override
      public void visitTryStatement(TryStatementTree tree) {
        tries.putIfAbsent(method, tree);
        super.visitTryStatement(tree);
      }
    });
  }

  @Test
  void closing_a_resource_throws_what_its_close_method_declares() {
    assertThat(thrown("resource").thrown).extracting(Type::fullyQualifiedName).contains("java.io.IOException");
    assertThat(thrown("resource").unknown).isFalse();
    assertThat(thrown("existingResource").thrown).extracting(Type::fullyQualifiedName).containsExactly("java.io.IOException");
  }

  @Test
  void a_resource_without_close_or_type_is_unknown() {
    assertThat(thrown("notCloseable").unknown).isTrue();
    assertThat(thrown("unknownResource").unknown).isTrue();
  }

  @Test
  void a_thrown_type_variable_is_unknown() {
    assertThat(thrown("typeVariable").unknown).isTrue();
  }

  @Test
  void lambdas_and_nested_classes_throw_nothing_to_the_enclosing_body() {
    ThrownTypes thrown = thrown("nested");

    assertThat(thrown.unknown).isFalse();
    assertThat(thrown.thrown).extracting(Type::fullyQualifiedName).containsExactly("java.io.IOException");
  }

  @Test
  void an_exception_caught_by_a_nested_try_is_not_thrown() {
    assertThat(thrown("caughtByNested").thrown).isEmpty();
  }

  @Test
  void an_unknown_call_is_unknown() {
    assertThat(thrown("unknownCall").unknown).isTrue();
  }

  private ThrownTypes thrown(String method) {
    TryStatementTree tree = tries.get(method);
    ThrownTypes thrown = new ThrownTypes();
    thrown.scanTry(tree.resourceList(), tree.block());
    return thrown;
  }
}
