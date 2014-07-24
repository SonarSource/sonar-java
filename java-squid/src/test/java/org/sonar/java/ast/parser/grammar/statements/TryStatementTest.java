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

public class TryStatementTest {

  private final LexerlessGrammarBuilder b = JavaGrammar.createGrammarBuilder();

  @Test
  public void ok() {
    b.rule(JavaGrammar.BLOCK).override(RuleMock.word(b, "block"));
    b.rule(JavaGrammar.CATCH_CLAUSE).override(RuleMock.word(b, "catchClause"));
    b.rule(JavaGrammar.FINALLY_).override(RuleMock.word(b, "finally_"));

    assertThat(b, JavaGrammar.STATEMENT)
      .matches("try block catchClause catchClause finally_")
      .matches("try block catchClause finally_")
      .matches("try block finally_");
  }

  @Test
  public void realLife() {
    // Java 7: multi-catch
    assertThat(b, JavaGrammar.STATEMENT)
      .matches("try {} catch (ClassNotFoundException | IllegalAccessException ex) {}");
    // Java 7: try-with-resources
    assertThat(b, JavaGrammar.STATEMENT)
      .matches("try (Resource resource = new Resource()) {}")
      .matches("try (Resource resource = new Resource()) {} catch (Expception e) {} finally {}");
  }

}
