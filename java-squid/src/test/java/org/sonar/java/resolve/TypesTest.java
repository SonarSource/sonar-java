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

import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class TypesTest {

  private Symbols symbols = new Symbols(new BytecodeCompleter(Lists.<File>newArrayList(), new ParametrizedTypeCache()));
  private Types types = new Types();

  @Test
  public void isSubtype() {
    // byte is direct subtype of short
    shouldNotBeSubtype(symbols.byteType, Arrays.asList(symbols.booleanType, symbols.charType));
    shouldBeSubtype(symbols.byteType, Arrays.asList(symbols.byteType, symbols.shortType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    // char is direct subtype of int
    shouldNotBeSubtype(symbols.charType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.shortType));
    shouldBeSubtype(symbols.charType, Arrays.asList(symbols.charType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    // short is direct subtype of int
    shouldNotBeSubtype(symbols.shortType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType));
    shouldBeSubtype(symbols.shortType, Arrays.asList(symbols.shortType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    // int is direct subtype of long
    shouldNotBeSubtype(symbols.intType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType));
    shouldBeSubtype(symbols.intType, Arrays.asList(symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    // long is direct subtype of float
    shouldNotBeSubtype(symbols.longType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType, symbols.intType));
    shouldBeSubtype(symbols.longType, Arrays.asList(symbols.longType, symbols.floatType, symbols.doubleType));
    // float is direct subtype of double
    shouldNotBeSubtype(symbols.floatType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType, symbols.intType, symbols.longType));
    shouldBeSubtype(symbols.floatType, Arrays.asList(symbols.floatType, symbols.doubleType));
    // double
    shouldNotBeSubtype(symbols.doubleType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType, symbols.intType, symbols.longType, symbols.floatType));
    shouldBeSubtype(symbols.doubleType, Arrays.asList(symbols.doubleType));
    // boolean
    shouldNotBeSubtype(symbols.booleanType, Arrays.asList(symbols.byteType, symbols.charType, symbols.shortType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    shouldBeSubtype(symbols.booleanType, Arrays.asList(symbols.booleanType));

    // TODO test void

    // null
    Type.ArrayType arrayTypeInt = new Type.ArrayType(symbols.intType, symbols.arrayClass);
    Type.ArrayType arrayTypeShort = new Type.ArrayType(symbols.shortType, symbols.arrayClass);
    shouldBeSubtype(arrayTypeShort, Arrays.<Type>asList(arrayTypeShort, arrayTypeInt));
    shouldNotBeSubtype(symbols.nullType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    shouldBeSubtype(symbols.nullType, Arrays.asList(symbols.nullType, arrayTypeInt, symbols.objectType));
    shouldBeSubtype(arrayTypeInt, Arrays.asList(symbols.objectType));
    Symbol.TypeSymbol typeSymbol = new Symbol.TypeSymbol(Flags.PUBLIC, "MyType", symbols.defaultPackage);
    Type.ClassType classType = (Type.ClassType) typeSymbol.type;
    classType.interfaces = Lists.newArrayList();
    Symbol.TypeSymbol subtypeSymbol = new Symbol.TypeSymbol(Flags.PUBLIC, "MySubtype", symbols.defaultPackage);
    Type.ClassType subClassType = (Type.ClassType) subtypeSymbol.type;
    subClassType.supertype = classType;
    subClassType.interfaces = Lists.newArrayList();
    shouldBeSubtype(subClassType, Arrays.<Type>asList(classType, subClassType));

  }

  @Test
  public void array_types_equality() throws Exception {
    Type.ArrayType arrayInt= new Type.ArrayType(symbols.intType, symbols.arrayClass);
    Type.ArrayType arrayInt2= new Type.ArrayType(symbols.intType, symbols.arrayClass);
    Type.ArrayType arrayBoolean = new Type.ArrayType(symbols.booleanType, symbols.arrayClass);
    assertThat(arrayInt.equals(arrayInt2)).isTrue();
    assertThat(arrayInt2.equals(arrayInt)).isTrue();
    assertThat(arrayInt2.equals(arrayBoolean)).isFalse();
    assertThat(arrayInt2.equals(arrayInt2)).isTrue();
    assertThat(arrayInt2.equals(null)).isFalse();
    assertThat(arrayInt2.equals(symbols.charType)).isFalse();

  }

  private void shouldNotBeSubtype(Type t, List<Type> s) {
    for (Type type : s) {
      assertThat(types.isSubtype(t, type)).as(t + " is subtype of " + type).isFalse();
    }
  }

  private void shouldBeSubtype(Type t, List<Type> s) {
    for (Type type : s) {
      assertThat(types.isSubtype(t, type)).as(t + " is subtype of " + type).isTrue();
    }
  }

}
