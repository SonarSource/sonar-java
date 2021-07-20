/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.assertions.SymbolAssert.assertThat;

class JSymbolTest {

  @Test
  void name() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { C() { } void m() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl constructor = (MethodTreeImpl) c.members().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(1);

    assertThat(cu.sema.typeSymbol(c.typeBinding))
      .hasSameNameAs(c.symbol())
      .hasName("C");
    assertThat(cu.sema.methodSymbol(constructor.methodBinding))
      .hasSameNameAs(constructor.symbol())
      .hasName("<init>");
    assertThat(cu.sema.methodSymbol(method.methodBinding))
      .hasSameNameAs(method.symbol())
      .hasName("m");
  }

  @Test
  void owner() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C1 { int f; class C2 { } void m(int p) { class C3 { Unknown u; } } }");
    ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c1.members().get(0);
    ClassTreeImpl c2 = (ClassTreeImpl) c1.members().get(1);
    MethodTreeImpl m = (MethodTreeImpl) c1.members().get(2);
    VariableTreeImpl p = (VariableTreeImpl) m.parameters().get(0);
    ClassTreeImpl c3 = (ClassTreeImpl) m.block().body().get(0);
    VariableTreeImpl u = (VariableTreeImpl) c3.members().get(0);

    assertThat(cu.sema.typeSymbol(c1.typeBinding))
      .as("of top-level class")
      .hasOwner(cu.sema.packageSymbol(c1.typeBinding.getPackage()));

    assertThat(cu.sema.typeSymbol(c2.typeBinding))
      .as("of nested class")
      .hasOwner(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.typeSymbol(c3.typeBinding))
      .as("of local class")
      .hasOwner(cu.sema.methodSymbol(m.methodBinding));

    assertThat(cu.sema.methodSymbol(m.methodBinding))
      .as("of method")
      .hasOwner(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(f.variableBinding))
      .as("of field")
      .hasOwner(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(p.variableBinding))
      .as("of method parameter")
      .hasOwner(cu.sema.methodSymbol(m.methodBinding));

    JType uType = cu.sema.type(u.variableBinding.getType());
    Symbol.TypeSymbol uTypeSymbol = uType.symbol();
    assertThat(uType.isUnknown()).isTrue();
    assertThat(uTypeSymbol.isUnknown()).isTrue();
    assertThat(uTypeSymbol.owner().isUnknown()).isTrue();
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

