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
package org.sonar.java.ast.parser.grammar.enums;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.parser.grammar.RuleMock;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class EnumBodyTest {

  private final LexerlessGrammarBuilder b = JavaGrammar.createGrammarBuilder();

  @Test
  public void ok() {
    b.rule(JavaGrammar.ENUM_CONSTANTS).override(RuleMock.word(b, "enumConstants"));
    b.rule(JavaGrammar.ENUM_BODY_DECLARATIONS).override(RuleMock.word(b, "enumBodyDeclarations"));

    assertThat(b, JavaGrammar.ENUM_BODY)
      .matches("{ }")
      .matches("{ enumConstants }")
      .matches("{ enumBodyDeclarations }")
      .matches("{ enumConstants enumBodyDeclarations }")
      .matches("{ enumConstants , enumBodyDeclarations }");
  }

}
