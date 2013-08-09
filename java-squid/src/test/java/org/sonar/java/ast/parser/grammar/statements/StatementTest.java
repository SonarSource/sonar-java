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
import org.sonar.sslr.parser.LexerlessGrammar;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class StatementTest {

  private final LexerlessGrammar g = JavaGrammar.createGrammar();

  @Test
  public void ok() {
    g.rule(JavaGrammar.BLOCK).mock();
    g.rule(JavaGrammar.ASSERT_STATEMENT).mock();
    g.rule(JavaGrammar.IF_STATEMENT).mock();
    g.rule(JavaGrammar.FOR_STATEMENT).mock();
    g.rule(JavaGrammar.WHILE_STATEMENT).mock();
    g.rule(JavaGrammar.DO_STATEMENT).mock();
    g.rule(JavaGrammar.TRY_STATEMENT).mock();
    g.rule(JavaGrammar.SWITCH_STATEMENT).mock();
    g.rule(JavaGrammar.SYNCHRONIZED_STATEMENT).mock();
    g.rule(JavaGrammar.RETURN_STATEMENT).mock();
    g.rule(JavaGrammar.THROW_STATEMENT).mock();
    g.rule(JavaGrammar.BREAK_STATEMENT).mock();
    g.rule(JavaGrammar.CONTINUE_STATEMENT).mock();
    g.rule(JavaGrammar.LABELED_STATEMENT).mock();
    g.rule(JavaGrammar.EXPRESSION_STATEMENT).mock();
    g.rule(JavaGrammar.EMPTY_STATEMENT).mock();

    assertThat(g.rule(JavaGrammar.STATEMENT))
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
