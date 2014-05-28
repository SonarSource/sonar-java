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

  private Symbols symbols = new Symbols(new BytecodeCompleter(Lists.<File>newArrayList()));
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
    Type.ArrayType arrayType = new Type.ArrayType(symbols.intType, symbols.arrayClass);
    shouldNotBeSubtype(symbols.nullType, Arrays.asList(symbols.booleanType, symbols.byteType, symbols.charType, symbols.shortType, symbols.intType, symbols.longType, symbols.floatType, symbols.doubleType));
    shouldBeSubtype(symbols.nullType, Arrays.asList(symbols.nullType, arrayType, symbols.objectType));
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
