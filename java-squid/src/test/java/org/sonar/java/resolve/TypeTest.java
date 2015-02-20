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
package org.sonar.java.resolve;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class TypeTest {

  private Symbols symbols = new Symbols(new BytecodeCompleter(Lists.<File>newArrayList(), new ParametrizedTypeCache()));

  @Test
  public void test_order_of_tags() {
    assertThat(Type.BYTE).isLessThan(Type.CHAR);
    assertThat(Type.CHAR).isLessThan(Type.SHORT);
    assertThat(Type.SHORT).isLessThan(Type.INT);
    assertThat(Type.INT).isLessThan(Type.LONG);
    assertThat(Type.LONG).isLessThan(Type.FLOAT);
    assertThat(Type.FLOAT).isLessThan(Type.DOUBLE);
    assertThat(Type.DOUBLE).isLessThan(Type.BOOLEAN);
    assertThat(Type.BOOLEAN).isLessThan(Type.VOID);
    assertThat(Type.VOID).isLessThan(Type.CLASS);
    assertThat(Type.CLASS).isLessThan(Type.ARRAY);
  }

  @Test
  public void checkTagging() {
    assertThat(new Type(Type.VOID, null).isTagged(Type.VOID)).isTrue();
  }

  @Test
  public void isNumerical_should_return_true_for_numerical_types() {
    assertThat(new Type(Type.BYTE, null).isNumerical()).isTrue();
    assertThat(new Type(Type.CHAR, null).isNumerical()).isTrue();
    assertThat(new Type(Type.SHORT, null).isNumerical()).isTrue();
    assertThat(new Type(Type.INT, null).isNumerical()).isTrue();
    assertThat(new Type(Type.LONG, null).isNumerical()).isTrue();
    assertThat(new Type(Type.FLOAT, null).isNumerical()).isTrue();
    assertThat(new Type(Type.DOUBLE, null).isNumerical()).isTrue();
    assertThat(new Type(Type.BOOLEAN, null).isNumerical()).isFalse();
    assertThat(new Type(Type.VOID, null).isNumerical()).isFalse();
    assertThat(new Type(Type.CLASS, null).isNumerical()).isFalse();
  }

  @Test
  public void to_string_on_type() throws Exception {
    assertThat(new Type(Type.VOID, null).toString()).isEmpty();
    String methodToString = new Type.MethodType(ImmutableList.<Type>of(), new Symbols(new BytecodeCompleter(Lists.<File>newArrayList(), new ParametrizedTypeCache())).intType,
      ImmutableList.<Type>of(), null).toString();
    assertThat(methodToString).isEqualTo("returns int");
  }

  @Test
  public void type_is_fully_qualified_name() {
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol("org.foo.bar", null);
    Symbol.TypeSymbol typeSymbol = new Symbol.TypeSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    Symbol.TypeSymbol typeSymbol2 = new Symbol.TypeSymbol(Flags.PUBLIC, "MyType", symbols.rootPackage);
    Type.ArrayType arrayType = new Type.ArrayType(typeSymbol.type, symbols.arrayClass);
    Type.ClassType classType = (Type.ClassType) typeSymbol.type;
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
    assertThat(new Type(Type.BYTE, null).isPrimitive()).isTrue();
    assertThat(new Type(Type.CHAR, null).isPrimitive()).isTrue();
    assertThat(new Type(Type.SHORT, null).isPrimitive()).isTrue();
    assertThat(new Type(Type.INT, null).isPrimitive()).isTrue();
    assertThat(new Type(Type.LONG, null).isPrimitive()).isTrue();
    assertThat(new Type(Type.FLOAT, null).isPrimitive()).isTrue();
    assertThat(new Type(Type.DOUBLE, null).isPrimitive()).isTrue();
    assertThat(new Type(Type.BOOLEAN, null).isPrimitive()).isTrue();
    assertThat(new Type(Type.VOID, null).isPrimitive()).isFalse();
    assertThat(new Type(Type.ARRAY, null).isPrimitive()).isFalse();
    assertThat(new Type(Type.CLASS, null).isPrimitive()).isFalse();
  }

  @Test
  public void isSubtypeOf() throws Exception {
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol("org.foo.bar", null);
    Symbol.TypeSymbol typeSymbol = new Symbol.TypeSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    Symbol.TypeVariableSymbol typeVariableSymbol = new Symbol.TypeVariableSymbol("T", typeSymbol);
    Type.ClassType classType = (Type.ClassType) typeSymbol.type;
    Type.TypeVariableType typeVariableType = (Type.TypeVariableType) typeVariableSymbol.type;
    Type.ArrayType arrayType = new Type.ArrayType(typeSymbol.type, symbols.arrayClass);
    typeVariableType.bounds = Lists.newArrayList(symbols.objectType);

    classType.supertype = symbols.objectType;
    classType.interfaces = Lists.newArrayList(symbols.cloneableType);
    assertThat(classType.isSubtypeOf("java.lang.Object")).isTrue();
    assertThat(classType.isSubtypeOf("org.foo.bar.MyType")).isTrue();
    assertThat(classType.isSubtypeOf("java.lang.CharSequence")).isFalse();
    assertThat(classType.isSubtypeOf("java.lang.Cloneable")).isTrue();
    assertThat(new Type(Type.BYTE, null).isSubtypeOf("java.lang.Object")).isFalse();

    assertThat(arrayType.isSubtypeOf("org.foo.bar.MyType[]")).isTrue();
    assertThat(arrayType.isSubtypeOf("org.foo.bar.MyType")).isFalse();
    assertThat(arrayType.isSubtypeOf("java.lang.Object[]")).isTrue();
    assertThat(arrayType.isSubtypeOf("org.foo.bar.SomeClass[]")).isFalse();

    assertThat(typeVariableType.isSubtypeOf("java.lang.Object")).isTrue();
    assertThat(typeVariableType.is("java.lang.Object")).isFalse();
    assertThat(typeVariableType.isSubtypeOf("java.lang.CharSequence")).isFalse();
  }

  @Test
  public void is_primitive_wrapper() {
    for (Type wrapper : symbols.boxedTypes.values()) {
      assertThat(wrapper.isPrimitiveWrapper()).isTrue();
    }
    assertThat(symbols.objectType.isPrimitiveWrapper()).isFalse();
    assertThat(symbols.intType.isPrimitiveWrapper()).isFalse();
  }

  @Test
  public void mapping_wrapper_primitive() {
    for (Type wrapper : symbols.boxedTypes.values()) {
      assertThat(wrapper.primitiveType()).isNotNull();
      assertThat(wrapper.primitiveWrapperType()).isNull();
    }
    for (Type primitive : symbols.boxedTypes.keySet()) {
      assertThat(primitive.primitiveType()).isNull();
      assertThat(primitive.primitiveWrapperType()).isNotNull();
    }
    assertThat(symbols.objectType.primitiveType()).isNull();
    assertThat(symbols.objectType.primitiveWrapperType()).isNull();
  }

  @Test
  public void parametrizedTypeType_methods_tests() {
    Symbol.PackageSymbol packageSymbol = new Symbol.PackageSymbol("org.foo.bar", null);
    Symbol.TypeSymbol typeSymbol = new Symbol.TypeSymbol(Flags.PUBLIC, "MyType", packageSymbol);
    Symbol.TypeVariableSymbol typeVariableSymbol = new Symbol.TypeVariableSymbol("E", typeSymbol);
    Type.ClassType classType = (Type.ClassType) typeSymbol.type;
    Type.TypeVariableType typeVariableType = (Type.TypeVariableType) typeVariableSymbol.type;
    Map<Type.TypeVariableType, Type> typeSubstitution = Maps.newHashMap();
    typeSubstitution.put(typeVariableType, classType);

    Type.ParametrizedTypeType ptt = new Type.ParametrizedTypeType(typeSymbol, typeSubstitution);
    assertThat(ptt.substitution(typeVariableType)).isEqualTo(classType);
    assertThat(ptt.substitution(new Type.TypeVariableType(new Symbol.TypeVariableSymbol("F", typeSymbol)))).isNull();
    assertThat(ptt.typeParameters()).hasSize(1);
    assertThat(ptt.typeParameters()).contains(typeVariableType);

    ptt = new Type.ParametrizedTypeType(typeSymbol, null);
    assertThat(ptt.substitution(typeVariableType)).isNull();
    assertThat(ptt.typeParameters()).isEmpty();
  }
}
