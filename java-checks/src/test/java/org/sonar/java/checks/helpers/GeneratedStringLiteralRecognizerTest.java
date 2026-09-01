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
package org.sonar.java.checks.helpers;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedStringLiteralRecognizerTest {

  @Test
  void recognizes_generated_literals() {
    assertGenerated("""
      package kotlin;
      @interface Metadata {
        String[] d1();
      }
      @Metadata(d1 = {"generated"})
      class A {}
      """);

    assertGenerated("""
      package kotlin.jvm.internal;
      @interface SourceDebugExtension {
        String[] value();
      }
      @SourceDebugExtension({
        "generated",
        \"""
          generated text block
          \"""})
      class A {}
      """);

    assertGenerated("""
      package kotlin.coroutines.jvm.internal;
      @interface DebugMetadata {
        String c();
        String f();
        String m();
        String[] n();
      }
      @DebugMetadata(c = "c", f = "f", m = "m", n = {"n"})
      class A {}
      """);
  }

  @Test
  void recognizes_generated_literals_without_dependencies() {
    assertGenerated("""
      @Metadata(d1 = {"generated"})
      class A {}
      """);

    assertGenerated("""
      @kotlin.jvm.internal.SourceDebugExtension({"generated"})
      class A {}
      """);
  }

  @Test
  void does_not_recognize_other_literals() {
    assertNotGenerated("""
      package kotlin;
      @interface Metadata {
        String[] d2();
      }
      @Metadata(d2 = {"not generated"})
      class A {}
      """);

    assertNotGenerated("""
      @interface NotKotlinMetadata {
        String[] d1();
      }
      @NotKotlinMetadata(d1 = {"not generated"})
      class A {
        String value = "not generated";
        char character = 'a';
      }
      """);
  }

  @Test
  void does_not_recognize_shadowed_kotlin_metadata() {
    assertNotGenerated("""
      import kotlin.Metadata;
      class A {
        @interface Metadata {
          String[] d1();
        }
        @Metadata(d1 = {"not generated"})
        class B {}
      }
      """);
  }

  private static void assertGenerated(String source) {
    assertThat(literals(source)).allMatch(GeneratedStringLiteralRecognizer::isGenerated);
  }

  private static void assertNotGenerated(String source) {
    assertThat(literals(source)).noneMatch(GeneratedStringLiteralRecognizer::isGenerated);
  }

  private static List<LiteralTree> literals(String source) {
    List<LiteralTree> literals = new ArrayList<>();
    JParserTestUtils.parse(source).accept(new BaseTreeVisitor() {
      @Override
      public void visitLiteral(LiteralTree tree) {
        literals.add(tree);
      }
    });
    assertThat(literals).isNotEmpty();
    return literals;
  }
}
