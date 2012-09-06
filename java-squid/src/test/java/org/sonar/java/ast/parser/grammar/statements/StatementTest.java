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
package org.sonar.java.ast.parser.grammar.statements;

import com.sonar.sslr.impl.Parser;
import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.parser.JavaParser;

import static com.sonar.sslr.test.parser.ParserMatchers.parse;
import static org.junit.Assert.assertThat;

public class StatementTest {

  Parser<JavaGrammar> p = JavaParser.create();
  JavaGrammar g = p.getGrammar();

  @Before
  public void init() {
    p.setRootRule(g.statement);
  }

  @Test
  public void ok() {
    g.block.mock();
    g.assertStatement.mock();
    g.ifStatement.mock();
    g.forStatement.mock();
    g.whileStatement.mock();
    g.doStatement.mock();
    g.tryStatement.mock();
    g.switchStatement.mock();
    g.synchronizedStatement.mock();
    g.returnStatement.mock();
    g.throwStatement.mock();
    g.breakStatement.mock();
    g.continueStatement.mock();
    g.labeledStatement.mock();
    g.expressionStatement.mock();
    g.emptyStatement.mock();

    assertThat(p, parse("block"));
    assertThat(p, parse("emptyStatement"));
    assertThat(p, parse("labeledStatement"));
    assertThat(p, parse("expressionStatement"));
    assertThat(p, parse("ifStatement"));
    assertThat(p, parse("assertStatement"));
    assertThat(p, parse("switchStatement"));
    assertThat(p, parse("whileStatement"));
    assertThat(p, parse("doStatement"));
    assertThat(p, parse("forStatement"));
    assertThat(p, parse("breakStatement"));
    assertThat(p, parse("continueStatement"));
    assertThat(p, parse("returnStatement"));
    assertThat(p, parse("throwStatement"));
    assertThat(p, parse("synchronizedStatement"));
    assertThat(p, parse("tryStatement"));
  }

}
