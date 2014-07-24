/*
 * SonarQube Java
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
package org.sonar.java.ast.parser.grammar;

import org.junit.Test;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class CornerCasesTest {

  private final LexerlessGrammarBuilder b = JavaGrammar.createGrammarBuilder();

  @Test
  public void test() {
    assertThat(b, JavaPunctuator.AND)
      .matches("&")
      .notMatches("&&")
      .notMatches("&=");
    assertThat(b, JavaPunctuator.ANDAND)
      .matches("&&");
    assertThat(b, JavaPunctuator.ANDEQU)
      .matches("&=");

    assertThat(b, JavaPunctuator.BANG)
      .matches("!")
      .notMatches("!=");
    assertThat(b, JavaPunctuator.NOTEQUAL)
      .matches("!=");

    assertThat(b, JavaPunctuator.BSR)
      .matches(">>>")
      .notMatches(">>>=");
    assertThat(b, JavaPunctuator.BSREQU)
      .matches(">>>=");

    assertThat(b, JavaPunctuator.DIV)
      .matches("/")
      .notMatches("/=");
    assertThat(b, JavaPunctuator.DIVEQU)
      .matches("/=");

    assertThat(b, JavaPunctuator.EQU)
      .matches("=")
      .notMatches("==");
    assertThat(b, JavaPunctuator.EQUAL)
      .matches("==");

    assertThat(b, JavaPunctuator.GT)
      .matches(">")
      .notMatches(">=")
      .notMatches(">>");
    assertThat(b, JavaPunctuator.GE)
      .matches(">=");

    assertThat(b, JavaPunctuator.HAT)
      .matches("^")
      .notMatches("^=");
    assertThat(b, JavaPunctuator.HATEQU)
      .matches("^=");

    assertThat(b, JavaPunctuator.LT)
      .matches("<")
      .notMatches("<=")
      .notMatches("<<");
    assertThat(b, JavaPunctuator.LE)
      .matches("<=");

    assertThat(b, JavaPunctuator.MINUS)
      .matches("-")
      .notMatches("-=")
      .notMatches("--");
    assertThat(b, JavaPunctuator.MINUSEQU)
      .matches("-=");
    assertThat(b, JavaPunctuator.DEC)
      .matches("--");

    assertThat(b, JavaPunctuator.MOD)
      .matches("%")
      .notMatches("%=");
    assertThat(b, JavaPunctuator.MODEQU)
      .matches("%=");

    assertThat(b, JavaPunctuator.OR)
      .matches("|")
      .notMatches("|=")
      .notMatches("||");
    assertThat(b, JavaPunctuator.OREQU)
      .matches("|=");
    assertThat(b, JavaPunctuator.OROR)
      .matches("||");

    assertThat(b, JavaPunctuator.PLUS)
      .matches("+")
      .notMatches("+=")
      .notMatches("++");
    assertThat(b, JavaPunctuator.PLUSEQU)
      .matches("+=");
    assertThat(b, JavaPunctuator.INC)
      .matches("++");

    assertThat(b, JavaPunctuator.SL)
      .matches("<<")
      .notMatches("<<=");
    assertThat(b, JavaPunctuator.SLEQU)
      .matches("<<=");

    assertThat(b, JavaPunctuator.SR)
      .matches(">>")
      .notMatches(">>=");
    assertThat(b, JavaPunctuator.SREQU)
      .matches(">>=");

    assertThat(b, JavaPunctuator.STAR)
      .matches("*")
      .notMatches("*=");
    assertThat(b, JavaPunctuator.STAREQU)
      .matches("*=");
  }

}
