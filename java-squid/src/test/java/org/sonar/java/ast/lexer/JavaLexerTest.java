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

import com.google.common.base.Charsets;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.LexerException;
import com.sonar.sslr.impl.channel.UnknownCharacterChannel;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;

import static com.sonar.sslr.test.lexer.LexerMatchers.hasComment;
import static com.sonar.sslr.test.lexer.LexerMatchers.hasToken;
import static com.sonar.sslr.test.lexer.LexerMatchers.hasTokens;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JavaLexerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static Lexer lexer;

  @BeforeClass
  public static void init() {
    lexer = JavaLexer.create(Charsets.UTF_8);
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.6
   */
  @Test
  public void whitespaces() {
    assertThat(lexer.lex(" \t\f\n\r").size(), is(1));
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.7
   */
  @Test
  public void comments() {
    assertThat(lexer.lex("// This is a comment"), hasComment("// This is a comment"));
    assertThat(lexer.lex("// This is a comment \n and_this_is_not"), hasComment("// This is a comment "));
    assertThat(lexer.lex("// This is a comment \r\n and_this_is_not"), hasComment("// This is a comment "));

    assertThat(lexer.lex("/* This is a comment \n and the second line */"), hasComment("/* This is a comment \n and the second line */"));
    assertThat(lexer.lex("/* this \n comment /* \n // /** ends \n here: */"), hasComment("/* this \n comment /* \n // /** ends \n here: */"));
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.8
   */
  @Test
  public void identifiers() {
    assertThat(lexer.lex("String"), hasToken("String", GenericTokenType.IDENTIFIER));
    assertThat(lexer.lex("i3"), hasToken("i3", GenericTokenType.IDENTIFIER));
    assertThat("identifier with unicode", lexer.lex("αβγ"), hasToken("αβγ", GenericTokenType.IDENTIFIER));
    assertThat(lexer.lex("MAX_VALUE"), hasToken("MAX_VALUE", GenericTokenType.IDENTIFIER));
    assertThat(lexer.lex("isLetterOrDigit"), hasToken("isLetterOrDigit", GenericTokenType.IDENTIFIER));
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.9
   */
  @Test
  public void keywords() {
    assertThat(lexer.lex("abstract"), hasToken("abstract", JavaKeyword.ABSTRACT));
    assertThat(lexer.lex("while"), hasToken("while", JavaKeyword.WHILE));
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.10.1
   * http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.1
   */
  @Test
  public void integer_literals() {
    // Decimal
    assertThat(lexer.lex("0"), hasToken("0", JavaTokenType.INTEGER_LITERAL));
    assertThat(lexer.lex("543"), hasToken("543", JavaTokenType.INTEGER_LITERAL));
    assertThat(lexer.lex("543l"), hasToken("543l", JavaTokenType.LONG_LITERAL));
    assertThat(lexer.lex("543L"), hasToken("543L", JavaTokenType.LONG_LITERAL));

    // Hexadecimal
    assertThat(lexer.lex("0xFF"), hasToken("0xFF", JavaTokenType.INTEGER_LITERAL));
    assertThat(lexer.lex("0xFFl"), hasToken("0xFFl", JavaTokenType.LONG_LITERAL));
    assertThat(lexer.lex("0xFFL"), hasToken("0xFFL", JavaTokenType.LONG_LITERAL));

    assertThat(lexer.lex("0XFF"), hasToken("0XFF", JavaTokenType.INTEGER_LITERAL));
    assertThat(lexer.lex("0XFFl"), hasToken("0XFFl", JavaTokenType.LONG_LITERAL));
    assertThat(lexer.lex("0XFFL"), hasToken("0XFFL", JavaTokenType.LONG_LITERAL));

    // Octal
    assertThat(lexer.lex("077"), hasToken("077", JavaTokenType.INTEGER_LITERAL));
    assertThat(lexer.lex("077l"), hasToken("077l", JavaTokenType.LONG_LITERAL));
    assertThat(lexer.lex("077L"), hasToken("077L", JavaTokenType.LONG_LITERAL));

    // Binary (new in Java 7)
    assertThat(lexer.lex("0b1010"), hasToken("0b1010", JavaTokenType.INTEGER_LITERAL));
    assertThat(lexer.lex("0b1010l"), hasToken("0b1010l", JavaTokenType.LONG_LITERAL));
    assertThat(lexer.lex("0b1010L"), hasToken("0b1010L", JavaTokenType.LONG_LITERAL));

    assertThat(lexer.lex("0B1010"), hasToken("0B1010", JavaTokenType.INTEGER_LITERAL));
    assertThat(lexer.lex("0B1010l"), hasToken("0B1010l", JavaTokenType.LONG_LITERAL));
    assertThat(lexer.lex("0B1010L"), hasToken("0B1010L", JavaTokenType.LONG_LITERAL));
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.10.2
   */
  @Test
  public void floating_point_literals() {
    // Decimal

    // with dot at the end
    assertThat(lexer.lex("1234."), hasToken("1234.", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("1234.E1"), hasToken("1234.E1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("1234.e+1"), hasToken("1234.e+1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("1234.E-1"), hasToken("1234.E-1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("1234.f"), hasToken("1234.f", JavaTokenType.FLOAT_LITERAL));

    // with dot between
    assertThat(lexer.lex("12.34"), hasToken("12.34", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("12.34E1"), hasToken("12.34E1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("12.34e+1"), hasToken("12.34e+1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("12.34E-1"), hasToken("12.34E-1", JavaTokenType.DOUBLE_LITERAL));

    assertThat(lexer.lex("12.34f"), hasToken("12.34f", JavaTokenType.FLOAT_LITERAL));
    assertThat(lexer.lex("12.34E1F"), hasToken("12.34E1F", JavaTokenType.FLOAT_LITERAL));
    assertThat(lexer.lex("12.34E+1d"), hasToken("12.34E+1d", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("12.34e-1D"), hasToken("12.34e-1D", JavaTokenType.DOUBLE_LITERAL));

    // with dot at the beginning
    assertThat(lexer.lex(".1234"), hasToken(".1234", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex(".1234e1"), hasToken(".1234e1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex(".1234E+1"), hasToken(".1234E+1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex(".1234E-1"), hasToken(".1234E-1", JavaTokenType.DOUBLE_LITERAL));

    assertThat(lexer.lex(".1234f"), hasToken(".1234f", JavaTokenType.FLOAT_LITERAL));
    assertThat(lexer.lex(".1234E1F"), hasToken(".1234E1F", JavaTokenType.FLOAT_LITERAL));
    assertThat(lexer.lex(".1234e+1d"), hasToken(".1234e+1d", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex(".1234E-1D"), hasToken(".1234E-1D", JavaTokenType.DOUBLE_LITERAL));

    // without dot
    assertThat(lexer.lex("1234e1"), hasToken("1234e1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("1234E+1"), hasToken("1234E+1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("1234E-1"), hasToken("1234E-1", JavaTokenType.DOUBLE_LITERAL));

    assertThat(lexer.lex("1234E1f"), hasToken("1234E1f", JavaTokenType.FLOAT_LITERAL));
    assertThat(lexer.lex("1234e+1d"), hasToken("1234e+1d", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("1234E-1D"), hasToken("1234E-1D", JavaTokenType.DOUBLE_LITERAL));

    // Hexadecimal

    // with dot at the end
    assertThat(lexer.lex("0xAF."), hasToken("0xAF.", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0XAF.P1"), hasToken("0XAF.P1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0xAF.p+1"), hasToken("0xAF.p+1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0XAF.p-1"), hasToken("0XAF.p-1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0xAF.f"), hasToken("0xAF.f", JavaTokenType.FLOAT_LITERAL));

    // with dot between
    assertThat(lexer.lex("0XAF.BC"), hasToken("0XAF.BC", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0xAF.BCP1"), hasToken("0xAF.BCP1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0XAF.BCp+1"), hasToken("0XAF.BCp+1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0xAF.BCP-1"), hasToken("0xAF.BCP-1", JavaTokenType.DOUBLE_LITERAL));

    assertThat(lexer.lex("0xAF.BCf"), hasToken("0xAF.BCf", JavaTokenType.FLOAT_LITERAL));
    assertThat(lexer.lex("0xAF.BCp1F"), hasToken("0xAF.BCp1F", JavaTokenType.FLOAT_LITERAL));
    assertThat(lexer.lex("0XAF.BCP+1d"), hasToken("0XAF.BCP+1d", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0XAF.BCp-1D"), hasToken("0XAF.BCp-1D", JavaTokenType.DOUBLE_LITERAL));

    // without dot
    assertThat(lexer.lex("0xAFp1"), hasToken("0xAFp1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0XAFp+1"), hasToken("0XAFp+1", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0xAFp-1"), hasToken("0xAFp-1", JavaTokenType.DOUBLE_LITERAL));

    assertThat(lexer.lex("0XAFp1f"), hasToken("0XAFp1f", JavaTokenType.FLOAT_LITERAL));
    assertThat(lexer.lex("0xAFp+1d"), hasToken("0xAFp+1d", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0XAFp-1D"), hasToken("0XAFp-1D", JavaTokenType.DOUBLE_LITERAL));
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.1
   */
  @Test
  public void underscore_in_numeric_literals() {
    assertThat(lexer.lex("1_000_000"), hasToken("1_000_000", JavaTokenType.INTEGER_LITERAL));
    assertThat(lexer.lex("0x7fff_ffff_ffff_ffffL"), hasToken("0x7fff_ffff_ffff_ffffL", JavaTokenType.LONG_LITERAL));
    assertThat(lexer.lex("3.14_15F"), hasToken("3.14_15F", JavaTokenType.FLOAT_LITERAL));
    assertThat(lexer.lex("1_234e1_0"), hasToken("1_234e1_0", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("0x1.ffff_ffff_ffff_fP1_023"), hasToken("0x1.ffff_ffff_ffff_fP1_023", JavaTokenType.DOUBLE_LITERAL));
    assertThat(lexer.lex("5_______2"), hasToken("5_______2", JavaTokenType.INTEGER_LITERAL));

    assertThat(lexer.lex("_0"), hasToken("_0", GenericTokenType.IDENTIFIER));
    assertThat(lexer.lex("._list"), allOf(hasToken(".", JavaPunctuator.DOT), hasToken("_list", GenericTokenType.IDENTIFIER)));
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.10.3
   */
  @Test
  public void boolean_literals() {
    assertThat(lexer.lex("true"), hasToken("true", JavaKeyword.TRUE));
    assertThat(lexer.lex("false"), hasToken("false", JavaKeyword.FALSE));
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.10.4
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.10.6
   */
  @Test
  public void character_literals() {
    assertThat("single character", lexer.lex("'a'"), hasToken("'a'", JavaTokenType.CHARACTER_LITERAL));
    assertThat("escaped LF", lexer.lex("'\\n'"), hasToken("'\\n'", JavaTokenType.CHARACTER_LITERAL));
    assertThat("escaped quote", lexer.lex("'\\''"), hasToken("'\\''", JavaTokenType.CHARACTER_LITERAL));
    assertThat("octal escape", lexer.lex("'\\177'"), hasToken("'\\177'", JavaTokenType.CHARACTER_LITERAL));
    assertThat("unicode escape", lexer.lex("'\\u03a9'"), hasToken("'\\u03a9'", JavaTokenType.CHARACTER_LITERAL));
    assertThat("unicode escape", lexer.lex("'\\uuuu005Cr'"), hasToken("'\\uuuu005Cr'", JavaTokenType.CHARACTER_LITERAL));
    assertThat("unicode escape", lexer.lex("'\\u005c\\u005c'"), hasToken("'\\u005c\\u005c'", JavaTokenType.CHARACTER_LITERAL));
  }

  @Test
  public void incomplete_character_literal() {
    thrown.expect(LexerException.class);
    lexer.lex("'\n'");
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.10.5
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.10.6
   */
  @Test
  public void string_literals() {
    assertThat("regular string", lexer.lex("\"string\""), hasToken("\"string\"", GenericTokenType.LITERAL));
    assertThat("empty string", lexer.lex("\"\""), hasToken("\"\"", GenericTokenType.LITERAL));
    assertThat("escaped LF", lexer.lex("\"\\n\""), hasToken("\"\\n\"", GenericTokenType.LITERAL));
    assertThat("escaped double quotes", lexer.lex("\"string, which contains \\\"escaped double quotes\\\"\""),
        hasToken("\"string, which contains \\\"escaped double quotes\\\"\"", GenericTokenType.LITERAL));
    assertThat("octal escape", lexer.lex("\"string \\177\""), hasToken("\"string \\177\"", GenericTokenType.LITERAL));
    assertThat("unicode escape", lexer.lex("\"string \\u03a9\""), hasToken("\"string \\u03a9\"", GenericTokenType.LITERAL));
  }

  @Test
  public void incomplete_string_literal() {
    thrown.expect(LexerException.class);
    lexer.lex("\"");
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.10.7
   */
  @Test
  public void null_literal() {
    assertThat(lexer.lex("null"), hasToken("null", JavaKeyword.NULL));
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.11
   */
  @Test
  public void separators() {
    assertThat(lexer.lex("("), hasToken("(", JavaPunctuator.LPAR));
    assertThat(lexer.lex(")"), hasToken(")", JavaPunctuator.RPAR));
    assertThat(lexer.lex("{"), hasToken("{", JavaPunctuator.LWING));
    assertThat(lexer.lex("}"), hasToken("}", JavaPunctuator.RWING));
    assertThat(lexer.lex("["), hasToken("[", JavaPunctuator.LBRK));
    assertThat(lexer.lex("]"), hasToken("]", JavaPunctuator.RBRK));
    assertThat(lexer.lex(";"), hasToken(";", JavaPunctuator.SEMI));
    assertThat(lexer.lex(","), hasToken(",", JavaPunctuator.COMMA));
    assertThat(lexer.lex("."), hasToken(".", JavaPunctuator.DOT));
  }

  /**
   * http://docs.oracle.com/javase/specs/jls/se5.0/html/lexical.html#3.12
   */
  @Test
  public void operators() {
    assertThat(lexer.lex("=="), hasToken("==", JavaPunctuator.EQUAL));

    // Special cases, which should be handled by parser, but not by lexer
    assertThat(lexer.lex(">>>"), hasTokens(">", ">", ">", "EOF"));
    assertThat(lexer.lex(">>>="), hasTokens(">", ">", ">", "=", "EOF"));
    assertThat(lexer.lex(">="), hasTokens(">", "=", "EOF"));
    assertThat(lexer.lex(">>"), hasTokens(">", ">", "EOF"));
    assertThat(lexer.lex(">>="), hasTokens(">", ">", "=", "EOF"));
  }

  @Test
  public void bom() {
    assertThat(lexer.lex(Character.toString(UnknownCharacterChannel.BOM_CHAR)), hasTokens("EOF"));
  }

}
