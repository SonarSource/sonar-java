/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.TypeCastExpressionTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;

import static org.assertj.core.api.Assertions.assertThat;

class JUtilsTest {

  static final JSema SEMA = test("").sema;
  private static final Type OBJECT_TYPE = SEMA.type(SEMA.resolveType("java.lang.Object"));

  @Nested
  class PrimitiveType {
    @Test
    void java_lang_Byte() {
      Type wrapperType = SEMA.type(SEMA.resolveType("java.lang.Byte"));
      Type primitiveType = wrapperType.primitiveType();

      assertThat(primitiveType).isNotNull();
      assertThat(primitiveType.fullyQualifiedName()).isEqualTo("byte");
    }

    @Test
    void object_is_not_a_primitive_type() {
      assertThat(OBJECT_TYPE.primitiveType()).isNull();
    }
  }

  @Nested
  class PrimitiveWrapperType {
    @Test
    void java_lang_Byte() {
      Type primitiveType = SEMA.type(SEMA.resolveType("byte"));
      Type wrapperType = primitiveType.primitiveWrapperType();

      assertThat(wrapperType).isNotNull();
      assertThat(wrapperType.fullyQualifiedName()).isEqualTo("java.lang.Byte");
    }

    @Test
    void object_is_not_a_primitive_wrapper_type() {
      assertThat(OBJECT_TYPE.primitiveWrapperType()).isNull();
    }
  }

  @Nested
  class IsPrimitiveWrapper {
    private final Type primitiveType = SEMA.type(SEMA.resolveType("byte"));

    @Test
    void wrapper() {
      Type wrapperType = primitiveType.primitiveWrapperType();
      assertThat(wrapperType).isNotNull();
      assertThat(wrapperType.isPrimitiveWrapper()).isTrue();
      assertThat(wrapperType.fullyQualifiedName()).isEqualTo("java.lang.Byte");
    }

    @Test
    void not_a_wrapper() {
      assertThat(OBJECT_TYPE.isPrimitiveWrapper()).isFalse();
    }

    @Test
    void not_a_class() {
      Type ObjectArrayType = SEMA.type(SEMA.resolveType("java.lang.Object[]"));
      assertThat(ObjectArrayType.isPrimitiveWrapper()).isFalse();
    }
  }

  @Nested
  class WrapTypeIfPrimitiveTest {
    @Test
    void prim_to_wrapped() {
      Type primitiveType = SEMA.type(SEMA.resolveType("byte"));
      Type wrappedType = JUtils.wrapTypeIfPrimitive(primitiveType);
      assertThat(wrappedType).isNotNull();
      assertThat(wrappedType.isPrimitive()).isFalse();
      assertThat(wrappedType.fullyQualifiedName()).isEqualTo("java.lang.Byte");
    }

    @Test
    void already_wrapped() {
      Type boxedType = SEMA.type(SEMA.resolveType("java.lang.Byte"));
      Type wrappedType = JUtils.wrapTypeIfPrimitive(boxedType);
      assertThat(wrappedType).isNotNull();
      assertThat(wrappedType.isPrimitive()).isFalse();
      assertThat(wrappedType.fullyQualifiedName()).isEqualTo("java.lang.Byte");
    }

    @Test
    void unrelated() {
      Type otherType = SEMA.type(SEMA.resolveType("java.lang.Object"));
      Type wrappedType = JUtils.wrapTypeIfPrimitive(otherType);
      assertThat(wrappedType).isNotNull();
      assertThat(wrappedType.isPrimitive()).isFalse();
      assertThat(wrappedType.fullyQualifiedName()).isEqualTo("java.lang.Object");
    }
  }
  @Nested
  class IsNullType {
    private final JavaTree.CompilationUnitTreeImpl cu = test("class C { Object m1() { return null; } Unknown m2() { return null; } }");
    private final ClassTreeImpl c = firstClass(cu);
    private final MethodTreeImpl m1 = firstMethod(c);

    @Test
    void nullType() {
      ReturnStatementTreeImpl s = (ReturnStatementTreeImpl) m1.block().body().get(0);
      AbstractTypedTree e = (AbstractTypedTree) s.expression();
      assertThat(cu.sema.type(e.typeBinding).isNullType())
        .isEqualTo(e.symbolType().isNullType())
        .isTrue();
    }

    @Test
    void a_non_null_type_is_not_null_type() {
      assertThat(cu.sema.type(m1.methodBinding.getReturnType()).isNullType())
        .isEqualTo(m1.returnType().symbolType().isNullType())
        .isFalse();
    }

