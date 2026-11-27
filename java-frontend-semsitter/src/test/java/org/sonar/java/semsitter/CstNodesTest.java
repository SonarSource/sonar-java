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
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.KeywordSuper;
import org.sonar.java.model.KeywordThis;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonarsource.semsitter.wire.Node;
import org.sonarsource.semsitter.wire.Ref;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.semsitter.TranslationStateFixtures.inner;
import static org.sonar.java.semsitter.TranslationStateFixtures.leaf;

class CstNodesTest {

  @Test
  void first_problem_is_an_error_or_a_missing_node() {
    Node missing = leaf(";", "", 2, 4).toBuilder().setMissing(true).build();

    assertThat(CstNodes.firstProblem(node(inner("program", leaf("x", "x", 1, 0), missing)))).extracting(CstNode::line).isEqualTo(2);
    assertThat(CstNodes.firstProblem(node(inner("ERROR")))).extracting(CstNode::k).isEqualTo("ERROR");
    assertThat(CstNodes.firstProblem(node(inner("program", leaf("x", "x", 1, 0))))).isNull();
  }

  @Test
  void enum_separator_is_a_comma_or_starts_the_body_declarations() {
    Node comma = leaf(",", ",", 1, 1);
    Node semicolon = leaf(";", ";", 1, 3);
    List<CstNode> siblings = node(inner("enum_body", leaf("A", "A", 1, 0), comma, leaf("B", "B", 1, 2), inner("enum_body_declarations", semicolon), leaf("}", "}", 1, 4))).children();

    assertThat(CstNodes.enumSeparator(siblings, 0).proto).isSameAs(comma);
    assertThat(CstNodes.enumSeparator(siblings, 2).proto).isSameAs(semicolon);
    assertThat(CstNodes.enumSeparator(siblings, 3)).isNull();
  }

  @Test
  void dotted_name_of_a_qualified_name() {
    IdentifierTreeImpl java = identifier("java");
    MemberSelectExpressionTreeImpl javaUtil = new MemberSelectExpressionTreeImpl(java, token("."), identifier("util"));
    MethodInvocationTreeImpl call = new MethodInvocationTreeImpl(identifier("call"), null, ArgumentListTreeImpl.emptyList());

    assertThat(CstNodes.dottedName(javaUtil)).isEqualTo("java.util");
    assertThat(CstNodes.dottedName(new MemberSelectExpressionTreeImpl(call, token("."), identifier("x")))).isEmpty();
    assertThat(CstNodes.dottedName(call)).isEmpty();
  }

  @Test
  void receiver_keywords_are_this_and_super() {
    IdentifierTreeImpl name = identifier("name");

    assertThat(CstNodes.receiverKeyword(identifier("this"))).isInstanceOf(KeywordThis.class);
    assertThat(CstNodes.receiverKeyword(identifier("super"))).isInstanceOf(KeywordSuper.class);
    assertThat(CstNodes.receiverKeyword(name)).isSameAs(name);
  }

  @Test
  void modifier_keywords() {
    assertThat(CstNodes.modifierKeyword("strictfp")).isEqualTo(Modifier.STRICTFP);
    assertThat(CstNodes.modifierKeyword("non-sealed")).isEqualTo(Modifier.NON_SEALED);
    assertThatThrownBy(() -> CstNodes.modifierKeyword("transitive")).isInstanceOf(IllegalArgumentException.class).hasMessage("Not a modifier: transitive");
  }

  @Test
  void keyword_lookups_fall_back_to_the_node() {
    CstNode node = node(inner("block", leaf("{", "{", 1, 0), leaf("}", "}", 1, 1)));

    assertThat(CstNodes.lastKeyword(node, ";")).isSameAs(node);
    assertThat(CstNodes.required(node, ";")).isSameAs(node);
    assertThat(CstNodes.requiredField(node, "body", null)).isSameAs(node);
    assertThat(CstNodes.requiredField(node, "body", node.children().get(0)).text()).isEqualTo("{");
    assertThat(CstNodes.nameNode(node)).isNull();
    assertThat(CstNodes.closingAngle(node(inner("type_arguments", leaf("<", "<", 3, 1), leaf(">", ">", 3, 5)))))
      .extracting(InternalSyntaxToken::text, InternalSyntaxToken::line, InternalSyntaxToken::column).containsExactly(">", 3, 5);
  }

  @Test
  void same_needs_two_nodes_of_one_proto() {
    Node proto = leaf("x", "x", 1, 0);

    assertThat(CstNodes.same(node(proto), node(proto))).isTrue();
    assertThat(CstNodes.same(node(proto), node(leaf("x", "x", 1, 0)))).isFalse();
    assertThat(CstNodes.same(null, node(proto))).isFalse();
    assertThat(CstNodes.same(node(proto), null)).isFalse();
  }

  @Test
  void first_named_skips_punctuation() {
    Node identifier = leaf("identifier", "a", 1, 1);
    Node keyword = leaf("int", "int", 1, 0);

    assertThat(CstNodes.firstNamed(node(inner("x", leaf("(", "(", 1, 0), identifier))).proto).isSameAs(identifier);
    assertThat(CstNodes.firstNamed(node(inner("x", keyword))).proto).isSameAs(keyword);
    assertThat(CstNodes.firstNamed(node(inner("x", leaf("+", "-", 1, 0), leaf("type_identifier", "T", 1, 1)))).text()).isEqualTo("T");
    CstNode empty = node(inner("x"));
    assertThat(CstNodes.firstNamed(empty)).isSameAs(empty);
  }

  @Test
  void only_bound_references_with_a_target_are_targets() {
    Node.Builder name = leaf("identifier", "a", 1, 0).toBuilder()
      .addRefs(Ref.newBuilder().setAccess("r").setOutcome("bound"))
      .addRefs(Ref.newBuilder().setAccess("r").setOutcome("ambiguous").setTarget("A#a"))
      .addRefs(Ref.newBuilder().setAccess("ref").setOutcome("bound").setTarget("A"));

    assertThat(node(name.build()).valueTarget()).isNull();
    assertThat(node(name.build()).typeTarget()).isEqualTo("A");
  }

  private static CstNode node(Node proto) {
    return new CstNode(proto);
  }

  private static IdentifierTreeImpl identifier(String name) {
    return new IdentifierTreeImpl(token(name));
  }

  private static InternalSyntaxToken token(String text) {
    return new InternalSyntaxToken(1, 0, text, List.of(), false);
  }
}
