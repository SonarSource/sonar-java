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
package org.sonar.java.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbols;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JSymbolTest {

  @Test
  void name() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { C() { } void m() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl constructor = (MethodTreeImpl) c.members().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(1);
    assertThat(cu.sema.typeSymbol(c.typeBinding).name())
      .isEqualTo(c.symbol().name())
      .isEqualTo("C");
    assertThat(cu.sema.methodSymbol(constructor.methodBinding).name())
      .isEqualTo(constructor.symbol().name())
      .isEqualTo("<init>");
    assertThat(cu.sema.methodSymbol(method.methodBinding).name())
      .isEqualTo(method.symbol().name())
      .isEqualTo("m");
  }

  @Test
  void owner() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C1 { int f; class C2 { } void m(int p) { class C3 { } } }");
    ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c1.members().get(0);
    ClassTreeImpl c2 = (ClassTreeImpl) c1.members().get(1);
    MethodTreeImpl m = (MethodTreeImpl) c1.members().get(2);
    VariableTreeImpl p = (VariableTreeImpl) m.parameters().get(0);
    ClassTreeImpl c3 = (ClassTreeImpl) m.block().body().get(0);

    assertThat(cu.sema.typeSymbol(c1.typeBinding).owner())
      .as("of top-level class")
      .isSameAs(cu.sema.packageSymbol(c1.typeBinding.getPackage()));

    assertThat(cu.sema.typeSymbol(c2.typeBinding).owner())
      .as("of nested class")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.typeSymbol(c3.typeBinding).owner())
      .as("of local class")
      .isSameAs(cu.sema.methodSymbol(m.methodBinding));

    assertThat(cu.sema.methodSymbol(m.methodBinding).owner())
      .as("of method")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(f.variableBinding).owner())
      .as("of field")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(p.variableBinding).owner())
      .as("of method parameter")
      .isSameAs(cu.sema.methodSymbol(m.methodBinding));
  }

  @Test
  void enclosingClass() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C1 { int f; class C2 { } void m(int p) { class C3 { } } }");
    ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c1.members().get(0);
    ClassTreeImpl c2 = (ClassTreeImpl) c1.members().get(1);
    MethodTreeImpl m = (MethodTreeImpl) c1.members().get(2);
    VariableTreeImpl p = (VariableTreeImpl) m.parameters().get(0);
    ClassTreeImpl c3 = (ClassTreeImpl) m.block().body().get(0);

    assertThat(cu.sema.typeSymbol(c1.typeBinding).enclosingClass())
      .as("of top-level class")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.typeSymbol(c2.typeBinding).enclosingClass())
      .as("of nested class")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.typeSymbol(c3.typeBinding).enclosingClass())
      .as("of local class")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.methodSymbol(m.methodBinding).enclosingClass())
      .as("of method")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(f.variableBinding).enclosingClass())
      .as("of field")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(p.variableBinding).enclosingClass())
      .as("of method parameter")
      .isSameAs(cu.sema.typeSymbol(c1.typeBinding));
  }

  @Test
  void variable_in_class_initializer() {
    JavaTree.CompilationUnitTreeImpl cu = test("enum E { C; { int i; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    BlockTreeImpl b = (BlockTreeImpl) c.members().get(1);
    VariableTreeImpl v = (VariableTreeImpl) b.body().get(0);
    assertThat(cu.sema.variableSymbol(v.variableBinding).owner())
      .isSameAs(cu.sema.typeSymbol(((ClassTreeImpl) v.symbol().owner().declaration()).typeBinding))
      .isSameAs(cu.sema.typeSymbol(c.typeBinding));
    assertThat(cu.sema.variableSymbol(v.variableBinding).enclosingClass())
      .isSameAs(cu.sema.typeSymbol(((ClassTreeImpl) v.symbol().owner().declaration()).typeBinding))
      .isSameAs(cu.sema.typeSymbol(c.typeBinding));
  }

  @Test
  void type() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; void m() {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl field = (VariableTreeImpl) c.members().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(1);

    assertThat(cu.sema.typeSymbol(c.typeBinding).owner().type())
      .isSameAs(c.symbol().owner().type())
      .isNull();

    assertThat(cu.sema.typeSymbol(c.typeBinding).type())
      .isSameAs(cu.sema.type(c.typeBinding));

    assertThat(cu.sema.variableSymbol(field.variableBinding).type())
      .isSameAs(cu.sema.type(field.variableBinding.getType()));

    assertThat(cu.sema.methodSymbol(method.methodBinding).type())
      .isSameAs(Symbols.unknownType);
  }

  @Nested
  class kinds {

    private JavaTree.CompilationUnitTreeImpl cu;
    private ClassTreeImpl c1;
    private VariableTreeImpl f;
    private MethodTreeImpl foo;

    @BeforeEach
    void parse() {
      cu = test("class C1 { int f; void foo() { } }");
      c1 = (ClassTreeImpl) cu.types().get(0);
      f = (VariableTreeImpl) c1.members().get(0);
      foo = (MethodTreeImpl) c1.members().get(1);
    }

    @Test
    void isPackageSymbol() {
      JTypeSymbol c1Symbol = cu.sema.typeSymbol(c1.typeBinding);
      assertThat(c1Symbol.isPackageSymbol()).isFalse();
      assertThat(c1Symbol.owner().isPackageSymbol()).isTrue();
    }

    @Test
    void isTypeSymbol() {
      assertThat(cu.sema.typeSymbol(c1.typeBinding).isTypeSymbol()).isTrue();
      assertThat(cu.sema.methodSymbol(foo.methodBinding).isTypeSymbol()).isFalse();
      assertThat(cu.sema.variableSymbol(f.variableBinding).isTypeSymbol()).isFalse();
    }

    @Test
    void isMethodSymbol() {
      assertThat(cu.sema.typeSymbol(c1.typeBinding).isMethodSymbol()).isFalse();
      assertThat(cu.sema.methodSymbol(foo.methodBinding).isMethodSymbol()).isTrue();
      assertThat(cu.sema.variableSymbol(f.variableBinding).isMethodSymbol()).isFalse();
    }

    @Test
    void isVariableSymbol() {
      assertThat(cu.sema.typeSymbol(c1.typeBinding).isVariableSymbol()).isFalse();
      assertThat(cu.sema.methodSymbol(foo.methodBinding).isVariableSymbol()).isFalse();
      assertThat(cu.sema.variableSymbol(f.variableBinding).isVariableSymbol()).isTrue();
    }
  }

  @Nested
  class modifiers {
    @Test
    void isStatic() {
      JavaTree.CompilationUnitTreeImpl cu = test("class C1 {"
        + "  static void foo() { }"
        + "  void bar() { }"
        + "}");

      assertThat(foo(cu).isStatic()).isTrue();
      assertThat(bar(cu).isStatic()).isFalse();
    }

    @Test
    void isFinal() {
      JavaTree.CompilationUnitTreeImpl cu = test("class C1 {"
        + "  final void foo() { }"
        + "  void bar() { }"
        + "}");

      assertThat(foo(cu).isFinal()).isTrue();
      assertThat(bar(cu).isFinal()).isFalse();
    }

    @Test
    void isAbstract() {
      JavaTree.CompilationUnitTreeImpl cu = test("abstract class C1 {"
        + "  abstract void foo();"
        + "  void bar() { }"
        + "}");

      assertThat(foo(cu).isAbstract()).isTrue();
      assertThat(bar(cu).isAbstract()).isFalse();
    }

    @Test
    void isPublic() {
      JavaTree.CompilationUnitTreeImpl cu = test("class C1 {"
        + "  public void foo() { }"
        + "  void bar() { }"
        + "}");

      assertThat(foo(cu).isPublic()).isTrue();
      assertThat(bar(cu).isPublic()).isFalse();
    }

    @Test
    void isPrivate() {
      JavaTree.CompilationUnitTreeImpl cu = test("class C1 {"
        + "  private void foo() { }"
        + "  void bar() { }"
        + "}");

      assertThat(foo(cu).isPrivate()).isTrue();
      assertThat(bar(cu).isPrivate()).isFalse();
    }

    @Test
    void isProtected() {
      JavaTree.CompilationUnitTreeImpl cu = test("class C1 {"
        + "  protected void foo() { }"
        + "  void bar() { }"
        + "}");

      assertThat(foo(cu).isProtected()).isTrue();
      assertThat(bar(cu).isProtected()).isFalse();
    }

    @Test
    void isPackageVisibility() {
      JavaTree.CompilationUnitTreeImpl cu = test("class C1 {"
        + "  void foo() { }"
        + "  private void bar() { }"
        + "}");

      assertThat(foo(cu).isPackageVisibility()).isTrue();
      assertThat(bar(cu).isPackageVisibility()).isFalse();
    }

    @Test
    void isDeprecated() {
      JavaTree.CompilationUnitTreeImpl cu = test("class C1 {"
        + "  @Deprecated"
        + "  void foo() { }"
        + "  void bar() { }"
        + "}");

      assertThat(foo(cu).isDeprecated()).isTrue();
      assertThat(bar(cu).isDeprecated()).isFalse();
    }

    @Test
    void isVolatile() {
      JavaTree.CompilationUnitTreeImpl cu = test("class C1 {"
        + "  volatile int a;"
        + "  int b;"
        + "}");

      JVariableSymbol a = variable(cu, 0);
      assertThat(a.isVolatile()).isTrue();

      JVariableSymbol b = variable(cu, 1);
      assertThat(b.isVolatile()).isFalse();
    }

    @Test
    void isInterface() {
      JavaTree.CompilationUnitTreeImpl cu = test(
        "interface I1 { } "
          + "class C1 { }");

      assertThat(cu.sema.typeSymbol(((ClassTreeImpl) cu.types().get(0)).typeBinding).isInterface()).isTrue();
      assertThat(cu.sema.typeSymbol(((ClassTreeImpl) cu.types().get(1)).typeBinding).isInterface()).isFalse();
    }

    @Test
    void isEnum() {
      JavaTree.CompilationUnitTreeImpl cu = test("class C1 { int a; void foo() { }} enum E1 { ACONST; }");

      ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
      JTypeSymbol c1Symbol = cu.sema.typeSymbol(c1.typeBinding);
      assertThat(c1Symbol.isEnum()).isFalse();

      JVariableSymbol a = cu.sema.variableSymbol(((VariableTreeImpl) c1.members().get(0)).variableBinding);
      assertThat(a.isEnum()).isFalse();

      JMethodSymbol foo = cu.sema.methodSymbol(((MethodTreeImpl) c1.members().get(1)).methodBinding);
      assertThat(foo.isEnum()).isFalse();

      ClassTreeImpl e1 = (ClassTreeImpl) cu.types().get(1);
      JTypeSymbol e1Symbol = cu.sema.typeSymbol(e1.typeBinding);
      assertThat(e1Symbol.isEnum()).isTrue();

      JVariableSymbol aconst = cu.sema.variableSymbol(((EnumConstantTreeImpl) e1.members().get(0)).variableBinding);
      assertThat(aconst.isEnum()).isTrue();
    }

    private JMethodSymbol foo(JavaTree.CompilationUnitTreeImpl cu) {
      return method(cu, 0);
    }

    private JMethodSymbol bar(JavaTree.CompilationUnitTreeImpl cu) {
      return method(cu, 1);
    }

    private JMethodSymbol method(JavaTree.CompilationUnitTreeImpl cu, int memberIndex) {
      ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
      return cu.sema.methodSymbol(((MethodTreeImpl) c1.members().get(memberIndex)).methodBinding);
    }

    private JVariableSymbol variable(JavaTree.CompilationUnitTreeImpl cu, int memberIndex) {
      ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
      return cu.sema.variableSymbol(((VariableTreeImpl) c1.members().get(memberIndex)).variableBinding);
    }
  }

  JavaTree.CompilationUnitTreeImpl test(String source) {
    List<File> classpath = Collections.emptyList();
    JavaTree.CompilationUnitTreeImpl t = (JavaTree.CompilationUnitTreeImpl) JParser.parse(
      "12",
      "File.java",
      source,
      true,
      classpath
    );
    SemanticModel.createFor(t, new SquidClassLoader(classpath));
    return t;
  }

}