    @Test
    void unresolved_type_is_not_null_type() {
      MethodTreeImpl m2 = nthMethod(c, 1);
      assertThat(cu.sema.type(m2.methodBinding.getReturnType()).isNullType())
        .isEqualTo(m2.returnType().symbolType().isNullType())
        .isFalse();
    }
  }

  @Nested
  class IsIntersectionType {
    private final JavaTree.CompilationUnitTreeImpl cu = test(
      "import java.io.Serializable;\n"
        + "import java.util.Comparator;\n"
        + "\n"
        + "class C {\n"
        + "  Serializable f = (Comparator<Object> & Serializable) (o1, o2) -> o1.toString().compareTo(o2.toString());\n"
        + "  Unknown u;\n"
        + "}");
    private final ClassTreeImpl c = firstClass(cu);
    private final VariableTreeImpl f = firstField(c);

    @Test
    void intersection_type() {
      TypeCastExpressionTreeImpl e = (TypeCastExpressionTreeImpl) f.initializer();
      assertThat(JUtils.isIntersectionType(cu.sema.type(e.typeBinding)))
        .isEqualTo(JUtils.isIntersectionType(e.symbolType()))
        .isTrue();
    }

    @Test
    void non_intersection_type() {
      assertThat(JUtils.isIntersectionType(cu.sema.type(f.variableBinding.getType())))
        .isEqualTo(JUtils.isIntersectionType(f.symbol().type()))
        .isFalse();
    }

    @Test
    void unresolved_type_is_not_an_intersection_type() {
      VariableTreeImpl u = nthField(c, 1);
      assertThat(JUtils.isIntersectionType(cu.sema.type(u.variableBinding.getType())))
        .isEqualTo(JUtils.isIntersectionType(u.symbol().type()))
        .isFalse();
    }
  }

  @Nested
  class IsTypeVar {
    private final JavaTree.CompilationUnitTreeImpl cu = test("class C<T> { T t; Unknown u; }");
    private final ClassTreeImpl c = firstClass(cu);

    @Test
    void type_var() {
      VariableTreeImpl t = firstField(c);
      assertThat(cu.sema.type(t.variableBinding.getType()).isTypeVar())
        .isEqualTo(t.symbol().type().isTypeVar())
        .isTrue();
    }

    @Test
    void simple_type_is_not_a_type_var() {
      assertThat(OBJECT_TYPE.isTypeVar()).isFalse();
    }

    @Test
    void unresolved_type_is_not_a_type_var() {
      VariableTreeImpl u = nthField(c, 1);
      assertThat(cu.sema.type(u.variableBinding.getType()).isTypeVar())
        .isEqualTo(u.symbol().type().isTypeVar())
        .isFalse();
    }
  }

  @Test
  void effectivelyFinal() {
    JavaTree.CompilationUnitTreeImpl cu = test("class A { void foo(Object o) { int i = 42; int j = 43; j++; foo(i); } }");
    ClassTreeImpl a = firstClass(cu);
    MethodTreeImpl m = firstMethod(a);
    List<StatementTree> body = m.block().body();
    VariableTreeImpl i = (VariableTreeImpl) body.get(0);
    VariableTreeImpl j = (VariableTreeImpl) body.get(1);

    assertThat(i.symbol().isVariableSymbol()).isTrue();
    assertThat(((Symbol.VariableSymbol) i.symbol()).isEffectivelyFinal()).isTrue();

    assertThat(j.symbol().isVariableSymbol()).isTrue();
    assertThat(((Symbol.VariableSymbol) j.symbol()).isEffectivelyFinal()).isFalse();
  }

