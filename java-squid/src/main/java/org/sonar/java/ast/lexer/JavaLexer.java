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
import com.sonar.sslr.impl.channel.PunctuatorChannel;
import com.sonar.sslr.impl.channel.RegexpChannel;
import org.sonar.channel.Channel;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;

import java.nio.charset.Charset;

public final class JavaLexer {

  private JavaLexer() {
  }

  private static final String EXP = "([Ee][+-]?+[0-9_]++)";
  private static final String BINARY_EXP = "([Pp][+-]?+[0-9_]++)";

  private static final String FLOAT_SUFFIX = "[fFdD]";
  private static final String INT_SUFFIX = "[lL]";

  public static Lexer create(Charset charset) {
    // FIXME BOM

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
        // Decimal
        .withChannel(digitStart(new RegexpChannel(JavaTokenType.FLOATING_LITERAL, "[0-9_]++\\.([0-9_]++)?+" + EXP + "?+" + FLOAT_SUFFIX + "?+")))
        // Decimal
        .withChannel(start('.', new RegexpChannel(JavaTokenType.FLOATING_LITERAL, "\\.[0-9][0-9_]*+" + EXP + "?+" + FLOAT_SUFFIX + "?+")))
        // Decimal
        .withChannel(digitStart(new RegexpChannel(JavaTokenType.FLOATING_LITERAL, "[0-9_]++" + FLOAT_SUFFIX)))
        .withChannel(digitStart(new RegexpChannel(JavaTokenType.FLOATING_LITERAL, "[0-9_]++" + EXP + FLOAT_SUFFIX + "?+")))
        // Hexadecimal
        .withChannel(start('0', new RegexpChannel(JavaTokenType.FLOATING_LITERAL, "0[xX][0-9_a-fA-F]++\\.[0-9_a-fA-F]*+" + BINARY_EXP + "?+" + FLOAT_SUFFIX + "?+")))
        // Hexadecimal
        .withChannel(start('0', new RegexpChannel(JavaTokenType.FLOATING_LITERAL, "0[xX][0-9_a-fA-F]++" + BINARY_EXP + FLOAT_SUFFIX + "?+")))

        // Integer Literals
        // Hexadecimal
        .withChannel(start('0', new RegexpChannel(JavaTokenType.INTEGER_LITERAL, "0[xX][0-9_a-fA-F]++" + INT_SUFFIX + "?+")))
        // Binary (Java 7)
        .withChannel(start('0', new RegexpChannel(JavaTokenType.INTEGER_LITERAL, "0[bB][01_]++" + INT_SUFFIX + "?+")))
        // Decimal and Octal
        .withChannel(digitStart(new RegexpChannel(JavaTokenType.INTEGER_LITERAL, "[0-9_]++" + INT_SUFFIX + "?+")))

        .withChannel(new JavaIdentifierAndKeywordChannel(JavaKeyword.values()))

        .withChannel(new PunctuatorChannel(JavaPunctuator.values()));

    return builder.build();
  }

  /**
   * Syntactic sugar.
   */
  private static Channel<Lexer> digitStart(Channel<Lexer> channel) {
    return new DigitStartChannel(channel);
  }

  /**
   * Syntactic sugar.
   */
  private static Channel<Lexer> start(char ch, Channel<Lexer> channel) {
    return new CharStartChannel(ch, channel);
  }

}
