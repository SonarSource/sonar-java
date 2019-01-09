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
package org.sonar.java.ast.visitors;

import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class AccessorsUtilsTest {

  private final ActionParser p = JavaParser.createParser();

  @Test
  public void method_badly_named_is_not_accessor() {
    assertThat(isAccessor("class T { private int a; int foo() { return a; } }")).isFalse();
    assertThat(isAccessor("class T { private int a; int foo(int a) { this.a = a; } }")).isFalse();
  }

  @Test
  public void method_named_properly_is_getter() {
    assertThat(isAccessor("class T { private int a; public int getA() { return a; } }")).isTrue();
  }

  @Test
  public void method_named_properly_is_setter() {
    assertThat(isAccessor("class T { private int a; public void setA(int a) { this.a = a; } }")).isTrue();
  }

  @Test
  public void boolean_method_named_properly_is_getter() {
    assertThat(isAccessor("class T { private boolean a; public boolean isA() { return a; } }")).isTrue();
    assertThat(isAccessor("class T { private Boolean a; public Boolean isA() { return a; } }")).isTrue();
    assertThat(isAccessor("class T { private Boolean a; public java.lang.Boolean isA() { return a; } }")).isTrue();

    assertThat(isAccessor("class T { private Boolean a; public foo.Boolean isA() { return a; } }")).isFalse();
    assertThat(isAccessor("class T { private Boolean a; public org.foo.Boolean isA() { return a; } }")).isFalse();
    assertThat(isAccessor("class T { private Integer a; public java.lang.Integer isA() { return a; } }")).isFalse();
    assertThat(isAccessor("class T { private Boolean a; public org.lang.Boolean isA() { return a; } }")).isFalse();
    assertThat(isAccessor("class T { private Boolean a; public java.foo.Boolean isA() { return a; } }")).isFalse();
    assertThat(isAccessor("class T { private Boolean a; public Boolean<Integer> isA() { return a; } }")).isFalse();
    assertThat(isAccessor("class T { private Boolean a; public void isA() { return a; } }")).isFalse();
  }

  @Test
  public void boolean_method_named_properly_is_getter_if_return_type_is_boolean() {
    assertThat(isAccessor("class T { private  int a; int isA() { return a; } }")).isFalse();
  }

  @Test
  public void is_getter_if_has_one_return_statement() {
    assertThat(isAccessor("class T { private boolean a; public boolean isA() { a=!a;return a; } }")).isFalse();
    assertThat(isAccessor("class T { private int a; public int getA() { a++;return a; } }")).isFalse();
    assertThat(isAccessor("class T { private boolean a; public boolean isA() { return a; } }")).isTrue();
    assertThat(isAccessor("class T { private int a; public int getA() { return a; } }")).isTrue();
    assertThat(isAccessor("class T { private int a; public void getA() { a++; } }")).isFalse();
  }

  @Test
  public void constructor_is_not_accessor() {
    assertThat(isAccessor("class getA {private int a; public getA() {} }")).isFalse();
  }

  @Test
  public void getter_should_reference_private_property() {
    assertThat(isAccessor("class T { private boolean a; public boolean isA() { return true;} }")).isFalse();
    assertThat(isAccessor("class T { boolean a; public boolean isA() { return a;} }")).isFalse();
    assertThat(isAccessor("class T { private boolean a; public boolean isA() { return a;} }")).isTrue();
    assertThat(isAccessor("class T { private int a; public int getA() { return 1; } }")).isFalse();
    assertThat(isAccessor("class T { int a; public int getA() { return a; } }")).isFalse();
    assertThat(isAccessor("class T { int a; public void getA() { return; } }")).isFalse();
    assertThat(isAccessor("class T { private int a; public int getA() { return a; } }")).isTrue();
  }

  @Test
  public void getter_should_have_no_parameters() {
    assertThat(isAccessor("class T { private boolean a; public boolean isA(boolean b) { return a;} }")).isFalse();
    assertThat(isAccessor("class T { private int a; public int getA(int b) { return 1; } }")).isFalse();
  }

  @Test
  public void method_with_no_body_is_not_getter_nor_setter() {
    assertThat(isAccessor("interface T { boolean isA(); }")).isFalse();
  }

  @Test
  public void setter_should_have_one_parameter() {
    assertThat(isAccessor("class T { private int a; public void setA() { this.a = a; } }")).isFalse();
    assertThat(isAccessor("class T { private int a; public void setA(int a, int b) { this.a = a; } }")).isFalse();
  }

  @Test
  public void setter_should_have_void_return_type() {
    assertThat(isAccessor("class T { private int a; public int setA(int a) { return a; } }")).isFalse();
  }

  @Test
  public void setter_body_is_an_assignement_statement_referencing_private_var() {
    assertThat(isAccessor("class T { private int a; public void setA(int a) { a++; } }")).isFalse();
    assertThat(isAccessor("class T { private int a; public void setA(int a) { b=0; } }")).isFalse();
    assertThat(isAccessor("class T { private int a; public void setA(int a) { b = a; } }")).isFalse();
    // limitation
    assertThat(isAccessor("class T { private int a; public void setA(int a) { a = b; } }")).isTrue();
    assertThat(isAccessor("class T { private int a; public void setA(int a) { this.a = b; } }")).isTrue();
  }

  @Test
  public void getter_using_this_are_not_accessor() throws Exception {
    // FIXME : those getters should be considered as accessors
    assertThat(isAccessor("class T { private int a; public int getA() { return this.a; } }")).isFalse();
  }

  @Test
  public void accessor_should_be_public() {
    assertThat(isAccessor("class T { private int a; void setA(int a) { this.a=a; } }")).isFalse();
    assertThat(isAccessor("class T { private int a; public void setA(int a) { this.a=a; } }")).isTrue();
    assertThat(isAccessor("class T { private Boolean a; public void setA(Boolean a) { this.a=a; } }")).isTrue();
  }

  private boolean isAccessor(String code) {
    ClassTree classTree = parseClass(code);
    return AccessorsUtils.isAccessor(classTree, extractMethod(classTree));
  }

  private ClassTree parseClass(String code) {
    return extractClass((CompilationUnitTree) p.parse(code));
  }

  private ClassTree extractClass(CompilationUnitTree cut) {
    return (ClassTree) cut.types().get(0);
  }

  private MethodTree extractMethod(ClassTree classTree) {
    for (Tree tree : classTree.members()) {
      if (tree.is(Tree.Kind.METHOD) || tree.is(Tree.Kind.CONSTRUCTOR)) {
        return (MethodTree) tree;
      }
    }
    return null;
  }
}
