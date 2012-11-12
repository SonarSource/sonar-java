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
package org.sonar.java.ast.parser.grammar.literals;

import org.junit.Test;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.parser.JavaGrammarImpl;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class IntegerLiteralTest {

  JavaGrammar g = new JavaGrammarImpl();

  @Test
  public void ok() {
    // Decimal
    assertThat(g.integerLiteral)
        .matches("0")
        .matches("543");
    assertThat(g.longLiteral)
        .matches("543l")
        .matches("543L");

    // Hexadecimal
    assertThat(g.integerLiteral)
        .matches("0xFF");
    assertThat(g.longLiteral)
        .matches("0xFFl")
        .matches("0xFFL");

    assertThat(g.integerLiteral)
        .matches("0XFF");
    assertThat(g.longLiteral)
        .matches("0XFFl")
        .matches("0XFFL");

    // Octal
    assertThat(g.integerLiteral)
        .matches("077");
    assertThat(g.longLiteral)
        .matches("077l")
        .matches("077L");

    // Binary (new in Java 7)
    assertThat(g.integerLiteral)
        .matches("0b1010");
    assertThat(g.longLiteral)
        .matches("0b1010l")
        .matches("0b1010L");

    assertThat(g.integerLiteral)
        .matches("0B1010");
    assertThat(g.longLiteral)
        .matches("0B1010l")
        .matches("0B1010L");

    // Underscore (new in Java 7)
    assertThat(g.integerLiteral)
        .matches("1_000_000")
        .matches("5_______2");

    assertThat(g.longLiteral)
        .matches("0x7fff_ffff_ffff_ffffL");
  }

  @Test
  public void nok() {
    assertThat(g.integerLiteral)
        .notMatches("0.1")
        .notMatches("_0")
        .notMatches("_d")
        .notMatches("._list");
    assertThat(g.longLiteral)
        .notMatches("0.1")
        .notMatches("_0")
        .notMatches("_d")
        .notMatches("._list");
  }

}
