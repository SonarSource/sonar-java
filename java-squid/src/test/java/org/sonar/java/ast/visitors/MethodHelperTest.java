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
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodHelperTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void subscribeTo() {
    SquidAstVisitor<LexerlessGrammar> visitor = new JavaAstVisitor() {};
    MethodHelper.subscribe(visitor);
    assertThat(visitor.getAstNodeTypesToVisit()).containsExactly(
      JavaGrammar.METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_METHOD_DECLARATOR_REST,
      JavaGrammar.CONSTRUCTOR_DECLARATOR_REST,
      JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST,
      JavaGrammar.ANNOTATION_METHOD_REST);
  }

  @Test
  public void isPublic() {
    assertThat(new MethodHelper(parseMethod("class A { public void f() {} }")).isPublic()).isTrue();
    assertThat(new MethodHelper(parseMethod("class A { public int f() {} }")).isPublic()).isTrue();
    assertThat(new MethodHelper(parseMethod("class A { public <T> void f(T a) {} }")).isPublic()).isTrue();
    assertThat(new MethodHelper(parseMethod("class A { public A() {} }")).isPublic()).isTrue();
    assertThat(new MethodHelper(parseMethod("class A { private void f() {} }")).isPublic()).isFalse();
    assertThat(new MethodHelper(parseMethod("class A { private int f() {} }")).isPublic()).isFalse();
    assertThat(new MethodHelper(parseMethod("class A { private <T> void f(T a) {} }")).isPublic()).isFalse();
    assertThat(new MethodHelper(parseMethod("class A { private A() {} }")).isPublic()).isFalse();

    assertThat(new MethodHelper(parseMethod("interface A { public void f(); }")).isPublic()).isTrue();
    assertThat(new MethodHelper(parseMethod("interface A { public int f(); }")).isPublic()).isTrue();
    assertThat(new MethodHelper(parseMethod("interface A { public <T> int f(T a); }")).isPublic()).isTrue();
    assertThat(new MethodHelper(parseMethod("interface A { private void f(); }")).isPublic()).isFalse();
    assertThat(new MethodHelper(parseMethod("interface A { private int f(); }")).isPublic()).isFalse();
    assertThat(new MethodHelper(parseMethod("interface A { private <T> int f(T a); }")).isPublic()).isFalse();

    assertThat(new MethodHelper(parseMethod("@interface Foo { public boolean value(); }")).isPublic()).isTrue();
    assertThat(new MethodHelper(parseMethod("@interface Foo { private boolean value(); }")).isPublic()).isFalse();
  }

  @Test
  public void isPublic_should_fail_when_invalid_type_given() {
    thrown.expect(IllegalStateException.class);
    new MethodHelper(mock(AstNode.class)).isPublic();
  }

  @Test
  public void isConstructor() {
    assertThat(new MethodHelper(mock(AstNode.class)).isConstructor()).isFalse();

    AstNode constructor = mock(AstNode.class);
    when(constructor.is(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST)).thenReturn(true);
    assertThat(new MethodHelper(constructor).isConstructor()).isTrue();
  }

  @Test
  public void getReturnType() {
    assertThat(new MethodHelper(parseMethod("class A { void f() {} }")).getReturnType().getTokenOriginalValue()).isEqualTo("void");
    assertThat(new MethodHelper(parseMethod("class A { int f() { return 0; } }")).getReturnType().getTokenOriginalValue()).isEqualTo("int");
  }

  @Test
  public void getName() {
    assertThat(new MethodHelper(parseMethod("class A { void foo() {} }")).getName().getTokenOriginalValue()).isEqualTo("foo");
    assertThat(new MethodHelper(parseMethod("class A { int bar() { return 0; } }")).getName().getTokenOriginalValue()).isEqualTo("bar");
    assertThat(new MethodHelper(parseMethod("@interface Foo { public boolean value(); }")).getName().getTokenOriginalValue()).isEqualTo("value");
  }

  @Test
  public void parameters() {
    assertThat(new MethodHelper(parseMethod("class A { void foo() {} }")).hasParameters()).isFalse();
    assertThat(new MethodHelper(parseMethod("class A { void foo() {} }")).getParameters()).hasSize(0);

    assertThat(new MethodHelper(parseMethod("class A { void foo(int a, int b) {} }")).hasParameters()).isTrue();
    assertThat(new MethodHelper(parseMethod("class A { void foo(int a, int b) {} }")).getParameters()).hasSize(2);

    assertThat(new MethodHelper(parseMethod("class A { <T> A(T a) {} }")).hasParameters()).isTrue();
    assertThat(new MethodHelper(parseMethod("class A { <T> A(T a) {} }")).getParameters()).hasSize(1);

    assertThat(new MethodHelper(parseMethod("@interface Foo { public boolean value(); }")).hasParameters()).isFalse();
    assertThat(new MethodHelper(parseMethod("@interface Foo { public boolean value(); }")).getParameters()).hasSize(0);
  }

  @Test
  public void getStatements() {
    assertThat(new MethodHelper(parseMethod("class A { void foo() {} }")).getStatements()).hasSize(0);
    assertThat(new MethodHelper(parseMethod("class A { void foo() { int a; int b; } }")).getStatements()).hasSize(2);
    assertThat(new MethodHelper(parseMethod("interface A { void foo(); }")).getStatements()).hasSize(0);
  }

  @Test
  public void getMethods() {
    assertThat(MethodHelper.getMethods(parse("class A {}").getFirstDescendant(JavaGrammar.CLASS_BODY))).hasSize(0);
    assertThat(MethodHelper.getMethods(parse("class A { ; }").getFirstDescendant(JavaGrammar.CLASS_BODY))).hasSize(0);
    assertThat(MethodHelper.getMethods(parse("class A { A() {} }").getFirstDescendant(JavaGrammar.CLASS_BODY))).hasSize(0);
    assertThat(MethodHelper.getMethods(parse("class A { void f() {} }").getFirstDescendant(JavaGrammar.CLASS_BODY))).hasSize(1);
    assertThat(MethodHelper.getMethods(parse("class A { int f() { return 0; } }").getFirstDescendant(JavaGrammar.CLASS_BODY))).hasSize(1);
    assertThat(MethodHelper.getMethods(parse("class A { <T> void f(T a) {} }").getFirstDescendant(JavaGrammar.CLASS_BODY))).hasSize(1);
    assertThat(MethodHelper.getMethods(parse("class A { void f() {} int g() { return 0; } }").getFirstDescendant(JavaGrammar.CLASS_BODY))).hasSize(2);

    assertThat(MethodHelper.getMethods(parse("enum A { ; void f() {} int g() { return 0; } }").getFirstDescendant(JavaGrammar.ENUM_BODY_DECLARATIONS))).hasSize(2);
  }

  private static AstNode parseMethod(String source) {
    AstNode node = parse(source);
    List<AstNode> descendants = node.getDescendants(
      JavaGrammar.METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_METHOD_DECLARATOR_REST,
      JavaGrammar.CONSTRUCTOR_DECLARATOR_REST,
      JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST,
      JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST,
      JavaGrammar.ANNOTATION_METHOD_REST);
    Preconditions.checkState(descendants.size() == 1);
    return descendants.get(0);
  }

  private static AstNode parse(String source) {
    return new ParserAdapter(Charsets.UTF_8, JavaGrammar.createGrammar()).parse(source);
  }

}
