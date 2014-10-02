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
package org.sonar.java.ast.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AstNodeSanitizerTest {

  private static final TokenType TOKEN_TYPE = mock(TokenType.class);
  private static final AstNodeType ASTNODE_TYPE = mock(AstNodeType.class);

  @Test
  public void should_not_update_token_nodes() {
    Token token = mock(Token.class);
    when(token.getType()).thenReturn(TOKEN_TYPE);
    AstNode astNode = new AstNode(token);
    astNode.setFromIndex(42);
    astNode.setToIndex(84);

    new AstNodeSanitizer().sanitize(astNode);

    assertThat(astNode.getToken()).isSameAs(token);
    assertThat(astNode.getFromIndex()).isEqualTo(42);
    assertThat(astNode.getToIndex()).isEqualTo(84);
  }

  @Test
  public void should_update_empty_nodes() {
    AstNode astNode = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);
    astNode.setFromIndex(-84);
    astNode.setToIndex(-42);

    new AstNodeSanitizer().sanitize(astNode);

    assertThat(astNode.hasToken()).isFalse();
    assertThat(astNode.getFromIndex()).isEqualTo(0);
    assertThat(astNode.getToIndex()).isEqualTo(0);
  }

  @Test
  public void should_update_nodes_with_a_child() {
    Token token = mock(Token.class);
    when(token.getType()).thenReturn(TOKEN_TYPE);

    AstNode astNode1 = new AstNode(token);
    astNode1.setFromIndex(1);
    astNode1.setToIndex(2);

    AstNode astNode = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);
    astNode.addChild(astNode1);

    new AstNodeSanitizer().sanitize(astNode);

    assertThat(astNode.getToken()).isSameAs(token);
    assertThat(astNode.getFromIndex()).isEqualTo(1);
    assertThat(astNode.getToIndex()).isEqualTo(2);
  }

  @Test
  public void should_update_nodes_with_children() {
    Token token1 = mock(Token.class);
    when(token1.getType()).thenReturn(TOKEN_TYPE);
    Token token2 = mock(Token.class);
    when(token2.getType()).thenReturn(TOKEN_TYPE);

    AstNode astNode1 = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);
    astNode1.setFromIndex(0);
    astNode1.setToIndex(1);

    AstNode astNode2 = new AstNode(token1);
    astNode2.setFromIndex(2);
    astNode2.setToIndex(3);

    AstNode astNode3 = new AstNode(token2);
    astNode3.setFromIndex(4);
    astNode3.setToIndex(5);

    AstNode astNode = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);
    astNode.addChild(astNode1);
    astNode.addChild(astNode2);
    astNode.addChild(astNode3);

    new AstNodeSanitizer().sanitize(astNode);

    assertThat(astNode.getToken()).isSameAs(token1);
    assertThat(astNode.getFromIndex()).isEqualTo(0);
    assertThat(astNode.getToIndex()).isEqualTo(5);
  }

  @Test
  public void should_sanitize_nested_nodes() {
    Token token = mock(Token.class);
    when(token.getType()).thenReturn(TOKEN_TYPE);

    AstNode astNode1 = new AstNode(token);
    astNode1.setFromIndex(21);
    astNode1.setToIndex(42);

    AstNode astNode2 = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);

    AstNode astNode = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);
    astNode.addChild(astNode1);
    astNode.addChild(astNode2);

    new AstNodeSanitizer().sanitize(astNode);

    assertThat(astNode.getToken()).isSameAs(token);
    assertThat(astNode.getFromIndex()).isEqualTo(21);
    assertThat(astNode.getToIndex()).isEqualTo(42);

    assertThat(astNode2.hasToken()).isFalse();
    assertThat(astNode2.getFromIndex()).isEqualTo(42);
    assertThat(astNode2.getToIndex()).isEqualTo(42);
  }

  @Test
  public void should_not_be_affected_by_previous_calls() {
    Token token = mock(Token.class);
    when(token.getType()).thenReturn(TOKEN_TYPE);
    AstNode astNode = new AstNode(token);
    astNode.setFromIndex(42);
    astNode.setToIndex(84);

    AstNodeSanitizer sanitizer = new AstNodeSanitizer();
    sanitizer.sanitize(astNode);

    astNode = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);

    sanitizer.sanitize(astNode);

    assertThat(astNode.hasToken()).isFalse();
    assertThat(astNode.getFromIndex()).isEqualTo(0);
    assertThat(astNode.getToIndex()).isEqualTo(0);
  }

}
