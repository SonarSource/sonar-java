/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonarsource.semsitter.wire.Node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.semsitter.TranslationStateFixtures.inner;
import static org.sonar.java.semsitter.TranslationStateFixtures.leaf;

class TranslatorPartTest {

  private final TranslationState state = new TranslationState(new JSema());
  private final TranslatorPart part = state.parts.names;

  @Test
  void token_of_an_unindexed_node_is_its_first_leaf_or_its_text() {
    Node semicolon = leaf(";", ";", 1, 9);
    state.tokens.put(semicolon, new InternalSyntaxToken(1, 9, ";", List.of(), false));

    assertThat(part.token(new CstNode(inner("statement", inner("x", leaf("y", "", 1, 0)), semicolon))).text()).isEqualTo(";");
    assertThat(part.token(new CstNode(inner("x", inner("y")))).text()).isEqualTo("x");
    assertThat(part.token(new CstNode(leaf("word", "word", 2, 3)))).extracting(InternalSyntaxToken::text, InternalSyntaxToken::line, InternalSyntaxToken::column)
      .containsExactly("word", 2, 3);
    assertThat(part.token(new CstNode(inner("null_literal"))).text()).isEmpty();
  }

  @Test
  void syntactic_type_of_a_type_the_scan_did_not_bind_is_unknown() {
    assertThat(part.syntacticType(new CstNode(leaf("integral_type", "int", 1, 0)))).isSameAs(Type.UNKNOWN);
    assertThat(part.syntacticType(new CstNode(leaf("type_identifier", "Missing", 1, 0)))).isSameAs(Type.UNKNOWN);
    assertThat(part.syntacticType(new CstNode(inner("generic_type", leaf("type_identifier", "Missing", 1, 0), inner("type_arguments"))))).isSameAs(Type.UNKNOWN);
  }

  @Test
  void a_declaration_without_a_name_binds_nothing() {
    IdentifierTreeImpl identifier = new IdentifierTreeImpl(new InternalSyntaxToken(1, 0, "x", List.of(), false));

    part.bindDeclaration(identifier, null, identifier);

    assertThat(identifier.symbol()).isSameAs(Symbol.UNKNOWN_SYMBOL);
  }
}
