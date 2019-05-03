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
package org.sonar.java.model.statement;

import org.junit.Test;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class ContinueStatementTreeImplTest {

  private static InternalSyntaxToken CONTINUE_TOKEN = createToken(JavaKeyword.CONTINUE.getValue());
  private static InternalSyntaxToken SEMICOLON_TOKEN = createToken(":");

  @Test
  public void test_no_label() {
    ContinueStatementTreeImpl continueStatementTree = new ContinueStatementTreeImpl(CONTINUE_TOKEN, null, SEMICOLON_TOKEN);

    assertThat(continueStatementTree.continueKeyword()).isEqualTo(CONTINUE_TOKEN);
    assertThat(continueStatementTree.kind()).isEqualTo(Tree.Kind.CONTINUE_STATEMENT);
    assertThat(continueStatementTree.semicolonToken()).isEqualTo(SEMICOLON_TOKEN);
    assertThat(continueStatementTree.label()).isNull();
    assertThat(continueStatementTree.children()).containsExactly(CONTINUE_TOKEN, SEMICOLON_TOKEN);
  }

  @Test
  public void test_with_label() {
    IdentifierTreeImpl label = new IdentifierTreeImpl(createToken("label"));
    ContinueStatementTreeImpl continueStatementTree = new ContinueStatementTreeImpl(CONTINUE_TOKEN, label, SEMICOLON_TOKEN);

    assertThat(continueStatementTree.continueKeyword()).isEqualTo(CONTINUE_TOKEN);
    assertThat(continueStatementTree.kind()).isEqualTo(Tree.Kind.CONTINUE_STATEMENT);
    assertThat(continueStatementTree.semicolonToken()).isEqualTo(SEMICOLON_TOKEN);
    assertThat(continueStatementTree.label()).isEqualTo(label);
    assertThat(continueStatementTree.children()).containsExactly(CONTINUE_TOKEN, label, SEMICOLON_TOKEN);
  }

  private static InternalSyntaxToken createToken(String value) {
    return new InternalSyntaxToken(1, 1, value, new ArrayList<>(), 0, 0, false);
  }
}
