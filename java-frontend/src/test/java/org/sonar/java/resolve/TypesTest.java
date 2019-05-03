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
package org.sonar.java.resolve;

import org.junit.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TypesTest {

  private Symbols symbols = new Symbols(new BytecodeCompleter(new SquidClassLoader(Collections.emptyList()), new ParametrizedTypeCache()));
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
    ArrayJavaType arrayTypeInt = new ArrayJavaType(symbols.intType, symbols.arrayClass);
    ArrayJavaType arrayTypeShort = new ArrayJavaType(symbols.shortType, symbols.arrayClass);
    shouldBeSubtype(arrayTypeShort, Arrays.<JavaType>asList(arrayTypeShort, arrayTypeInt));
    shouldNotBeSubtype(symbols.nullType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    shouldBeSubtype(symbols.nullType, Arrays.asList(symbols.nullType, arrayTypeInt, symbols.objectType));
    shouldBeSubtype(arrayTypeInt, Arrays.asList(symbols.objectType));
    JavaSymbol.TypeJavaSymbol typeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MyType", symbols.defaultPackage);
    ClassJavaType classType = (ClassJavaType) typeSymbol.type;
    classType.interfaces = new ArrayList<>();
    JavaSymbol.TypeJavaSymbol subtypeSymbol = new JavaSymbol.TypeJavaSymbol(Flags.PUBLIC, "MySubtype", symbols.defaultPackage);
    ClassJavaType subClassType = (ClassJavaType) subtypeSymbol.type;
    subClassType.supertype = classType;
    subClassType.interfaces = new ArrayList<>();
    shouldBeSubtype(subClassType, Arrays.<JavaType>asList(classType, subClassType));

  }

  @Test
  public void array_types_equality() throws Exception {
    ArrayJavaType arrayInt= new ArrayJavaType(symbols.intType, symbols.arrayClass);
    ArrayJavaType arrayInt2= new ArrayJavaType(symbols.intType, symbols.arrayClass);
    ArrayJavaType arrayBoolean = new ArrayJavaType(symbols.booleanType, symbols.arrayClass);
    assertThat(arrayInt.equals(arrayInt2)).isTrue();
    assertThat(arrayInt2.equals(arrayInt)).isTrue();
    assertThat(arrayInt2.equals(arrayBoolean)).isFalse();
    assertThat(arrayInt2.equals(arrayInt2)).isTrue();
    assertThat(arrayInt2.equals(null)).isFalse();
    assertThat(arrayInt2.equals(symbols.charType)).isFalse();

  }

  private void shouldNotBeSubtype(JavaType t, List<JavaType> s) {
    for (JavaType type : s) {
      assertThat(types.isSubtype(t, type)).as(t + " is subtype of " + type).isFalse();
    }
  }

  private void shouldBeSubtype(JavaType t, List<JavaType> s) {
    for (JavaType type : s) {
      assertThat(types.isSubtype(t, type)).as(t + " is subtype of " + type).isTrue();
    }
  }

}
