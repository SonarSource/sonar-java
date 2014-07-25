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
package org.sonar.java.ast.parser.grammar.statements;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.parser.grammar.RuleMock;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class VariableDeclaratorTest {

  @Test
  public void ok() {
    LexerlessGrammarBuilder b = JavaGrammar.createGrammarBuilder();

    b.rule(JavaGrammar.DIM).override(RuleMock.word(b, "dim"));
    b.rule(JavaGrammar.VARIABLE_INITIALIZER).override(RuleMock.word(b, "variableInitializer"));

    assertThat(b, JavaGrammar.VARIABLE_DECLARATOR)
      .matches("identifier dim dim = variableInitializer")
      .matches("identifier dim dim")
      .matches("identifier dim = variableInitializer")
      .matches("identifier dim")
      .matches("identifier = variableInitializer")
      .matches("identifier");
  }

  @Test
  public void ko() {
    assertThat(JavaGrammar.VARIABLE_DECLARATOR)
      .notMatches("");
  }

}
