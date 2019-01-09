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
import org.sonar.java.ast.parser.JavaLexer;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class KeywordTest {

  /**
   * JLS7 3.9
   */
  @Test
  public void realLife() {
    assertThat(JavaLexer.KEYWORD)
      .matches("abstract")
      .matches("assert")
      .matches("boolean")
      .matches("break")
      .matches("byte")
      .matches("case")
      .matches("catch")
      .matches("char")
      .matches("class")
      // TODO According to JLS7 3.9 "const" is a keyword
      .notMatches("const")
      .matches("continue")
      .matches("default")
      .matches("do")
      .matches("double")
      .matches("else")
      .matches("enum")
      .matches("extends")
      .matches("final")
      .matches("finally")
      .matches("float")
      .matches("for")
      .matches("if")
      // TODO According to JLS7 3.9 "goto" is a keyword
      .notMatches("goto")
      .matches("implements")
      .matches("import")
      .matches("instanceof")
      .matches("int")
      .matches("interface")
      .matches("long")
      .matches("native")
      .matches("new")
      .matches("package")
      .matches("private")
      .matches("protected")
      .matches("public")
      .matches("return")
      .matches("short")
      .matches("static")
      .matches("strictfp")
      .matches("super")
      .matches("switch")
      .matches("synchronized")
      .matches("this")
      .matches("throw")
      .matches("throws")
      .matches("transient")
      .matches("try")
      .matches("void")
      .matches("volatile")
      .matches("while")
      // FIXME according to JLS7 3.9 "false", "true" and "null" are not keywords
      .matches("false")
      .matches("true")
      .matches("null");
  }

}
