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
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.sonar.java.model.JParserTestUtils.typeOf;
import static org.sonar.java.model.assertions.SymbolAssert.assertThat;

class JSymbolTest {

  @Test
  void name() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { C() { } void m() { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl constructor = (MethodTreeImpl) c.members().get(0);
    MethodTreeImpl method = (MethodTreeImpl) c.members().get(1);

    assertThat(typeOf(c).symbol())
      .hasName("C");
    assertThat(constructor.symbol())
      .hasName("<init>");
    assertThat(method.symbol())
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

    Symbol packageSymbol = c1.symbol().owner();
    assertThat(packageSymbol.isPackageSymbol()).isTrue();
    assertThat(packageSymbol.name()).isEmpty();

    assertThat(typeOf(c2).symbol())
      .as("of nested class")
      .hasOwner(typeOf(c1).symbol());

    assertThat(typeOf(c3).symbol())
      .as("of local class")
      .hasOwner(m.symbol());

    assertThat(m.symbol())
      .as("of method")
      .hasOwner(typeOf(c1).symbol());

    assertThat(f.symbol())
      .as("of field")
      .hasOwner(typeOf(c1).symbol());

    assertThat(p.symbol())
      .as("of method parameter")
      .hasOwner(m.symbol());

    assertThat(p.symbol().type().symbol())
      .as("of type int")
      .hasOwner(Symbol.ROOT_PACKAGE)
      .hasSameHashCodeAs(p.type().symbolType().symbol().hashCode());

    Type uType = u.symbol().type();
    Symbol.TypeSymbol uTypeSymbol = uType.symbol();
    assertThat(uType.isUnknown()).isTrue();
    assertThat(uTypeSymbol.isUnknown()).isTrue();
    assertThat(uTypeSymbol.owner().isUnknown()).isTrue();
  }

  @Test
  void owner_of_types_without_package_default_to_root_package_instead_of_null() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C {\n" +
      "  Object objectField;\n" + // 'Object' is a regular type from 'java.lang' package
      "  int intField;\n" + // 'int' is a primitive type without package
      "  Object[] objectArrayField;\n" + // 'Object[]' is an array type without package
      "  java.util.List<?> listField;\n" + // '?' is a wildcard type without package
      "}\n"
    );
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);

    Symbol objectFieldType = ((VariableTree) c.members().get(0)).type().symbolType().symbol();
    assertThat(objectFieldType.owner().name()).isEqualTo("java.lang");

    Symbol intFieldType = ((VariableTree) c.members().get(1)).type().symbolType().symbol();
    assertThat(intFieldType.owner().name()).isEmpty();
    assertThat(intFieldType.owner()).isEqualTo(Symbol.ROOT_PACKAGE);

    Symbol objectArrayFieldType = ((VariableTree) c.members().get(2)).type().symbolType().symbol();
    assertThat(objectArrayFieldType.owner()).isEqualTo(Symbol.ROOT_PACKAGE);

