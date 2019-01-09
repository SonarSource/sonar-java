/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.ast.parser.grammar;

import org.junit.Test;
import org.sonar.java.ast.api.JavaPunctuator;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class CornerCasesTest {

  @Test
  public void test() {
    assertThat(JavaPunctuator.AND)
      .matches("&")
      .notMatches("&&")
      .notMatches("&=");
    assertThat(JavaPunctuator.ANDAND)
      .matches("&&");
    assertThat(JavaPunctuator.ANDEQU)
      .matches("&=");

    assertThat(JavaPunctuator.BANG)
      .matches("!")
      .notMatches("!=");
    assertThat(JavaPunctuator.NOTEQUAL)
      .matches("!=");

    assertThat(JavaPunctuator.BSR)
      .matches(">>>")
      .notMatches(">>>=");
    assertThat(JavaPunctuator.BSREQU)
      .matches(">>>=");

    assertThat(JavaPunctuator.DIV)
      .matches("/")
      .notMatches("/=");
    assertThat(JavaPunctuator.DIVEQU)
      .matches("/=");

    assertThat(JavaPunctuator.EQU)
      .matches("=")
      .notMatches("==");
    assertThat(JavaPunctuator.EQUAL)
      .matches("==");

    assertThat(JavaPunctuator.GT)
      .matches(">")
      .notMatches(">=")
      .notMatches(">>");
    assertThat(JavaPunctuator.GE)
      .matches(">=");

    assertThat(JavaPunctuator.HAT)
      .matches("^")
      .notMatches("^=");
    assertThat(JavaPunctuator.HATEQU)
      .matches("^=");

    assertThat(JavaPunctuator.LT)
      .matches("<")
      .notMatches("<=")
      .notMatches("<<");
    assertThat(JavaPunctuator.LE)
      .matches("<=");

    assertThat(JavaPunctuator.MINUS)
      .matches("-")
      .notMatches("-=")
      .notMatches("--");
    assertThat(JavaPunctuator.MINUSEQU)
      .matches("-=");
    assertThat(JavaPunctuator.DEC)
      .matches("--");

    assertThat(JavaPunctuator.MOD)
      .matches("%")
      .notMatches("%=");
    assertThat(JavaPunctuator.MODEQU)
      .matches("%=");

    assertThat(JavaPunctuator.OR)
      .matches("|")
      .notMatches("|=")
      .notMatches("||");
    assertThat(JavaPunctuator.OREQU)
      .matches("|=");
    assertThat(JavaPunctuator.OROR)
      .matches("||");

    assertThat(JavaPunctuator.PLUS)
      .matches("+")
      .notMatches("+=")
      .notMatches("++");
    assertThat(JavaPunctuator.PLUSEQU)
      .matches("+=");
    assertThat(JavaPunctuator.INC)
      .matches("++");

    assertThat(JavaPunctuator.SL)
      .matches("<<")
      .notMatches("<<=");
    assertThat(JavaPunctuator.SLEQU)
      .matches("<<=");

    assertThat(JavaPunctuator.SR)
      .matches(">>")
      .notMatches(">>=");
    assertThat(JavaPunctuator.SREQU)
      .matches(">>=");

    assertThat(JavaPunctuator.STAR)
      .matches("*")
      .notMatches("*=");
    assertThat(JavaPunctuator.STAREQU)
      .matches("*=");
  }

}
