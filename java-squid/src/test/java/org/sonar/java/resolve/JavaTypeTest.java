/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.resolve;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class JavaTypeTest {

  private Symbols symbols = new Symbols(new BytecodeCompleter(Lists.<File>newArrayList(), new ParametrizedTypeCache()));

  @Test
  public void test_order_of_tags() {
    assertThat(JavaType.BYTE).isLessThan(JavaType.CHAR);
    assertThat(JavaType.CHAR).isLessThan(JavaType.SHORT);
    assertThat(JavaType.SHORT).isLessThan(JavaType.INT);
    assertThat(JavaType.INT).isLessThan(JavaType.LONG);
    assertThat(JavaType.LONG).isLessThan(JavaType.FLOAT);
    assertThat(JavaType.FLOAT).isLessThan(JavaType.DOUBLE);
    assertThat(JavaType.DOUBLE).isLessThan(JavaType.BOOLEAN);
    assertThat(JavaType.BOOLEAN).isLessThan(JavaType.VOID);
    assertThat(JavaType.VOID).isLessThan(JavaType.CLASS);
    assertThat(JavaType.CLASS).isLessThan(JavaType.ARRAY);
  }

  @Test
  public void checkTagging() {
    assertThat(new JavaType(JavaType.VOID, null).isTagged(JavaType.VOID)).isTrue();
  }

  @Test
  public void isNumerical_should_return_true_for_numerical_types() {
    assertThat(new JavaType(JavaType.BYTE, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.CHAR, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.SHORT, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.INT, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.LONG, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.FLOAT, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.DOUBLE, null).isNumerical()).isTrue();
    assertThat(new JavaType(JavaType.BOOLEAN, null).isNumerical()).isFalse();
    assertThat(new JavaType(JavaType.VOID, null).isNumerical()).isFalse();
    assertThat(new JavaType(JavaType.CLASS, null).isNumerical()).isFalse();
  }

  @Test
  public void to_string_on_type() throws Exception {
    assertThat(new JavaType(JavaType.VOID, null).toString()).isEmpty();
    String methodToString = new JavaType.MethodJavaType(ImmutableList.<JavaType>of(), new Symbols(new BytecodeCompleter(Lists.<File>newArrayList(), new ParametrizedTypeCache())).intType,
      ImmutableList.<JavaType>of(), null).toString();
    assertThat(methodToString).isEqualTo("returns int");
  }

  @Test
  public void type_is_fully_qualified_name() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    JavaSymbol.TypeJavaSymbol typeSymbol2 = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", symbols.rootPackage);
    JavaType.ArrayJavaType arrayType = new JavaType.ArrayJavaType(typeSymbol.type, symbols.arrayClass);
    JavaType.ClassJavaType classType = (JavaType.ClassJavaType) typeSymbol.type;
    classType.interfaces = Lists.newArrayList();
    assertThat(symbols.byteType.is("byte")).isTrue();
    assertThat(symbols.byteType.is("int")).isFalse();
    assertThat(classType.is("org.foo.bar.MyType")).isTrue();
    assertThat(typeSymbol2.type.is("MyType")).isTrue();
    assertThat(classType.is("org.foo.bar.SomeClass")).isFalse();
    assertThat(arrayType.is("org.foo.bar.MyType[]")).isTrue();
    assertThat(arrayType.is("org.foo.bar.MyType")).isFalse();
    assertThat(arrayType.is("org.foo.bar.SomeClass[]")).isFalse();
    assertThat(symbols.nullType.is("org.foo.bar.SomeClass")).isTrue();
    assertThat(symbols.unknownType.is("org.foo.bar.SomeClass")).isFalse();
  }

  @Test
  public void isPrimitive() {
    assertThat(new JavaType(JavaType.BYTE, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.CHAR, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.SHORT, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.INT, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.LONG, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.FLOAT, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.DOUBLE, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.BOOLEAN, null).isPrimitive()).isTrue();
    assertThat(new JavaType(JavaType.VOID, null).isPrimitive()).isFalse();
    assertThat(new JavaType(JavaType.ARRAY, null).isPrimitive()).isFalse();
    assertThat(new JavaType(JavaType.CLASS, null).isPrimitive()).isFalse();


    //Test primitive type
    for (org.sonar.plugins.java.api.semantic.Type.Primitives primitive : org.sonar.plugins.java.api.semantic.Type.Primitives.values()) {
      assertThat(symbols.charType.isPrimitive(primitive)).isEqualTo(primitive.equals(org.sonar.plugins.java.api.semantic.Type.Primitives.CHAR));
    }

  }

  @Test
  public void isSubtypeOf() throws Exception {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    JavaSymbol.TypeVariableJavaSymbol typeVariableSymbol = new JavaSymbol.TypeVariableJavaSymbol("T", typeSymbol);
    JavaType.ClassJavaType classType = (JavaType.ClassJavaType) typeSymbol.type;
    JavaType.TypeVariableJavaType typeVariableType = (JavaType.TypeVariableJavaType) typeVariableSymbol.type;
    JavaType.ArrayJavaType arrayType = new JavaType.ArrayJavaType(typeSymbol.type, symbols.arrayClass);
    typeVariableType.bounds = Lists.newArrayList(symbols.objectType);

    classType.supertype = symbols.objectType;
    classType.interfaces = Lists.newArrayList(symbols.cloneableType);
    assertThat(classType.isSubtypeOf("java.lang.Object")).isTrue();
    assertThat(classType.isSubtypeOf(symbols.objectType)).isTrue();

    assertThat(classType.isSubtypeOf("org.foo.bar.MyType")).isTrue();
    assertThat(classType.isSubtypeOf(typeSymbol.type)).isTrue();

    assertThat(classType.isSubtypeOf("java.lang.CharSequence")).isFalse();
    assertThat(classType.isSubtypeOf(symbols.stringType)).isFalse();

    assertThat(classType.isSubtypeOf("java.lang.Cloneable")).isTrue();
    assertThat(classType.isSubtypeOf(symbols.cloneableType)).isTrue();
    assertThat(new JavaType(JavaType.BYTE, null).isSubtypeOf("java.lang.Object")).isFalse();

    assertThat(arrayType.isSubtypeOf("org.foo.bar.MyType[]")).isTrue();
    assertThat(arrayType.isSubtypeOf(new JavaType.ArrayJavaType(typeSymbol.type, symbols.arrayClass))).isTrue();

    assertThat(arrayType.isSubtypeOf("org.foo.bar.MyType")).isFalse();
    assertThat(arrayType.isSubtypeOf(typeSymbol.type)).isFalse();

    assertThat(arrayType.isSubtypeOf("java.lang.Object[]")).isTrue();
    assertThat(arrayType.isSubtypeOf(new JavaType.ArrayJavaType(symbols.objectType, symbols.arrayClass))).isTrue();

    assertThat(arrayType.isSubtypeOf("java.lang.Object")).isTrue();
    assertThat(arrayType.isSubtypeOf(symbols.objectType)).isTrue();

    assertThat(symbols.nullType.isSubtypeOf(symbols.objectType)).isTrue();
    assertThat(symbols.nullType.isSubtypeOf("java.lang.Object")).isTrue();
    assertThat(symbols.objectType.isSubtypeOf(symbols.nullType)).isFalse();

    assertThat(symbols.nullType.isSubtypeOf(arrayType)).isTrue();
    assertThat(arrayType.isSubtypeOf(symbols.nullType)).isFalse();
    assertThat(symbols.nullType.isSubtypeOf(symbols.nullType)).isTrue();

    assertThat(arrayType.isSubtypeOf("org.foo.bar.SomeClass[]")).isFalse();

    assertThat(typeVariableType.isSubtypeOf("java.lang.Object")).isTrue();
    assertThat(typeVariableType.is("java.lang.Object")).isFalse();
    assertThat(typeVariableType.isSubtypeOf("java.lang.CharSequence")).isFalse();

    assertThat(Symbols.unknownType.is("java.lang.Object")).isFalse();
    assertThat(Symbols.unknownType.isSubtypeOf("java.lang.CharSequence")).isFalse();
    assertThat(Symbols.unknownType.isSubtypeOf(symbols.objectType)).isFalse();
  }

  @Test
  public void is_primitive_wrapper() {
    for (JavaType wrapper : symbols.boxedTypes.values()) {
      assertThat(wrapper.isPrimitiveWrapper()).isTrue();
    }
    assertThat(symbols.objectType.isPrimitiveWrapper()).isFalse();
    assertThat(symbols.intType.isPrimitiveWrapper()).isFalse();
  }

  @Test
  public void mapping_wrapper_primitive() {
    for (JavaType wrapper : symbols.boxedTypes.values()) {
      assertThat(wrapper.primitiveType()).isNotNull();
      assertThat(wrapper.primitiveWrapperType()).isNull();
    }
    for (JavaType primitive : symbols.boxedTypes.keySet()) {
      assertThat(primitive.primitiveType()).isNull();
      assertThat(primitive.primitiveWrapperType()).isNotNull();
    }
    assertThat(symbols.objectType.primitiveType()).isNull();
    assertThat(symbols.objectType.primitiveWrapperType()).isNull();
  }

  @Test
  public void parametrizedTypeType_methods_tests() {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    JavaSymbol.TypeVariableJavaSymbol typeVariableSymbol = new JavaSymbol.TypeVariableJavaSymbol("E", typeSymbol);
    JavaType.ClassJavaType classType = (JavaType.ClassJavaType) typeSymbol.type;
    JavaType.TypeVariableJavaType typeVariableType = (JavaType.TypeVariableJavaType) typeVariableSymbol.type;
    TypeSubstitution typeSubstitution = new TypeSubstitution();
    typeSubstitution.add(typeVariableType, classType);

    JavaType.ParametrizedTypeJavaType ptt = new JavaType.ParametrizedTypeJavaType(typeSymbol, typeSubstitution);
    assertThat(ptt.substitution(typeVariableType)).isEqualTo(classType);
    assertThat(ptt.substitution(new JavaType.TypeVariableJavaType(new JavaSymbol.TypeVariableJavaSymbol("F", typeSymbol)))).isNull();
    assertThat(ptt.typeParameters()).hasSize(1);
    assertThat(ptt.typeParameters()).contains(typeVariableType);

    ptt = new JavaType.ParametrizedTypeJavaType(typeSymbol, null);
    assertThat(ptt.substitution(typeVariableType)).isNull();
    assertThat(ptt.typeParameters()).isEmpty();
  }

  @Test
  public void fully_qualified_name() throws Exception {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    JavaSymbol.TypeJavaSymbol rootPackageTypeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType2", symbols.defaultPackage);
    assertThat(typeSymbol.type.fullyQualifiedName()).isEqualTo("org.foo.bar.MyType");
    assertThat(rootPackageTypeSymbol.type.fullyQualifiedName()).isEqualTo("MyType2");
  }

  @Test
  public void is_class_is_array() throws Exception {
    JavaSymbol.PackageJavaSymbol packageSymbol = new JavaSymbol.PackageJavaSymbol("org.foo.bar", null);
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    JavaType.ArrayJavaType arrayType = new JavaType.ArrayJavaType(typeSymbol.type, symbols.arrayClass);

    assertThat(typeSymbol.type.isClass()).isTrue();
    assertThat(typeSymbol.type.isArray()).isFalse();
    assertThat(arrayType.isClass()).isFalse();
    assertThat(arrayType.isArray()).isTrue();

  }
}