    Type listFieldTypeTree = ((VariableTree) c.members().get(3)).type().symbolType();
    Symbol.TypeSymbol listType = listFieldTypeTree.symbol();
    assertThat(listType.owner().name()).isEqualTo("java.util");
    assertThat(listFieldTypeTree.typeArguments()).hasSize(1);
    Type wildcardType = listFieldTypeTree.typeArguments().get(0);
    assertThat(wildcardType.name()).isEqualTo("?");
    assertThat(wildcardType.symbol().owner()).isEqualTo(Symbol.ROOT_PACKAGE);
  }

  @Test
  void owner_local_record() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C1 { void m() { record r(Object p) {  } } }");
    ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c1.members().get(0);
    ClassTreeImpl r = (ClassTreeImpl) m.block().body().get(0);
    VariableTreeImpl p = (VariableTreeImpl) r.recordComponents().get(0);

    assertThat(p.symbol())
      .as("of record component")
      .hasOwner(typeOf(r).symbol());

    assertThat(m.symbol())
      .as("of method")
      .hasOwner(typeOf(c1).symbol());
  }

  @Test
  void owner_lambdaVariable_variableBinding() {
    JavaTree.CompilationUnitTreeImpl cu = test("""
      class C {
        java.util.function.Function<Integer, Integer> variableBinding = i -> i;
      }
      """
    );
    var classTree = (ClassTreeImpl) cu.types().get(0);
    var lambda = (LambdaExpressionTree) ((VariableTree) classTree.members().get(0)).initializer();
    var i = (IdentifierTree) lambda.body();
    Symbol owner = i.symbol().owner();
    assertThat(owner).isSameAs(lambda.symbol());
  }

  @Test
  void owner_lambdaVariable_methodBinding() {
    JavaTree.CompilationUnitTreeImpl cu = test("""
      class C {
         void foo() {
             java.util.function.Function<Long, Long> methodBinding = l -> l;
          }
      }
      """
    );
    ClassTreeImpl classTree = (ClassTreeImpl) cu.types().get(0);
    MethodTree method = (MethodTree) classTree.members().get(0);
    VariableTree bindingInsideMethod = (VariableTree) method.block().body().get(0);
    LambdaExpressionTree lambda = (LambdaExpressionTree) bindingInsideMethod.initializer();
    VariableTree l = lambda.parameters().get(0);
    Symbol owner = l.symbol().owner();
    assertThat(owner).isSameAs(lambda.symbol());
  }

  @Test
  void primitive_type_hash_code() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { int u; int v; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl uField = (VariableTreeImpl) c.members().get(0);
    VariableTreeImpl vField = (VariableTreeImpl) c.members().get(1);
    assertThat(uField.symbol().type().symbol())
      .hasSameHashCodeAs(vField.symbol().type().symbol());
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

    assertThat(typeOf(c1).symbol())
      .as("of top-level class")
      .hasEnclosingClass(typeOf(c1).symbol());

    assertThat(typeOf(c2).symbol())
      .as("of nested class")
      .hasEnclosingClass(typeOf(c1).symbol());

    assertThat(typeOf(c3).symbol())
      .as("of local class")
      .hasEnclosingClass(typeOf(c1).symbol());

    assertThat(m.symbol())
      .as("of method")
      .hasEnclosingClass(typeOf(c1).symbol());

    assertThat(f.symbol())
      .as("of field")
      .hasEnclosingClass(typeOf(c1).symbol());

    assertThat(p.symbol())
      .as("of method parameter")
      .hasEnclosingClass(typeOf(c1).symbol());
  }

  @Test
  void record_enclosingClass() {
    JavaTree.CompilationUnitTreeImpl cu = test("record C1(int f) { record C2(int p) { } }");
    ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c1.recordComponents().get(0);
    ClassTreeImpl c2 = (ClassTreeImpl) c1.members().get(0);
    VariableTreeImpl p = (VariableTreeImpl) c2.recordComponents().get(0);

    assertThat(f.symbol())
      .as("of field")
      .hasEnclosingClass(typeOf(c1).symbol());

    assertThat(p.symbol())
      .as("of method parameter")
      .hasEnclosingClass(typeOf(c2).symbol());
  }

  @Test
  void super_keyword_in_the_java_lang_Object_scope_has_unknown_type_instead_of_null() {
    JavaTree.CompilationUnitTreeImpl cu = test("""
      package java.lang;
      class Object {
        void foo() { super.hashCode(); }
      }
      """);
    ClassTree objectClass = (ClassTree) cu.types().get(0);
    MethodTree fooMethod = (MethodTree) objectClass.members().get(0);
    ExpressionStatementTree firstStatement = (ExpressionStatementTree) fooMethod.block().body().get(0);
    MethodInvocationTree hashCodeInvocation = (MethodInvocationTree) firstStatement.expression();
    MemberSelectExpressionTree methodSelect = (MemberSelectExpressionTree) hashCodeInvocation.methodSelect();
    KeywordSuper keywordSuper = (KeywordSuper) methodSelect.expression();

    assertThat(keywordSuper.symbolType()).isEqualTo(Type.UNKNOWN);
  }

  private void variable_in_class_initializer(boolean isStatic) {
    String src = "enum E { C; " + (isStatic ? "static " : "") + "{ int i; } }";
    JavaTree.CompilationUnitTreeImpl cu = test(src);
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    BlockTreeImpl b = (BlockTreeImpl) c.members().get(1);
    VariableTreeImpl v = (VariableTreeImpl) b.body().get(0);
    Symbol.TypeSymbol t = typeOf(c).symbol();

    Symbol variableSymbol = v.symbol();
    Symbol.MethodSymbol initializerBlock = (Symbol.MethodSymbol) variableSymbol.owner();

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
    assertThat(initializerBlock.overriddenSymbols()).isEmpty();
    assertThat(initializerBlock.usages()).isEmpty();
    assertThat(initializerBlock.parameterTypes()).isEmpty();
    assertThat(initializerBlock.declarationParameters()).isEmpty();
    assertThat(initializerBlock.thrownTypes()).isEmpty();
    assertThat(initializerBlock.isOverridable()).isFalse();
    assertThat(initializerBlock.isParametrizedMethod()).isFalse();
    assertThat(initializerBlock.isDefaultMethod()).isFalse();
    assertThat(initializerBlock.isSynchronizedMethod()).isFalse();
    assertThat(initializerBlock.isVarArgsMethod()).isFalse();
    assertThat(initializerBlock.isNativeMethod()).isFalse();
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

    assertThat(typeOf(c).symbol().owner())
      .isOfSameTypeAs(c.symbol().owner())
      .isOfUnknownType();

    assertThat(typeOf(c).symbol())
      .isOfType(typeOf(c));

    assertThat(field.symbol())
      .isOfType(field.symbol().type());

    assertThat(method.symbol()).isOfUnknownType();
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

    assertThat(variable.symbol())
      .isOfType(variable.symbol().type());
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

  @Test
  void test_with_type_annotation() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C { void m(List<@Nullable String> p) { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    Symbol.MethodSymbol declarationSymbol = m.symbol();
    SymbolMetadata metadata = declarationSymbol.metadata();
    assertThat(metadata).isNotNull();
    SymbolMetadata parameterMetadata = m.parameters().get(0).symbol().metadata();
    assertThat(parameterMetadata.annotations()).hasSize(1);
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
      Symbol.TypeSymbol c1Symbol = typeOf(c1).symbol();
      assertThat(c1Symbol.isPackageSymbol()).isFalse();
      assertThat(c1Symbol.owner().isPackageSymbol()).isTrue();
    }

    @Test
    void isTypeSymbol() {
      assertThat(typeOf(c1).symbol().isTypeSymbol()).isTrue();
      assertThat(foo.symbol().isTypeSymbol()).isFalse();
      assertThat(f.symbol().isTypeSymbol()).isFalse();
    }

    @Test
    void isMethodSymbol() {
      assertThat(typeOf(c1).symbol().isMethodSymbol()).isFalse();
      assertThat(foo.symbol().isMethodSymbol()).isTrue();
      assertThat(f.symbol().isMethodSymbol()).isFalse();
    }

    @Test
    void isVariableSymbol() {
      assertThat(typeOf(c1).symbol().isVariableSymbol()).isFalse();
      assertThat(foo.symbol().isVariableSymbol()).isFalse();
      assertThat(f.symbol().isVariableSymbol()).isTrue();
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

      Symbol a = variable(cu, 0);
      assertThat(a.isVolatile()).isTrue();

      Symbol b = variable(cu, 1);
      assertThat(b.isVolatile()).isFalse();
    }

    @Test
    void isInterface() {
      JavaTree.CompilationUnitTreeImpl cu = test(
        "interface I1 { } "
          + "class C1 { }");

      assertThat(typeOf(((ClassTreeImpl) cu.types().get(0))).symbol().isInterface()).isTrue();
      assertThat(typeOf(((ClassTreeImpl) cu.types().get(1))).symbol().isInterface()).isFalse();
    }

    @Test
    void isEnum() {
      JavaTree.CompilationUnitTreeImpl cu = test("class C1 { int a; void foo() { }} enum E1 { ACONST; }");

      ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
      Symbol.TypeSymbol c1Symbol = typeOf(c1).symbol();
      assertThat(c1Symbol.isEnum()).isFalse();

      Symbol a = ((VariableTreeImpl) c1.members().get(0)).symbol();
      assertThat(a.isEnum()).isFalse();

      Symbol.MethodSymbol foo = ((MethodTreeImpl) c1.members().get(1)).symbol();
      assertThat(foo.isEnum()).isFalse();

      ClassTreeImpl e1 = (ClassTreeImpl) cu.types().get(1);
      Symbol.TypeSymbol e1Symbol = typeOf(e1).symbol();
      assertThat(e1Symbol.isEnum()).isTrue();

      Symbol aconst = ((EnumConstantTreeImpl) e1.members().get(0)).symbol();
      assertThat(aconst.isEnum()).isTrue();
    }

    private Symbol.MethodSymbol foo(JavaTree.CompilationUnitTreeImpl cu) {
      return method(cu, 0);
    }

    private Symbol.MethodSymbol bar(JavaTree.CompilationUnitTreeImpl cu) {
      return method(cu, 1);
    }

    private Symbol.MethodSymbol method(JavaTree.CompilationUnitTreeImpl cu, int memberIndex) {
      ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
      return ((MethodTreeImpl) c1.members().get(memberIndex)).symbol();
    }

    private Symbol variable(JavaTree.CompilationUnitTreeImpl cu, int memberIndex) {
      ClassTreeImpl c1 = (ClassTreeImpl) cu.types().get(0);
      return ((VariableTreeImpl) c1.members().get(memberIndex)).symbol();
    }
  }

  @Test
  void testEquality_twoLambdas() {
    CompilationUnitTree cu = test("""
      class C {
        F k = s -> {};
        F j = s -> {};
        interface F extends java.util.function.Consumer<String> {}
      }
      """);
    ClassTree classTree = (ClassTree) cu.types().get(0);
    var k = (VariableTree) classTree.members().get(0);
    var lambda1 = (LambdaExpressionTree) k.initializer();
    Symbol.MethodSymbol methodSymbol1 = lambda1.symbol();

    var j = (VariableTree) classTree.members().get(1);
    var lambda2 = (LambdaExpressionTree) j.initializer();
    Symbol.MethodSymbol methodSymbol2 = lambda2.symbol();

    assertNotEquals(methodSymbol1, methodSymbol2);
  }

  @Test
  void equivalent_method_and_lambda_have_different_symbols() {
    CompilationUnitTree cu = test("""
      class C {
        java.util.function.Consumer<String> k = s -> {};
        static void j(String s) {};
      }
      """);
    ClassTree classTree = (ClassTree) cu.types().get(0);
    var k = (VariableTree) classTree.members().get(0);
    var lambda1 = (LambdaExpressionTree) k.initializer();
    Symbol.MethodSymbol methodSymbol1 = lambda1.symbol();

    Symbol.MethodSymbol methodSymbol2 = ((MethodTree) classTree.members().get(1)).symbol();

    assertNotEquals(methodSymbol1, methodSymbol2);
    assertNotEquals(methodSymbol2, methodSymbol1);
  }

  @Test
  void methods_with_same_name_from_different_owners_have_different_symbols() {
    CompilationUnitTree cu = test("""
      class C {
        void j(String s) {}
      }
      class D {
        void j(String s) {}
      }
      """);
    ClassTree cClassTree = (ClassTree) cu.types().get(0);
    var cj = (MethodTree) cClassTree.members().get(0);
    Symbol.MethodSymbol methodSymbol1 = cj.symbol();

    ClassTree dClassTree = (ClassTree) cu.types().get(1);
    var dj = (MethodTree) dClassTree.members().get(0);
    Symbol.MethodSymbol methodSymbol2 = dj.symbol();

    assertNotEquals(methodSymbol1, methodSymbol2);
    assertNotEquals(methodSymbol2, methodSymbol1);
  }

  @Test
  void overloading_methods_have_different_symbols() {
    CompilationUnitTree cu = test("""
      class C {
        void j(String s) {}
        void j(Integer i) {}
      }
      """);
    ClassTree cClassTree = (ClassTree) cu.types().get(0);
    var jString = (MethodTree) cClassTree.members().get(0);
    Symbol.MethodSymbol methodSymbol1 = jString.symbol();

    var jInteger = (MethodTree) cClassTree.members().get(1);
    Symbol.MethodSymbol methodSymbol2 = jInteger.symbol();

    assertNotEquals(methodSymbol1, methodSymbol2);
    assertNotEquals(methodSymbol2, methodSymbol1);
  }

  @Test
  void method_declaration_and_invocation_share_the_same_symbol() {
    CompilationUnitTree cu = test("""
      class C<R> {
        int f(int i) { return f(i-1); }
      }
      """);
    ClassTree cClassTree = (ClassTree) cu.types().get(0);
    var f = (MethodTree) cClassTree.members().get(0);
    var returnTree = (ReturnStatementTree) f.block().body().get(0);
    var invocation = (MethodInvocationTree) returnTree.expression();
    Symbol.MethodSymbol fInRecursion = invocation.methodSymbol();

    assertEquals(f.symbol(), fInRecursion);
    assertEquals(fInRecursion, f.symbol());
  }

  @Test
  void methods_and_types_have_different_symbols() {
    CompilationUnitTree cu = test("""
      class C {
        int f(int i) { 
          return 0;
        }
      }
      """);

    ClassTree cClassTree = (ClassTree) cu.types().get(0);
    Symbol cSymbol = (Symbol) cClassTree.symbol();

    MethodTree f = (MethodTree) cClassTree.members().get(0);
    Symbol fSymbol = (Symbol) f.symbol();
    assertNotEquals(fSymbol, cSymbol);

    // Symmetric case
    assertNotEquals(cSymbol, fSymbol);
  }

  @Test
  void methods_and_variables_have_different_symbols() {
    CompilationUnitTree cu = test("""
      class C<R> {
        int f = 0;
        int f(int i) { return 0; }
      }
      """);
    ClassTree cClassTree = (ClassTree) cu.types().get(0);
    var fVar = (VariableTree) cClassTree.members().get(0);
    var fMethod = (MethodTree) cClassTree.members().get(1);

    assertNotEquals(fMethod.symbol(), fVar.symbol());

    // Test symmetric case
    assertNotEquals(fVar.symbol(), fMethod.symbol());
  }

  @Test
  void methods_with_different_type_parameters_have_different_symbols() {
    CompilationUnitTree cu = test("""
      class C {
        <T> int f(String s);
        {
          f("foo");
        }
      }
      """);
    ClassTree cClassTree = (ClassTree) cu.types().get(0);
    var methodDeclaration = (MethodTree) cClassTree.members().get(0);
    var block = (BlockTree) cClassTree.members().get(1);
    var expression1 = (ExpressionStatementTree) block.body().get(0);
    var invocation1 = (MethodInvocationTree) expression1.expression();

    assertNotEquals(methodDeclaration.symbol(), invocation1.methodSymbol());
    assertNotEquals(invocation1.methodSymbol(), methodDeclaration.symbol());
  }

  @Test
  void methods_with_different_type_arguments_have_different_symbols() {
    CompilationUnitTree cu = test("""
      class C<T> {
        {
          new C<String>();
          new C<Integer>();
        }
      }
      """);
    ClassTree cClassTree = (ClassTree) cu.types().get(0);
    var block = (BlockTree) cClassTree.members().get(0);
    var expression1 = (ExpressionStatementTree) block.body().get(0);
    var invocation1 = (NewClassTree) expression1.expression();

    var expression2 = (ExpressionStatementTree) block.body().get(1);
    var invocation2 = (NewClassTree) expression2.expression();

    assertNotEquals(invocation2.methodSymbol(), invocation1.methodSymbol());
    assertNotEquals(invocation1.methodSymbol(), invocation2.methodSymbol());
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
