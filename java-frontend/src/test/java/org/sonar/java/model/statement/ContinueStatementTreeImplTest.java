/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.statement;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

class ContinueStatementTreeImplTest {

  private static final InternalSyntaxToken CONTINUE_TOKEN = createToken(JavaKeyword.CONTINUE.getValue());
  private static final InternalSyntaxToken SEMICOLON_TOKEN = createToken(":");

  @Test
  void test_no_label() {
    ContinueStatementTreeImpl continueStatementTree = new ContinueStatementTreeImpl(CONTINUE_TOKEN, null, SEMICOLON_TOKEN);

    assertThat(continueStatementTree.continueKeyword()).isEqualTo(CONTINUE_TOKEN);
    assertThat(continueStatementTree.kind()).isEqualTo(Tree.Kind.CONTINUE_STATEMENT);
    assertThat(continueStatementTree.semicolonToken()).isEqualTo(SEMICOLON_TOKEN);
    assertThat(continueStatementTree.label()).isNull();
    assertThat(continueStatementTree.children()).containsExactly(CONTINUE_TOKEN, SEMICOLON_TOKEN);
  }

  @Test
  void test_with_label() {
    IdentifierTreeImpl label = new IdentifierTreeImpl(createToken("label"));
    ContinueStatementTreeImpl continueStatementTree = new ContinueStatementTreeImpl(CONTINUE_TOKEN, label, SEMICOLON_TOKEN);

    assertThat(continueStatementTree.continueKeyword()).isEqualTo(CONTINUE_TOKEN);
    assertThat(continueStatementTree.kind()).isEqualTo(Tree.Kind.CONTINUE_STATEMENT);
    assertThat(continueStatementTree.semicolonToken()).isEqualTo(SEMICOLON_TOKEN);
    assertThat(continueStatementTree.label()).isEqualTo(label);
    assertThat(continueStatementTree.children()).containsExactly(CONTINUE_TOKEN, label, SEMICOLON_TOKEN);
  }

  private static InternalSyntaxToken createToken(String value) {
    return new InternalSyntaxToken(1, 1, value, new ArrayList<>(), false);
  }
}
