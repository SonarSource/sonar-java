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
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class TypeTest {

  private Symbols symbols = new Symbols(new BytecodeCompleter(Lists.<File>newArrayList()));

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
    String methodToString  = new Type.MethodType(ImmutableList.<Type>of(), new Symbols(new BytecodeCompleter(Lists.<File>newArrayList())).intType, ImmutableList.<Type>of(), null).toString();
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
    assertThat(symbols.nullType.is("org.foo.bar.SomeClass")).isTrue();
    assertThat(symbols.unknownType.is("org.foo.bar.SomeClass")).isFalse();


  }
}
