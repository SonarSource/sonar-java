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

public class StatementTest {

  @Test
  public void ok() {
    LexerlessGrammarBuilder b = JavaGrammar.createGrammarBuilder();

    b.rule(JavaGrammar.BLOCK).override(RuleMock.word(b, "block"));
    b.rule(JavaGrammar.ASSERT_STATEMENT).override(RuleMock.word(b, "emptyStatement"));
    b.rule(JavaGrammar.IF_STATEMENT).override(RuleMock.word(b, "labeledStatement"));
    b.rule(JavaGrammar.FOR_STATEMENT).override(RuleMock.word(b, "expressionStatement"));
    b.rule(JavaGrammar.WHILE_STATEMENT).override(RuleMock.word(b, "ifStatement"));
    b.rule(JavaGrammar.DO_STATEMENT).override(RuleMock.word(b, "assertStatement"));
    b.rule(JavaGrammar.TRY_STATEMENT).override(RuleMock.word(b, "switchStatement"));
    b.rule(JavaGrammar.SWITCH_STATEMENT).override(RuleMock.word(b, "whileStatement"));
    b.rule(JavaGrammar.SYNCHRONIZED_STATEMENT).override(RuleMock.word(b, "doStatement"));
    b.rule(JavaGrammar.RETURN_STATEMENT).override(RuleMock.word(b, "forStatement"));
    b.rule(JavaGrammar.THROW_STATEMENT).override(RuleMock.word(b, "breakStatement"));
    b.rule(JavaGrammar.BREAK_STATEMENT).override(RuleMock.word(b, "continueStatement"));
    b.rule(JavaGrammar.CONTINUE_STATEMENT).override(RuleMock.word(b, "returnStatement"));
    b.rule(JavaGrammar.LABELED_STATEMENT).override(RuleMock.word(b, "throwStatement"));
    b.rule(JavaGrammar.EXPRESSION_STATEMENT).override(RuleMock.word(b, "synchronizedStatement"));
    b.rule(JavaGrammar.EMPTY_STATEMENT).override(RuleMock.word(b, "tryStatement"));

    assertThat(b, JavaGrammar.STATEMENT)
      .matches("block")
      .matches("emptyStatement")
      .matches("labeledStatement")
      .matches("expressionStatement")
      .matches("ifStatement")
      .matches("assertStatement")
      .matches("switchStatement")
      .matches("whileStatement")
      .matches("doStatement")
      .matches("forStatement")
      .matches("breakStatement")
      .matches("continueStatement")
      .matches("returnStatement")
      .matches("throwStatement")
      .matches("synchronizedStatement")
      .matches("tryStatement");
  }

}
