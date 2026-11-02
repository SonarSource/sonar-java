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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.java.psi.JavaPsiConfiguration;
import org.sonar.java.psi.JavaPsiEnvironment;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PsiTokenIndexTest {
  @ParameterizedTest
  @MethodSource("sources")
  void physical_tokens_preserve_original_utf16_slices(String source) {
    var index = index(source);
    int previousEnd = 0;
    for (var lexeme : index.lexemes()) {
      assertThat(lexeme.start()).isGreaterThanOrEqualTo(previousEnd);
      assertThat(lexeme.token().text()).isEqualTo(source.substring(lexeme.start(), lexeme.end()));
      String prefix = source.substring(0, lexeme.start());
      assertThat(lexeme.token().line()).isEqualTo(prefix.split("\\r\\n?|\\n", -1).length);
      int lineStart = Math.max(prefix.lastIndexOf('\n'), prefix.lastIndexOf('\r')) + 1;
      assertThat(lexeme.token().range().start().columnOffset()).isEqualTo(lexeme.start() - lineStart);
      previousEnd = lexeme.end();
    }
    assertThat(index.eof().isEOF()).isTrue();
    assertThat(previousEnd).isEqualTo(source.length());
  }

  @Test
  void operators_keep_physical_spelling_and_combined_shifts() {
    assertThat(index("a>>b; a>>>b; a>>=b; a>>>=b;").lexemes().stream().map(lexeme -> lexeme.token().text()))
      .containsExactly("a", ">>", "b", ";", "a", ">>>", "b", ";", "a", ">>=", "b", ";", "a", ">>>=", "b", ";", "");
  }

  @Test
  void unicode_identifiers_keep_their_raw_ranges() {
    String source = "cl" + "\\" + "u0061ss A {}";
    var token = index(source).lexemes().getFirst().token();
    assertThat(token.text()).isEqualTo("cl" + "\\" + "u0061ss");
    assertThat(token.range().start()).isEqualTo(org.sonar.plugins.java.api.location.Position.at(1, 1));
    assertThat(token.range().end()).isEqualTo(org.sonar.plugins.java.api.location.Position.at(1, 11));
  }

  @Test
  void markdown_groups_keep_indentation_and_eof_comments() {
    var index = index("/// first\n\t/// second\nclass A {} // eof");
    var trivia = index.lexemes().getFirst().token().trivias();
    assertThat(trivia).hasSize(1);
    assertThat(trivia.getFirst().commentKind()).isEqualTo(SyntaxTrivia.CommentKind.MARKDOWN);
    assertThat(trivia.getFirst().comment()).isEqualTo("/// first\n\t/// second");
    assertThat(index.eof().trivias()).extracting(SyntaxTrivia::comment).containsExactly("// eof");
  }

  static Stream<String> sources() {
    String escape = "\\" + "u";
    return Stream.of(
      "", " \t\r\n", "\"\"\"text\"\"\"", "/// first\n\n/// second", "/**/ /** doc */ /*block*/ // line\r\nclass A {} // eof",
      "/// first\n/// second\nclass A {}", "/// one\r\n\t/// two\r/// three\n",
      "class A {\r\n\tint 𐐀;\rString x=\"😀\";\n}",
      "class A { String x=\"\"\"\r\n  abc\\\r\n  def\r\n  \"\"\"; }",
      "int x=0xCA_FE; long y=0b11L; double a=.25e+2, b=0x1.fp-2; float f=1f; char c='\\n';",
      "a>>b; a>>>b; a>>=b; a>>>=b; a<<b; a<=b; a!=b; a++ + --b;",
      "cl" + escape + "0061ss A { int " + escape + "0078 = 1; }",
      "// comment " + escape + "000a class A {}",
      "// eof", "/** doc */", "/* one */ /* two */"
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {"#", "`", "/* unterminated", "\"unterminated", "'x", "\"\\q\"", "'ab'", "\"\"\"\nunterminated"})
  void rejects_malformed_lexical_input(String source) {
    assertThatThrownBy(() -> index(source)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void contextual_closers_share_slices_without_duplicating_trivia() {
    var index = index("/* c */ >>>");
    var first = index.slice(8, 9);
    assertThat(first.text()).isEqualTo(">");
    assertThat(first.trivias()).hasSize(1);
    assertThat(index.slice(8, 9)).isSameAs(first);
    assertThat(index.slice(9, 10).trivias()).isEmpty();
    assertThat(index.slice(10, 11).trivias()).isEmpty();
    assertThat(index.eof().isEOF()).isTrue();
  }

  @Test
  void escaped_comment_delimiters_preserve_the_existing_rejection() {
    String source = "\\" + "u002f\\" + "u002a comment */ class A {}";
    assertThatThrownBy(() -> index(source)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid comment kind");
  }

  private static PsiTokenIndex index(String source) {
    var config = new JavaPsiConfiguration(null, List.of(), 26, true, true);
    try (var environment = JavaPsiEnvironment.open(config, List.of(new JavaPsiEnvironment.SourceSnapshot(Path.of("File.java"), source, StandardCharsets.UTF_8)))) {
      return environment.read((project, files) -> new PsiTokenIndex(source));
    }
  }

}
