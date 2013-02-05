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
package org.sonar.java.ast.parser.grammar;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class CornerCasesTest {

  private LexerlessGrammar g = JavaGrammar.createGrammar();

  @Test
  public void test() {
    assertThat(g.rule(JavaGrammar.AND))
        .matches("&")
        .notMatches("&&")
        .notMatches("&=");
    assertThat(g.rule(JavaGrammar.ANDAND))
        .matches("&&");
    assertThat(g.rule(JavaGrammar.ANDEQU))
        .matches("&=");

    assertThat(g.rule(JavaGrammar.BANG))
        .matches("!")
        .notMatches("!=");
    assertThat(g.rule(JavaGrammar.NOTEQUAL))
        .matches("!=");

    assertThat(g.rule(JavaGrammar.BSR))
        .matches(">>>")
        .notMatches(">>>=");
    assertThat(g.rule(JavaGrammar.BSREQU))
        .matches(">>>=");

    assertThat(g.rule(JavaGrammar.DIV))
        .matches("/")
        .notMatches("/=");
    assertThat(g.rule(JavaGrammar.DIVEQU))
        .matches("/=");

    assertThat(g.rule(JavaGrammar.EQU))
        .matches("=")
        .notMatches("==");
    assertThat(g.rule(JavaGrammar.EQUAL))
        .matches("==");

    assertThat(g.rule(JavaGrammar.GT))
        .matches(">")
        .notMatches(">=")
        .notMatches(">>");
    assertThat(g.rule(JavaGrammar.GE))
        .matches(">=");

    assertThat(g.rule(JavaGrammar.HAT))
        .matches("^")
        .notMatches("^=");
    assertThat(g.rule(JavaGrammar.HATEQU))
        .matches("^=");

    assertThat(g.rule(JavaGrammar.LT))
        .matches("<")
        .notMatches("<=")
        .notMatches("<<");
    assertThat(g.rule(JavaGrammar.LE))
        .matches("<=");

    assertThat(g.rule(JavaGrammar.MINUS))
        .matches("-")
        .notMatches("-=")
        .notMatches("--");
    assertThat(g.rule(JavaGrammar.MINSEQU))
        .matches("-=");
    assertThat(g.rule(JavaGrammar.DEC))
        .matches("--");

    assertThat(g.rule(JavaGrammar.MOD))
        .matches("%")
        .notMatches("%=");
    assertThat(g.rule(JavaGrammar.MODEQU))
        .matches("%=");

    assertThat(g.rule(JavaGrammar.OR))
        .matches("|")
        .notMatches("|=")
        .notMatches("||");
    assertThat(g.rule(JavaGrammar.OREQU))
        .matches("|=");
    assertThat(g.rule(JavaGrammar.OROR))
        .matches("||");

    assertThat(g.rule(JavaGrammar.PLUS))
        .matches("+")
        .notMatches("+=")
        .notMatches("++");
    assertThat(g.rule(JavaGrammar.PLUSEQU))
        .matches("+=");
    assertThat(g.rule(JavaGrammar.INC))
        .matches("++");

    assertThat(g.rule(JavaGrammar.SL))
        .matches("<<")
        .notMatches("<<=");
    assertThat(g.rule(JavaGrammar.SLEQU))
        .matches("<<=");

    assertThat(g.rule(JavaGrammar.SR))
        .matches(">>")
        .notMatches(">>=");
    assertThat(g.rule(JavaGrammar.SREQU))
        .matches(">>=");

    assertThat(g.rule(JavaGrammar.STAR))
        .matches("*")
        .notMatches("*=");
    assertThat(g.rule(JavaGrammar.STAREQU))
        .matches("*=");
  }

}
