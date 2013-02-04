/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.ast.lexer;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.channel.BomCharacterChannel;
import com.sonar.sslr.impl.channel.PunctuatorChannel;
import com.sonar.sslr.impl.channel.UnknownCharacterChannel;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;

import java.nio.charset.Charset;

public final class JavaLexer {

  private JavaLexer() {
  }

  public static Lexer create(Charset charset) {
    Lexer.Builder builder = Lexer.builder()
        .withCharset(charset)
        .withFailIfNoChannelToConsumeOneCharacter(true)

        // Channels, which consumes more frequently should come first.
        // Whitespace character occurs more frequently than any other, and thus come first:
        .withChannel(new WhitespaceChannel())

        // Comments
        .withChannel(new InlineCommentChannel())
        .withChannel(new MultilineCommentChannel())

        // String Literals
        .withChannel(new CharacterLiteralChannel('"', GenericTokenType.LITERAL))

        // Character Literals
        .withChannel(new CharacterLiteralChannel('\'', JavaTokenType.CHARACTER_LITERAL))

        // Floating-Point Literals
        .withChannel(new FloatLiteralChannel())

        // Integer Literals
        .withChannel(new IntegerLiteralChannel())

        .withChannel(new JavaIdentifierAndKeywordChannel(JavaKeyword.values()))

        .withChannel(new PunctuatorChannel(JavaPunctuator.values()))

        .withChannel(new BomCharacterChannel())

        .withChannel(new UnknownCharacterChannel(true));

    return builder.build();
  }

}
