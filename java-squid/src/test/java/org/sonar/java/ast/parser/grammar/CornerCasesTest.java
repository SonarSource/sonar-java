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
import org.sonar.sslr.parser.LexerlessGrammar;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class CornerCasesTest {

  private LexerlessGrammar g = JavaGrammar.createGrammar();

  @Test
  public void test() {
    assertThat(g.rule(JavaPunctuator.AND))
        .matches("&")
        .notMatches("&&")
        .notMatches("&=");
    assertThat(g.rule(JavaPunctuator.ANDAND))
        .matches("&&");
    assertThat(g.rule(JavaPunctuator.ANDEQU))
        .matches("&=");

    assertThat(g.rule(JavaPunctuator.BANG))
        .matches("!")
        .notMatches("!=");
    assertThat(g.rule(JavaPunctuator.NOTEQUAL))
        .matches("!=");

    assertThat(g.rule(JavaPunctuator.BSR))
        .matches(">>>")
        .notMatches(">>>=");
    assertThat(g.rule(JavaPunctuator.BSREQU))
        .matches(">>>=");

    assertThat(g.rule(JavaPunctuator.DIV))
        .matches("/")
        .notMatches("/=");
    assertThat(g.rule(JavaPunctuator.DIVEQU))
        .matches("/=");

    assertThat(g.rule(JavaPunctuator.EQU))
        .matches("=")
        .notMatches("==");
    assertThat(g.rule(JavaPunctuator.EQUAL))
        .matches("==");

    assertThat(g.rule(JavaPunctuator.GT))
        .matches(">")
        .notMatches(">=")
        .notMatches(">>");
    assertThat(g.rule(JavaPunctuator.GE))
        .matches(">=");

    assertThat(g.rule(JavaPunctuator.HAT))
        .matches("^")
        .notMatches("^=");
    assertThat(g.rule(JavaPunctuator.HATEQU))
        .matches("^=");

    assertThat(g.rule(JavaPunctuator.LT))
        .matches("<")
        .notMatches("<=")
        .notMatches("<<");
    assertThat(g.rule(JavaPunctuator.LE))
        .matches("<=");

    assertThat(g.rule(JavaPunctuator.MINUS))
        .matches("-")
        .notMatches("-=")
        .notMatches("--");
    assertThat(g.rule(JavaPunctuator.MINUSEQU))
        .matches("-=");
    assertThat(g.rule(JavaPunctuator.DEC))
        .matches("--");

    assertThat(g.rule(JavaPunctuator.MOD))
        .matches("%")
        .notMatches("%=");
    assertThat(g.rule(JavaPunctuator.MODEQU))
        .matches("%=");

    assertThat(g.rule(JavaPunctuator.OR))
        .matches("|")
        .notMatches("|=")
        .notMatches("||");
    assertThat(g.rule(JavaPunctuator.OREQU))
        .matches("|=");
    assertThat(g.rule(JavaPunctuator.OROR))
        .matches("||");

    assertThat(g.rule(JavaPunctuator.PLUS))
        .matches("+")
        .notMatches("+=")
        .notMatches("++");
    assertThat(g.rule(JavaPunctuator.PLUSEQU))
        .matches("+=");
    assertThat(g.rule(JavaPunctuator.INC))
        .matches("++");

    assertThat(g.rule(JavaPunctuator.SL))
        .matches("<<")
        .notMatches("<<=");
    assertThat(g.rule(JavaPunctuator.SLEQU))
        .matches("<<=");

    assertThat(g.rule(JavaPunctuator.SR))
        .matches(">>")
        .notMatches(">>=");
    assertThat(g.rule(JavaPunctuator.SREQU))
        .matches(">>=");

    assertThat(g.rule(JavaPunctuator.STAR))
        .matches("*")
        .notMatches("*=");
    assertThat(g.rule(JavaPunctuator.STAREQU))
        .matches("*=");
  }

}
