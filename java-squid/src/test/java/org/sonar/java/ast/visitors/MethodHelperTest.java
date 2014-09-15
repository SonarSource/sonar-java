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
package org.sonar.java.ast.visitors;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodHelperTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void isConstructor() {
    assertThat(new MethodHelper(mock(AstNode.class)).isConstructor()).isFalse();

    AstNode constructor = mock(AstNode.class);
    when(constructor.is(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST)).thenReturn(true);
    assertThat(new MethodHelper(constructor).isConstructor()).isTrue();
  }

  @Test
  public void getName() {
    assertThat(new MethodHelper(parseMethod("class A { void foo() {} }")).getName().getTokenOriginalValue()).isEqualTo("foo");
    assertThat(new MethodHelper(parseMethod("class A { int bar() { return 0; } }")).getName().getTokenOriginalValue()).isEqualTo("bar");
    assertThat(new MethodHelper(parseMethod("@interface Foo { public boolean value(); }")).getName().getTokenOriginalValue()).isEqualTo("value");
  }

  private static AstNode parseMethod(String source) {
    AstNode node = parse(source);
    List<AstNode> descendants = node.getDescendants(
      JavaGrammar.METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_METHOD_DECLARATOR_REST,
      JavaGrammar.CONSTRUCTOR_DECLARATOR_REST,
      JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST,
      Kind.METHOD);
    Preconditions.checkState(descendants.size() == 1);
    return descendants.get(0);
  }

  private static AstNode parse(String source) {
    return JavaParser.createParser(Charsets.UTF_8, true).parse(source);
  }

}
