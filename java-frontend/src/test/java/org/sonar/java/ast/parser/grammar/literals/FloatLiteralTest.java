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

public class FloatLiteralTest {

  @Test
  public void ok() {
    assertThat(JavaTokenType.DOUBLE_LITERAL)
      // Decimal

      // with dot at the end
      .matches("1234.")
      .matches("1234.E1")
      .matches("1234.e+1")
      .matches("1234.E-1");
    assertThat(JavaTokenType.FLOAT_LITERAL)
      .matches("1234.f");

    // with dot between
    assertThat(JavaTokenType.DOUBLE_LITERAL)
      .matches("12.34")
      .matches("12.34E1")
      .matches("12.34e+1")
      .matches("12.34E-1");
    assertThat(JavaTokenType.FLOAT_LITERAL)
      .matches("12.34f")
      .matches("12.34E1F");
    assertThat(JavaTokenType.DOUBLE_LITERAL)
      .matches("12.34E+1d")
      .matches("12.34e-1D")

      // with dot at the beginning
      .matches(".1234")
      .matches(".1234e1")
      .matches(".1234E+1")
      .matches(".1234E-1");
    assertThat(JavaTokenType.FLOAT_LITERAL)
      .matches(".1234f")
      .matches(".1234E1F");
    assertThat(JavaTokenType.DOUBLE_LITERAL)
      .matches(".1234e+1d")
      .matches(".1234E-1D")

      // without dot
      .matches("1234D")
      .matches("1234d");
    assertThat(JavaTokenType.FLOAT_LITERAL)
      .matches("1234F")
      .matches("1234f");
    assertThat(JavaTokenType.DOUBLE_LITERAL)
      .matches("1234e1")
      .matches("1234E+1")
      .matches("1234E-1");
    assertThat(JavaTokenType.FLOAT_LITERAL)
      .matches("1234E1f");
    assertThat(JavaTokenType.DOUBLE_LITERAL)
      .matches("1234e+1d")
      .matches("1234E-1D")

      // Hexadecimal

      // with dot at the end
      .matches("0XAF.P1")
      .matches("0xAF.p+1")
      .matches("0XAF.p-1")

      // with dot between
      .matches("0xAF.BCP1")
      .matches("0XAF.BCp+1")
      .matches("0xAF.BCP-1");

    assertThat(JavaTokenType.FLOAT_LITERAL)
      .matches("0xAF.BCp1F");
    assertThat(JavaTokenType.DOUBLE_LITERAL)
      .matches("0XAF.BCP+1d")
      .matches("0XAF.BCp-1D")

      // without dot
      .matches("0xAFp1")
      .matches("0XAFp+1")
      .matches("0xAFp-1");

    assertThat(JavaTokenType.FLOAT_LITERAL)
      .matches("0XAFp1f");
    assertThat(JavaTokenType.DOUBLE_LITERAL)
      .matches("0xAFp+1d")
      .matches("0XAFp-1D");
  }

  @Test
  public void nok() {
    assertThat(JavaTokenType.DOUBLE_LITERAL)
      .notMatches("0xAF.f")
      .notMatches("0xAF.BCf")
      .notMatches("0xAF.")
      .notMatches("0XAF.BC")
      .notMatches("0xAF");
    assertThat(JavaTokenType.FLOAT_LITERAL)
      .notMatches("0xAF.f")
      .notMatches("0xAF.BCf")
      .notMatches("0xAF.")
      .notMatches("0XAF.BC")
      .notMatches("0xAF");
  }

}