  @Test
  void placeholder_are_never_effectivelyFinal() {
    JavaTree.CompilationUnitTreeImpl cu = test("class A { void foo() { \"\".substring(1); } }");
    ClassTreeImpl a = firstClass(cu);
    MethodTreeImpl m = firstMethod(a);
    ExpressionStatementTreeImpl es = (ExpressionStatementTreeImpl) m.block().body().get(0);
    MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) es.expression();
    Symbol.MethodSymbol methodSymbol = mit.methodSymbol();
    Symbol symbol = methodSymbol.declarationParameters().get(0);
    assertThat(symbol.isVariableSymbol()).isTrue();
    assertThat(((Symbol.VariableSymbol) symbol).isEffectivelyFinal()).isFalse();
  }

  @Nested
  class IsLocalVariable {
    private final JavaTree.CompilationUnitTreeImpl cu = test("class C {\n"
      + "  static { int value; }\n"
      + "  Object field;\n"
      + "  void m() { String localVariable; }\n"
      + "}");
    private final ClassTreeImpl c = firstClass(cu);

    @Test
    void local_variable() {
      MethodTreeImpl m = nthMethod(c, 2);
      VariableTreeImpl localVariable = (VariableTreeImpl) m.block().body().get(0);
      assertThat(localVariable.symbol().isLocalVariable()).isTrue();
    }

    @Test
    void variable_from_initializer_is_local_variable() {
      BlockTree staticInitializer = (BlockTree) c.members().get(0);
      VariableTreeImpl v = (VariableTreeImpl) staticInitializer.body().get(0);
      assertThat(v.symbol().isLocalVariable()).isTrue();
    }

    @Test
    void field_is_not_a_local_variable() {
      VariableTreeImpl field = nthField(c, 1);
      assertThat(field.symbol().isLocalVariable()).isFalse();
    }

    @Test
    void type_symbol_is_not_a_local_variable() {
      assertThat(c.symbol().isLocalVariable()).isFalse();
    }
  }

  @Nested
  class IsParameter {
    private final JavaTree.CompilationUnitTreeImpl cu = test("class C {\n"
      + "  Object field;\n"
      + "  void m(Object p) {\n"
      + "    String localVariable;\n"
      + "    m(this);\n"
      + "    \"\".substring(1);\n"
      + "  }\n"
      + "}");
    private final ClassTreeImpl c = firstClass(cu);
    private final MethodTreeImpl m = nthMethod(c, 1);

    @Test
    void field_is_not_parameter() {
      VariableTreeImpl field = firstField(c);
      assertThat(field.symbol().isParameter()).isFalse();
    }

    @Test
    void local_variable_is_not_parameter() {
      VariableTreeImpl localVariable = (VariableTreeImpl) m.block().body().get(0);
      assertThat(localVariable.symbol().isParameter()).isFalse();
    }

    @Test
    void parameter() {
      VariableTreeImpl p = (VariableTreeImpl) m.parameters().get(0);
      assertThat(p.symbol().isParameter()).isTrue();
    }

    @Test
    void not_a_variable_is_not_a_parameter() {
      assertThat(OBJECT_TYPE.symbol().isParameter()).isFalse();
    }

    @Test
    void this_is_not_a_parameter() {
      ExpressionStatementTreeImpl es = (ExpressionStatementTreeImpl) m.block().body().get(1);
      MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) es.expression();
      IdentifierTreeImpl arg0 = (IdentifierTreeImpl) mit.arguments().get(0);
      assertThat(arg0.symbol().isParameter()).isFalse();
    }

    @Test
    void placeholder_symbols_are_parameters() {
      ExpressionStatementTreeImpl es = (ExpressionStatementTreeImpl) m.block().body().get(2);
      MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) es.expression();
      Symbol.MethodSymbol symbol = mit.methodSymbol();
      assertThat(symbol.declarationParameters().get(0).isParameter()).isTrue();
    }
  }

  @Nested
  class ConstantValue {
    private final JavaTree.CompilationUnitTreeImpl cu = test("class C { static int field; void m(Object... os) { m(this); \"\".substring(1); } }");
    private final ClassTreeImpl c = firstClass(cu);
    private final MethodTreeImpl m = nthMethod(c, 1);

    @Test
    void this_can_not_be_evaluated() {
      ExpressionStatementTreeImpl es = (ExpressionStatementTreeImpl) m.block().body().get(0);
      MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) es.expression();
      IdentifierTreeImpl thisArg = (IdentifierTreeImpl) mit.arguments().get(0);
      assertThat(thisArg.symbol().isVariableSymbol()).isTrue();
      assertThat(((Symbol.VariableSymbol) thisArg.symbol()).constantValue()).isEmpty();
    }

    @Test
    void static_non_final_field_can_not_be_evaluated() {
      VariableTreeImpl field = firstField(c);
      assertThat(field.symbol().isVariableSymbol()).isTrue();
      assertThat(((Symbol.VariableSymbol) field.symbol()).constantValue()).isEmpty();
    }

    @Test
    void placeholders_can_not_be_evaluated() {
      ExpressionStatementTreeImpl es = (ExpressionStatementTreeImpl) m.block().body().get(1);
      MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) es.expression();
      Symbol.MethodSymbol methodSymbol = mit.methodSymbol();
      Symbol symbol = methodSymbol.declarationParameters().get(0);
      assertThat(symbol.isVariableSymbol()).isTrue();
      assertThat(((Symbol.VariableSymbol) symbol).constantValue()).isEmpty();
    }

    @Test
    void constantValue() {
      JavaTree.CompilationUnitTreeImpl cu = test("interface I { short SHORT = 42; char CHAR = 42; byte BYTE = 42; boolean BOOLEAN = false; }");
      ClassTreeImpl c = firstClass(cu);

      Symbol.VariableSymbol shortConstant = cu.sema.variableSymbol(firstField(c).variableBinding);
      assertThat(shortConstant.constantValue().orElseThrow(AssertionError::new))
        .isInstanceOf(Integer.class)
        .isEqualTo(42);

      Symbol.VariableSymbol charConstant = cu.sema.variableSymbol(nthField(c, 1).variableBinding);
      assertThat(charConstant.constantValue().orElseThrow(AssertionError::new))
        .isInstanceOf(Integer.class)
        .isEqualTo(42);

      Symbol.VariableSymbol byteConstant = cu.sema.variableSymbol(nthField(c, 2).variableBinding);
      assertThat(byteConstant.constantValue().orElseThrow(AssertionError::new))
        .isInstanceOf(Integer.class)
        .isEqualTo(42);

      Symbol.VariableSymbol booleanConstant = cu.sema.variableSymbol(nthField(c, 3).variableBinding);
      assertThat(booleanConstant.constantValue().orElseThrow(AssertionError::new))
        .isInstanceOf(Boolean.class)
        .isEqualTo(Boolean.FALSE);
    }
  }

  @Nested
  class SuperTypes {
    private final JavaTree.CompilationUnitTreeImpl cu = test(
      "class C implements java.io.Serializable, Unknown { Unknown u; }\n"
      + "abstract class B extends C implements java.util.List { }");
    private final ClassTreeImpl c = firstClass(cu);

    @Test
    void Objects_has_no_supertypes() {
      assertThat(OBJECT_TYPE.symbol().superTypes()).isEmpty();
    }

    @Test
    void unresolved_type_has_no_supertypes() {
      VariableTreeImpl u = firstField(c);
      assertThat(u.type().symbolType().symbol().superTypes()).isEmpty();
    }

    @Test
    void unresolved_types_are_also_part_of_supertypes() {
      Set<Type> superTypes = c.symbol().superTypes();
      assertThat(superTypes).hasSize(3);
      assertThat(superTypes.stream().map(Type::name)).containsOnly("Object", "Serializable", "Unknown");
    }

    @Test
    void supertypes_are_called_in_all_hierarchy() {
      ClassTreeImpl b = nthClass(cu, 1);
      Set<Type> superTypes = b.symbol().superTypes();

      if (Runtime.version().feature()>=21) {
        assertThat(superTypes).hasSize(8);
        assertThat(superTypes.stream().map(Type::name)).containsOnly("C", "Object", "Serializable",
          "Unknown", "List", "Collection", "SequencedCollection", "Iterable");
      } else {
        assertThat(superTypes).hasSize(7);
        assertThat(superTypes.stream().map(Type::name)).containsOnly("C", "Object", "Serializable",
          "Unknown", "List", "Collection", "Iterable");
      }
    }
  }

  @Test
  void outermostClass() {
    JavaTree.CompilationUnitTreeImpl cu = test("package org.foo; class A { class B { class C { } } }");
    ClassTreeImpl a = firstClass(cu);
    Symbol.TypeSymbol aTypeSymbol = a.symbol();
    ClassTreeImpl b = firstClass(a);
    ClassTreeImpl c = firstClass(b);

    assertThat(aTypeSymbol.outermostClass()).isSameAs(aTypeSymbol);
    assertThat(b.symbol().outermostClass()).isSameAs(aTypeSymbol);
    assertThat(c.symbol().outermostClass()).isSameAs(aTypeSymbol);
  }

  @Nested
  class GetPackage {
    @Test
    void from_package() {
      JavaTree.CompilationUnitTreeImpl cu = test("package org.foo; class A { }");
      ClassTreeImpl a = firstClass(cu);

      Symbol orgFooPackage = a.symbol().owner();
      assertThat(orgFooPackage.isPackageSymbol()).isTrue();
      assertThat(JUtils.getPackage(orgFooPackage)).isSameAs(orgFooPackage);
    }

    @Test
    void from_class() {
      JavaTree.CompilationUnitTreeImpl cu = test("package org.foo; class A { class B { class C { } } }");
      ClassTreeImpl a = firstClass(cu);
      ClassTreeImpl b = firstClass(a);
      ClassTreeImpl c = firstClass(b);

      Symbol orgFooPackage = a.symbol().owner();
      assertThat(orgFooPackage.isPackageSymbol()).isTrue();
      assertThat(JUtils.getPackage(a.symbol()))
        .isSameAs(JUtils.getPackage(b.symbol()))
        .isSameAs(JUtils.getPackage(c.symbol()))
        .isSameAs(orgFooPackage);
    }

    @Test
    void from_method() {
      JavaTree.CompilationUnitTreeImpl cu = test("package org.foo; class A { void m() {} }");
      ClassTreeImpl a = firstClass(cu);
      MethodTreeImpl m = firstMethod(a);

      Symbol orgFooPackage = a.symbol().owner();
      assertThat(orgFooPackage.isPackageSymbol()).isTrue();
      assertThat(JUtils.getPackage(a.symbol()))
        .isSameAs(JUtils.getPackage(m.symbol()))
        .isSameAs(orgFooPackage);
    }

    @Test
    void from_field() {
      JavaTree.CompilationUnitTreeImpl cu = test("package org.foo; class A { Object o; }");
      ClassTreeImpl a = firstClass(cu);
      VariableTreeImpl o = firstField(a);

      Symbol orgFooPackage = a.symbol().owner();
      assertThat(orgFooPackage.isPackageSymbol()).isTrue();
      assertThat(JUtils.getPackage(a.symbol()))
        .isSameAs(JUtils.getPackage(o.symbol()))
        .isSameAs(orgFooPackage);
    }

    @Test
    void from_unknown() {
      assertThat(JUtils.getPackage(Symbols.unknownSymbol)).isSameAs(Symbols.rootPackage);
    }
  }

  @Nested
  class IsVarArgsMethod {
    private final JavaTree.CompilationUnitTreeImpl cu = test("class A {\n"
      + "  void bar(Object ... os) { }\n"
      + "  void foo(Object o) { }\n"
      + "}");
    private final ClassTreeImpl a = firstClass(cu);

    @Test
    void isVarArgsMethod() {
      MethodTreeImpl varArgsMethod = firstMethod(a);
      assertThat(varArgsMethod.symbol().isVarArgsMethod()).isTrue();
    }

    @Test
    void non_vararg_method_is_not_varargs() {
      MethodTreeImpl nonVarArgsMethod = nthMethod(a, 1);
      assertThat(nonVarArgsMethod.symbol().isVarArgsMethod()).isFalse();
    }

    @Test
    void unknown_method_is_not_varargs() {
      assertThat(Symbols.unknownMethodSymbol.isVarArgsMethod()).isFalse();
    }
  }

  @Nested
  class IsSynchronizedMethod {
    private final JavaTree.CompilationUnitTreeImpl cu = test("class A {\n"
      + "  synchronized void foo() { }\n"
      + "  void bar() { }\n"
      + "}");
    private final ClassTreeImpl a = firstClass(cu);

    @Test
    void isSynchronizedMethod() {
      MethodTreeImpl synchronizedMethod = firstMethod(a);
      assertThat(synchronizedMethod.symbol().isSynchronizedMethod()).isTrue();
    }

    @Test
    void not_synchronized() {
      MethodTreeImpl nonSynchronizedMethod = nthMethod(a, 1);
      assertThat(nonSynchronizedMethod.symbol().isSynchronizedMethod()).isFalse();
    }

    @Test
    void unknown_method_is_not_synchronized() {
      assertThat(Symbols.unknownMethodSymbol.isSynchronizedMethod()).isFalse();
    }
  }

  @Nested
  class IsNativeMethod {
    private final JavaTree.CompilationUnitTreeImpl cu = test("class A {\n"
      + "  native void foo();\n"
      + "  void bar() { }\n"
      + "}");
    private final ClassTreeImpl a = firstClass(cu);

    @Test
    void isNativeMethod() {
      MethodTreeImpl nativeMethod = firstMethod(a);
      assertThat(JUtils.isNativeMethod(nativeMethod.symbol())).isTrue();
    }

    @Test
    void not_native() {
      MethodTreeImpl nonNativeMethod = nthMethod(a, 1);
      assertThat(JUtils.isNativeMethod(nonNativeMethod.symbol())).isFalse();
    }

    @Test
    void unknown_method_is_not_native() {
      assertThat(JUtils.isNativeMethod(Symbols.unknownMethodSymbol)).isFalse();
    }
  }

  @Nested
  class IsDefaultMethod {
    private final JavaTree.CompilationUnitTreeImpl cu = test("interface A {\n"
      + "  default void foo() {}\n"
      + "  void bar();\n"
      + "}");
    private final ClassTreeImpl a = firstClass(cu);

    @Test
    void isDefaultMethod() {
      MethodTreeImpl defaultMethod = firstMethod(a);
      assertThat(defaultMethod.symbol().isDefaultMethod()).isTrue();
    }

    @Test
    void not_default() {
      MethodTreeImpl nonDefaultMethod = nthMethod(a, 1);
      assertThat(nonDefaultMethod.symbol().isDefaultMethod()).isFalse();
    }

    @Test
    void unknown_method_is_not_default() {
      assertThat(Symbols.unknownMethodSymbol.isDefaultMethod()).isFalse();
    }
  }

  @Nested
  class DefaultValue {
    private final JavaTree.CompilationUnitTreeImpl cu = test("@interface A {\n"
      + "  int foo() default 42;\n"
      + "  String bar();\n"
      + "}");
    private final ClassTreeImpl a = firstClass(cu);

    @Test
    void defaultValue() {
      MethodTreeImpl defaultValueMethod = firstMethod(a);
      assertThat(JUtils.defaultValue(defaultValueMethod.symbol())).isEqualTo(42);
    }

    @Test
    void no_default_value() {
      MethodTreeImpl nonDefaultValueMethod = nthMethod(a, 1);
      assertThat(JUtils.defaultValue(nonDefaultValueMethod.symbol())).isNull();
    }

    @Test
    void unknown_method_has_no_default_value() {
      assertThat(JUtils.defaultValue(Symbols.unknownMethodSymbol)).isNull();
    }
  }

  @Nested
  class IsOverridable {
    private final JavaTree.CompilationUnitTreeImpl cu = test("class A {\n"
      + "  void foo() {}\n"
      + "  private void bar() {}\n"
      + "  static void qix() {}\n"
      + "  final void gul() {}\n"
      + "}\n"
      + "record B() {\n"
      + "  void foo() {}\n"
      + "}");
    private final ClassTreeImpl a = firstClass(cu);

    @Test
    void isOverridable() {
      MethodTreeImpl foo = firstMethod(a);
      assertThat(foo.symbol().isOverridable()).isTrue();
    }

    @Test
    void private_method_is_not_overridable() {
      Symbol.MethodSymbol symbol = nthMethod(a, 1).symbol();
      assertThat(symbol.isPrivate()).isTrue();
      assertThat(symbol.isOverridable()).isFalse();
    }

    @Test
    void static_method_is_not_overridable() {
      Symbol.MethodSymbol symbol = nthMethod(a, 2).symbol();
      assertThat(symbol.isStatic()).isTrue();
      assertThat(symbol.isOverridable()).isFalse();
    }

    @Test
    void final_method_is_not_overridable() {
      Symbol.MethodSymbol symbol = nthMethod(a, 3).symbol();
      assertThat(symbol.isFinal()).isTrue();
      assertThat(symbol.isOverridable()).isFalse();
    }

    // final owner
    @Test
    void method_from_record_is_not_overridable() {
      ClassTreeImpl b = nthClass(cu, 1);
      Symbol.MethodSymbol symbol = (firstMethod(b)).symbol();
      assertThat(symbol.isFinal()).isFalse();
      assertThat(symbol.isOverridable()).isFalse();
    }

    @Test
    void unknown_method_is_not_overridable() {
      assertThat(Symbols.unknownMethodSymbol.isOverridable()).isFalse();
    }
  }

  @Nested
  class IsParametrizedMethod {
    private final JavaTree.CompilationUnitTreeImpl cu = test("class C {\n"
      + "  <T> void m(T p) { m(42); }\n"
      + "  void n() {}\n"
      + " }");
    private final ClassTreeImpl c = firstClass(cu);

    @Test
    void isParametrizedMethod() {
      MethodTreeImpl m = firstMethod(c);
      ExpressionStatementTreeImpl s = (ExpressionStatementTreeImpl) m.block().body().get(0);
      MethodInvocationTreeImpl methodInvocation = (MethodInvocationTreeImpl) s.expression();

      assertThat(cu.sema.methodSymbol(m.methodBinding).isParametrizedMethod())
        .isEqualTo(m.symbol().isParametrizedMethod())
        .isEqualTo(m.methodBinding.isGenericMethod())
        .isTrue();
      assertThat(cu.sema.methodSymbol(methodInvocation.methodBinding).isParametrizedMethod())
        .isEqualTo(methodInvocation.methodSymbol().isParametrizedMethod())
        .isEqualTo(methodInvocation.methodBinding.isParameterizedMethod())
        .isTrue();
    }

    @Test
    void non_parametrized_method_is_not_parametrized() {
      MethodTreeImpl n = nthMethod(c, 1);
      assertThat(n.symbol().isParametrizedMethod()).isFalse();
    }

    @Test
    void unknown_method_is_not_parametrized() {
      assertThat(Symbols.unknownMethodSymbol.isParametrizedMethod()).isFalse();
    }
  }

  @Test
  void isRawType() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C {} class D<T> { void foo(D d, Unknown u) {} }");
    ClassTreeImpl c = firstClass(cu);

    ClassTreeImpl dGeneric = nthClass(cu, 1);
    MethodTreeImpl m = firstMethod(dGeneric);
    VariableTreeImpl dRaw = (VariableTreeImpl) m.parameters().get(0);
    VariableTreeImpl unknown = (VariableTreeImpl) m.parameters().get(1);

    assertThat(c.symbol().type().isRawType())
      .isSameAs(dGeneric.symbol().type().isRawType())
      .isSameAs(unknown.symbol().type().isRawType())
      .isFalse();

    assertThat(dRaw.type().symbolType().isRawType()).isTrue();
  }

  @Test
  void declaringType() {
    JavaTree.CompilationUnitTreeImpl cu = test("class C<T> { void foo(C d, Unknown u) {} }");
    ClassTreeImpl c = firstClass(cu);

    MethodTreeImpl m = firstMethod(c);
    VariableTreeImpl cRaw = (VariableTreeImpl) m.parameters().get(0);
    VariableTreeImpl unknown = (VariableTreeImpl) m.parameters().get(1);

    assertThat(unknown.symbol().type().declaringType())
      .isSameAs(unknown.symbol().type());

    assertThat(cRaw.type().symbolType().declaringType())
      .isSameAs(c.symbol().type().declaringType())
      .isSameAs(c.symbol().type());
  }

  @Nested
  class DirectSuperTypes {
    private final JavaTree.CompilationUnitTreeImpl cu = test(
      "class C implements java.io.Serializable, Unknown { Unknown u; }\n"
      + "abstract class B extends C implements java.util.List { }");

    @Test
    void unkown_type_has_no_direct_supertypes() {
      assertThat(JUtils.directSuperTypes(Symbols.unknownType)).isEmpty();
    }

    @Test
    void object_type_has_no_direct_supertypes() {
      assertThat(JUtils.directSuperTypes(OBJECT_TYPE)).isEmpty();
    }

    @Test
    void direct_supertypes_might_be_equal_to_all_supertypes() {
      ClassTreeImpl c = firstClass(cu);
      Set<Type> directSuperTypes = JUtils.directSuperTypes(c.symbol().type());
      Set<Type> allSuperTypes = c.symbol().superTypes();
      assertThat(directSuperTypes)
        .hasSize(3)
        .isEqualTo(allSuperTypes);
      assertThat(directSuperTypes.stream().map(Type::name)).containsOnly("Object", "Serializable", "Unknown");
    }

    @Test
    void direct_supertypes_is_generaly_a_subset_of_all_supertypes() {
      ClassTreeImpl b = nthClass(cu, 1);
      Set<Type> directSuperTypes = JUtils.directSuperTypes(b.symbol().type());
      Set<Type> allSuperTypes = b.symbol().superTypes();
      assertThat(directSuperTypes).hasSize(2);
      assertThat(directSuperTypes.stream().map(Type::name)).containsOnly("C", "List");
      assertThat(allSuperTypes)
        .hasSizeBetween(7, 8)
        .containsAll(directSuperTypes);
    }
  }

  @Nested
  class EnclosingClass {
    private final JavaTree.CompilationUnitTreeImpl cu = test("package org.foo;\n"
      + "class A {\n"
      + "  class B {\n"
      + "    void foo() { }\n"
      + "    Object o;\n"
      + "  }\n"
      + "}");

    @Test
    void enclosingClass_from_compilation_unit_is_null() {
      assertThat(JUtils.enclosingClass(cu)).isNull();
    }

    @Test
    void enclosingClass_from_a_class_is_itself() {
      ClassTreeImpl a = firstClass(cu);
      assertThat(JUtils.enclosingClass(a)).isSameAs(a.symbol());

      ClassTreeImpl b = firstClass(a);
      assertThat(JUtils.enclosingClass(b)).isSameAs(b.symbol());
    }

    @Test
    void enclosingClass_from_any_other_tree_is_first_parent_class() {
      ClassTreeImpl a = firstClass(cu);
      ClassTreeImpl b = firstClass(a);

      MethodTreeImpl foo = firstMethod(b);
      assertThat(JUtils.enclosingClass(foo)).isSameAs(b.symbol());

      VariableTreeImpl o = nthField(b, 1);
      assertThat(JUtils.enclosingClass(o)).isSameAs(b.symbol());
    }
  }

  @Nested
  class ImporTreeSymbol {

    private final JavaTree.CompilationUnitTreeImpl cu = test("import java.util.List;\n"
      + "import org.foo.Unknown;\n"
      + "class A {}");

    @Test
    void resolved_imports_have_type_symbols() {
      ImportTree listImport = (ImportTree) cu.imports().get(0);
      Symbol listImportSymbol = listImport.symbol();

      assertThat(listImportSymbol).isNotNull();
      assertThat(listImportSymbol.isTypeSymbol()).isTrue();
      assertThat(listImportSymbol.type().fullyQualifiedName()).isEqualTo("java.util.List");
    }

    @Test
    void unresolved_imports_have_unknown_symbols() {
      ImportTree unknownImport = (ImportTree) cu.imports().get(1);
      Symbol unknownImportSymbol = unknownImport.symbol();

      assertThat(unknownImportSymbol).isNotNull();
      assertThat(unknownImportSymbol.isUnknown()).isTrue();
    }
  }

  @Test
  void typeParameterTreeSymbol() {
    JavaTree.CompilationUnitTreeImpl cu = test("class A<T> { }");
    ClassTreeImpl a = firstClass(cu);
    TypeParameterTree t = a.typeParameters().get(0);

    Symbol symbol = t.symbol();
    assertThat(symbol).isNotNull();
    assertThat(symbol.type().isTypeVar()).isTrue();
  }

  @Test
  void parameterAnnotations() {
    assertThat(JUtils.parameterAnnotations(Symbols.unknownMethodSymbol, 42)).isEqualTo(Symbols.EMPTY_METADATA);

    JavaTree.CompilationUnitTreeImpl cu = test("package org.foo; class A { void m(@MyAnnotation Object o) { } } @interface MyAnnotation {}");
    ClassTreeImpl a = firstClass(cu);
    MethodTreeImpl m = firstMethod(a);

    SymbolMetadata parameterAnnotations = JUtils.parameterAnnotations(m.symbol(), 0);
    assertThat(parameterAnnotations.annotations()).hasSize(1);
    assertThat(parameterAnnotations.isAnnotatedWith("org.foo.MyAnnotation")).isTrue();
    assertThat(parameterAnnotations.valuesForAnnotation("org.foo.MyAnnotation")).isNotNull();
  }

  @Test
  void test_has_unknown_type_in_hierarchy_with_unexpected_null_owner() {
    JInitializerBlockSymbol method = new JInitializerBlockSymbol(null, true);
    assertThat(JUtils.hasUnknownTypePreventingOverrideResolution(method)).isTrue();
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

  private static ClassTreeImpl firstClass(JavaTree.CompilationUnitTreeImpl cu) {
    return nthClass(cu, 0);
  }

  private static ClassTreeImpl nthClass(JavaTree.CompilationUnitTreeImpl cu, int n) {
    return (ClassTreeImpl) cu.types().get(n);
  }

  private static ClassTreeImpl firstClass(ClassTreeImpl classTree) {
    return nthClass(classTree, 0);
  }

  private static ClassTreeImpl nthClass(ClassTreeImpl classTree, int n) {
    return (ClassTreeImpl) classTree.members().get(n);
  }

  private static MethodTreeImpl firstMethod(ClassTreeImpl classTree) {
    return nthMethod(classTree, 0);
  }

  private static MethodTreeImpl nthMethod(ClassTreeImpl classTree, int n) {
    return (MethodTreeImpl) classTree.members().get(n);
  }

  private static VariableTreeImpl firstField(ClassTreeImpl classTree) {
    return nthField(classTree, 0);
  }

  private static VariableTreeImpl nthField(ClassTreeImpl classTree, int n) {
    return (VariableTreeImpl) classTree.members().get(n);
  }
}
