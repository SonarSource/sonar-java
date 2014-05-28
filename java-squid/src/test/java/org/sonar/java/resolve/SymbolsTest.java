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

import static org.fest.assertions.Assertions.assertThat;

public class SymbolsTest {

  private Symbols symbols = new Symbols(new BytecodeCompleter(Lists.<File>newArrayList()));

  @Test
  public void root_package() {
    assertThat(symbols.rootPackage.name).isEqualTo("");
    assertThat(symbols.rootPackage.owner()).isNull();
  }

  @Test
  public void builtin_types() {
    assertThat(symbols.byteType.tag).isEqualTo(Type.BYTE);
    assertThat(symbols.byteType.symbol.name).isEqualTo("byte");
    assertThat(symbols.byteType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.byteType.symbol.owner()).isSameAs(symbols.rootPackage);

    assertThat(symbols.charType.tag).isEqualTo(Type.CHAR);
    assertThat(symbols.charType.symbol.name).isEqualTo("char");
    assertThat(symbols.charType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.charType.symbol.owner()).isSameAs(symbols.rootPackage);

    assertThat(symbols.shortType.tag).isEqualTo(Type.SHORT);
    assertThat(symbols.shortType.symbol.name).isEqualTo("short");
    assertThat(symbols.shortType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.shortType.symbol.owner()).isSameAs(symbols.rootPackage);

    assertThat(symbols.intType.tag).isEqualTo(Type.INT);
    assertThat(symbols.intType.symbol.name).isEqualTo("int");
    assertThat(symbols.intType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.intType.symbol.owner()).isSameAs(symbols.rootPackage);

    assertThat(symbols.longType.tag).isEqualTo(Type.LONG);
    assertThat(symbols.longType.symbol.name).isEqualTo("long");
    assertThat(symbols.longType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.longType.symbol.owner()).isSameAs(symbols.rootPackage);

    assertThat(symbols.floatType.tag).isEqualTo(Type.FLOAT);
    assertThat(symbols.floatType.symbol.name).isEqualTo("float");
    assertThat(symbols.floatType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.floatType.symbol.owner()).isSameAs(symbols.rootPackage);

    assertThat(symbols.doubleType.tag).isEqualTo(Type.DOUBLE);
    assertThat(symbols.doubleType.symbol.name).isEqualTo("double");
    assertThat(symbols.doubleType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.doubleType.symbol.owner()).isSameAs(symbols.rootPackage);

    assertThat(symbols.booleanType.tag).isEqualTo(Type.BOOLEAN);
    assertThat(symbols.booleanType.symbol.name).isEqualTo("boolean");
    assertThat(symbols.booleanType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.booleanType.symbol.owner()).isSameAs(symbols.rootPackage);

    assertThat(symbols.nullType.tag).isEqualTo(Type.BOT);
    assertThat(symbols.nullType.symbol.name).isEqualTo("<nulltype>");
    assertThat(symbols.nullType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.nullType.symbol.owner()).isSameAs(symbols.rootPackage);
  }

  @Test
  public void array_class() {
    assertThat(symbols.arrayClass.owner()).isSameAs(symbols.noSymbol);
    assertThat(symbols.arrayClass.name).isEqualTo("Array");
    assertThat(symbols.arrayClass.flags()).isEqualTo(Flags.PUBLIC);

    assertThat(symbols.arrayClass.members.lookup("length")).hasSize(1);
    Symbol.VariableSymbol lengthSymbol = (Symbol.VariableSymbol) symbols.arrayClass.members.lookup("length").get(0);
    assertThat(lengthSymbol.name).isEqualTo("length");
    assertThat(lengthSymbol.owner()).isSameAs(symbols.arrayClass);
    assertThat(lengthSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.FINAL);
    assertThat(lengthSymbol.type).isSameAs(symbols.intType);

    Type.ClassType arrayClassType = ((Type.ClassType) symbols.arrayClass.type);
    assertThat(arrayClassType.supertype).isSameAs(symbols.objectType);
    assertThat(arrayClassType.interfaces).containsOnly(symbols.cloneableType, symbols.serializableType);
  }

}
