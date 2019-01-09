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
package org.sonar.java.ast.parser.grammar.literals;

import org.junit.Test;
import org.sonar.java.ast.api.JavaTokenType;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class IntegerLiteralTest {

  @Test
  public void ok() {
    // Decimal
    assertThat(JavaTokenType.INTEGER_LITERAL)
      .matches("0")
      .matches("543");
    assertThat(JavaTokenType.LONG_LITERAL)
      .matches("543l")
      .matches("543L");

    // Hexadecimal
    assertThat(JavaTokenType.INTEGER_LITERAL)
      .matches("0xFF");
    assertThat(JavaTokenType.LONG_LITERAL)
      .matches("0xFFl")
      .matches("0xFFL");

    assertThat(JavaTokenType.INTEGER_LITERAL)
      .matches("0XFF");
    assertThat(JavaTokenType.LONG_LITERAL)
      .matches("0XFFl")
      .matches("0XFFL");

    // Octal
    assertThat(JavaTokenType.INTEGER_LITERAL)
      .matches("077");
    assertThat(JavaTokenType.LONG_LITERAL)
      .matches("077l")
      .matches("077L");

    // Binary (new in Java 7)
    assertThat(JavaTokenType.INTEGER_LITERAL)
      .matches("0b1010");
    assertThat(JavaTokenType.LONG_LITERAL)
      .matches("0b1010l")
      .matches("0b1010L");

    assertThat(JavaTokenType.INTEGER_LITERAL)
      .matches("0B1010");
    assertThat(JavaTokenType.LONG_LITERAL)
      .matches("0B1010l")
      .matches("0B1010L");

    // Underscore (new in Java 7)
    assertThat(JavaTokenType.INTEGER_LITERAL)
      .matches("1_000_000")
      .matches("5_______2");

    assertThat(JavaTokenType.LONG_LITERAL)
      .matches("0x7fff_ffff_ffff_ffffL");
  }

  @Test
  public void nok() {
    assertThat(JavaTokenType.INTEGER_LITERAL)
      .notMatches("0.1")
      .notMatches("_0")
      .notMatches("_d")
      .notMatches("._list");
    assertThat(JavaTokenType.LONG_LITERAL)
      .notMatches("0.1")
      .notMatches("_0")
      .notMatches("_d")
      .notMatches("._list");
  }

}
