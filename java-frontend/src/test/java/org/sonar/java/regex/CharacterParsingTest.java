/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.regex;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonar.java.regex.ast.JavaCharacter;
import org.sonar.java.regex.ast.RegexSource;
import org.sonar.plugins.java.api.tree.LiteralTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.regex.RegexParserTestUtils.getAllStringLiteralsFromFile;

class CharacterParsingTest {

  private static final List<LiteralTree> literals = new ArrayList<>();

  @BeforeAll
  static void setup() {
    literals.addAll(getAllStringLiteralsFromFile(new File("src/test/files/regex/CharacterParsing.java")));
  }

  @Test
  void parsingUnicode() {
    // "\t\u1234"
    LiteralTree source = literals.get(0);
    assertThat(source.value()).isEqualTo("\"\\t\\u1234\"");

    RegexSource regexSource = new RegexSource(Collections.singletonList(source));

    List<JavaCharacter> unicodeCharacters = parseUnicode(regexSource);
    assertThat(unicodeCharacters.stream().map(JavaCharacter::getCharacter))
      .hasSize(3)
      .containsExactly('\\', 't', '\u1234');

    List<JavaCharacter> javaCharacters = parseJavaCharacters(regexSource);
    assertThat(javaCharacters.stream().map(JavaCharacter::getCharacter))
      .hasSize(2)
      .containsExactly('\t', '\u1234');
  }

  @Test
  void escapedBackslashes() {
    // "\\\\u+[a-fA-F0-9]{4}"
    LiteralTree source = literals.get(1);
    assertThat(source.value()).isEqualTo("\"\\\\\\\\u+[a-fA-F0-9]{4}\"");

    RegexSource regexSource = new RegexSource(Collections.singletonList(source));

    List<JavaCharacter> unicodeCharacters = parseUnicode(regexSource);
    assertThat(unicodeCharacters.stream().map(JavaCharacter::getCharacter))
      .hasSize(20)
      .startsWith('\\', '\\', '\\', '\\', 'u', '+', '[');

    List<JavaCharacter> javaCharacters = parseJavaCharacters(regexSource);
    assertThat(javaCharacters.stream().map(JavaCharacter::getCharacter))
      .hasSize(18)
      .startsWith('\\', '\\', 'u', '+', '[');
  }

  private static List<JavaCharacter> parseJavaCharacters(RegexSource regexSource) {
    JavaCharacterParser characterParser = new JavaCharacterParser(regexSource);
    List<JavaCharacter> javaCharacters = new ArrayList<>();
    while (!characterParser.isAtEnd()) {
      javaCharacters.add(characterParser.getCurrent());
      characterParser.moveNext();
    }
    return javaCharacters;
  }

  private static List<JavaCharacter> parseUnicode(RegexSource regexSource) {
    JavaUnicodeEscapeParser unicodeParser = new JavaUnicodeEscapeParser(regexSource);
    List<JavaCharacter> unicodeCharacters = new ArrayList<>();
    while (unicodeParser.getCurrent() != null) {
      unicodeCharacters.add(unicodeParser.getCurrent());
      unicodeParser.moveNext();
    }
    return unicodeCharacters;
  }

}