    assertThat(cu.sema.typeSymbol(c1.typeBinding))
      .as("of top-level class")
      .hasEnclosingClass(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.typeSymbol(c2.typeBinding))
      .as("of nested class")
      .hasEnclosingClass(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.typeSymbol(c3.typeBinding))
      .as("of local class")
      .hasEnclosingClass(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.methodSymbol(m.methodBinding))
      .as("of method")
      .hasEnclosingClass(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(f.variableBinding))
      .as("of field")
      .hasEnclosingClass(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(p.variableBinding))
      .as("of method parameter")
      .hasEnclosingClass(cu.sema.typeSymbol(c1.typeBinding));
  }

  @Test
  void record_enclosingClass() {
    JavaTree.CompilationUnitTreeImpl cu = test("record C1(int f) { record C2(int p) { } }");
    ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c1.recordComponents().get(0);
    ClassTreeImpl c2 = (ClassTreeImpl) c1.members().get(0);
    VariableTreeImpl p = (VariableTreeImpl) c2.recordComponents().get(0);

    assertThat(cu.sema.variableSymbol(f.variableBinding))
      .as("of field")
      .hasEnclosingClass(cu.sema.typeSymbol(c1.typeBinding));

    assertThat(cu.sema.variableSymbol(p.variableBinding))
      .as("of method parameter")
      .hasEnclosingClass(cu.sema.typeSymbol(c2.typeBinding));
  }

  private void variable_in_class_initializer(boolean isStatic) {
    String src = "enum E { C; " + (isStatic ? "static " : "") + "{ int i; } }";
    JavaTree.CompilationUnitTreeImpl cu = test(src);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    BlockTreeImpl b = (BlockTreeImpl) c.members().get(1);
    VariableTreeImpl v = (VariableTreeImpl) b.body().get(0);
    Symbol.TypeSymbol t = cu.sema.typeSymbol(c.typeBinding);

    JVariableSymbol variableSymbol = cu.sema.variableSymbol(v.variableBinding);
    assertThat(variableSymbol).isSameAs(v.symbol());
    JInitializerBlockSymbol initializerBlock = (JInitializerBlockSymbol) variableSymbol.owner();

    if (isStatic) {
      assertThat(initializerBlock).isSameAs(cu.sema.staticInitializerBlockSymbol((JTypeSymbol) c.symbol()));
    } else {
      assertThat(initializerBlock).isSameAs(cu.sema.initializerBlockSymbol((JTypeSymbol) c.symbol()));
    }

    assertThat(variableSymbol).hasEnclosingClass(t);
    assertThat(initializerBlock).hasEnclosingClass(t).hasOwner(t);
    assertThat(initializerBlock.isStatic()).isEqualTo(isStatic);
    assertThat(initializerBlock).hasName(isStatic ? "<clinit> (initializer block)" : "<init> (initializer block)");
    assertThat(initializerBlock.signature()).isEqualTo(isStatic ? "E.<clinit> (initializer block)" : "E.<init> (initializer block)");

    assertThat(initializerBlock.isMethodSymbol()).isTrue();
    assertThat(initializerBlock.isVariableSymbol()).isFalse();
    assertThat(initializerBlock.isTypeSymbol()).isFalse();
    assertThat(initializerBlock.isEnum()).isFalse();
    assertThat(initializerBlock.isInterface()).isFalse();
    assertThat(initializerBlock.isPackageSymbol()).isFalse();
    assertThat(initializerBlock.isFinal()).isFalse();
    assertThat(initializerBlock.isAbstract()).isFalse();
    assertThat(initializerBlock.isPrivate()).isFalse();
    assertThat(initializerBlock.isProtected()).isFalse();
    assertThat(initializerBlock.isPublic()).isFalse();
    assertThat(initializerBlock.isPackageVisibility()).isFalse();
    assertThat(initializerBlock.isDeprecated()).isFalse();
    assertThat(initializerBlock.isVolatile()).isFalse();
    assertThat(initializerBlock.isUnknown()).isFalse();

    assertThat(initializerBlock.declaration()).isNull();
    assertThat(initializerBlock.returnType()).isUnknown();
    assertThat(initializerBlock).isOfUnknownType();
    assertThat(initializerBlock.overriddenSymbol()).isNull();
    assertThat(initializerBlock.overriddenSymbols()).isEmpty();
    assertThat(initializerBlock.usages()).isEmpty();
    assertThat(initializerBlock.parameterTypes()).isEmpty();
    assertThat(initializerBlock.thrownTypes()).isEmpty();
  }

  @Test
  void variable_in_class_initializer() {
    variable_in_class_initializer(false);
  }

  @Test
  void variable_in_static_class_initializer() {
    variable_in_class_initializer(true);
  }

  @Test
  void type() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int f; void m() {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl field = (VariableTreeImpl) c.members().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(1);

    assertThat(cu.sema.typeSymbol(c.typeBinding).owner())
      .isOfSameTypeAs(c.symbol().owner())
      .isOfUnknownType();

    assertThat(cu.sema.typeSymbol(c.typeBinding))
      .isOfType(cu.sema.type(c.typeBinding));

    assertThat(cu.sema.variableSymbol(field.variableBinding))
      .isOfType(cu.sema.type(field.variableBinding.getType()));

    assertThat(cu.sema.methodSymbol(method.methodBinding)).isOfUnknownType();
  }

  @Test
  void package_has_an_unknown_type() {
    JavaTree.CompilationUnitTreeImpl cu = test("package p; class C { p.C f; }");
    VariableTree field = (VariableTree) ((ClassTree) cu.types().get(0)).members().get(0);
    MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) field.type();

    IdentifierTree p = (IdentifierTree) memberSelect.expression();
    assertThat(p.symbolType().isUnknown()).isTrue();
    assertThat(p.symbol().type().isUnknown()).isTrue();

    IdentifierTree c = memberSelect.identifier();
    assertThat(c.symbol().type().fullyQualifiedName()).isEqualTo("p.C");
  }

  @Test
  void var_type_as_local_variable() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C {void m() { var v = new java.util.ArrayList<String>(); }}");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl variable = (VariableTreeImpl) method.block().body().get(0);

    assertThat(variable.type().symbolType().symbol())
      .isOfType("java.util.ArrayList");

    assertThat(variable.symbol())
      .isOfType("java.util.ArrayList");

    assertThat(cu.sema.variableSymbol(variable.variableBinding))
      .isOfType(cu.sema.type(variable.variableBinding.getType()));
  }

  @Test
  void var_type_as_lambda_parameters() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { java.util.function.BiFunction<Long, Boolean, String> f = (var x, var y) -> x + \",\" + y; }");

    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl field = (VariableTreeImpl) c.members().get(0);
    LambdaExpressionTree lambda = (LambdaExpressionTree) field.initializer();

    assertThat(field.symbol()).isOfType("java.util.function.BiFunction");

    VariableTree x = lambda.parameters().get(0);
    assertThat(x.symbol()).isOfType("java.lang.Long");

    VariableTree y = lambda.parameters().get(1);
    assertThat(y.symbol()).isOfType("java.lang.Boolean");
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

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
