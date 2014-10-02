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
import org.junit.Test;

import java.lang.reflect.Field;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AstNodeReflectorTest {

  private final AstNodeType ASTNODE_TYPE = mock(AstNodeType.class);

  @Test
  public void setToken() {
    Token token = mock(Token.class);

    AstNode astNode = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);
    assertThat(astNode.hasToken()).isFalse();
    assertThat(astNode.getToken()).isNull();

    AstNodeReflector.setToken(astNode, token);
    assertThat(astNode.hasToken()).isTrue();
    assertThat(astNode.getToken()).isSameAs(token);
  }

  @Test
  public void setChildIndex() throws Exception {
    AstNode astNode = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);

    AstNodeReflector.setChildIndex(astNode, 42);
    assertThat(getChildIndex(astNode)).isEqualTo(42);
  }

  @Test
  public void setParent() {
    AstNode parentAstNode = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);

    AstNode astNode = new AstNode(ASTNODE_TYPE, ASTNODE_TYPE.toString(), null);
    assertThat(astNode.getParent()).isNull();

    AstNodeReflector.setParent(astNode, parentAstNode);
    assertThat(astNode.getParent()).isSameAs(parentAstNode);
  }

  private static int getChildIndex(AstNode astNode) throws Exception {
    Field childIndexField = AstNode.class.getDeclaredField("childIndex");
    childIndexField.setAccessible(true);
    return childIndexField.getInt(astNode);
  }

}
