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

import org.assertj.core.api.Fail;
import org.junit.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class SymbolsTest {

  private Symbols symbols = new Symbols(new BytecodeCompleter(new SquidClassLoader(Collections.emptyList()), new ParametrizedTypeCache()));

  @Test
  public void root_package() {
    assertThat(symbols.rootPackage.name).isEqualTo("");
    assertThat(symbols.rootPackage.owner()).isNull();
  }

  @Test
  public void builtin_types() {
    assertThat(symbols.byteType.tag).isEqualTo(JavaType.BYTE);
    assertThat(symbols.byteType.symbol.name).isEqualTo("byte");
    assertThat(symbols.byteType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.byteType.symbol.owner()).isSameAs(symbols.rootPackage);
    assertThat(symbols.byteType.primitiveType()).isNull();
    assertThat(symbols.byteType.primitiveWrapperType()).isNotNull();

    assertThat(symbols.charType.tag).isEqualTo(JavaType.CHAR);
    assertThat(symbols.charType.symbol.name).isEqualTo("char");
    assertThat(symbols.charType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.charType.symbol.owner()).isSameAs(symbols.rootPackage);
    assertThat(symbols.charType.primitiveType()).isNull();
    assertThat(symbols.charType.primitiveWrapperType()).isNotNull();

    assertThat(symbols.shortType.tag).isEqualTo(JavaType.SHORT);
    assertThat(symbols.shortType.symbol.name).isEqualTo("short");
    assertThat(symbols.shortType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.shortType.symbol.owner()).isSameAs(symbols.rootPackage);
    assertThat(symbols.shortType.primitiveType()).isNull();
    assertThat(symbols.shortType.primitiveWrapperType()).isNotNull();

    assertThat(symbols.intType.tag).isEqualTo(JavaType.INT);
    assertThat(symbols.intType.symbol.name).isEqualTo("int");
    assertThat(symbols.intType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.intType.symbol.owner()).isSameAs(symbols.rootPackage);
    assertThat(symbols.intType.primitiveType()).isNull();
    assertThat(symbols.intType.primitiveWrapperType()).isNotNull();

    assertThat(symbols.longType.tag).isEqualTo(JavaType.LONG);
    assertThat(symbols.longType.symbol.name).isEqualTo("long");
    assertThat(symbols.longType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.longType.symbol.owner()).isSameAs(symbols.rootPackage);
    assertThat(symbols.longType.primitiveType()).isNull();
    assertThat(symbols.longType.primitiveWrapperType()).isNotNull();

    assertThat(symbols.floatType.tag).isEqualTo(JavaType.FLOAT);
    assertThat(symbols.floatType.symbol.name).isEqualTo("float");
    assertThat(symbols.floatType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.floatType.symbol.owner()).isSameAs(symbols.rootPackage);
    assertThat(symbols.floatType.primitiveType()).isNull();
    assertThat(symbols.floatType.primitiveWrapperType()).isNotNull();

    assertThat(symbols.doubleType.tag).isEqualTo(JavaType.DOUBLE);
    assertThat(symbols.doubleType.symbol.name).isEqualTo("double");
    assertThat(symbols.doubleType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.doubleType.symbol.owner()).isSameAs(symbols.rootPackage);
    assertThat(symbols.doubleType.primitiveType()).isNull();
    assertThat(symbols.doubleType.primitiveWrapperType()).isNotNull();

    assertThat(symbols.booleanType.tag).isEqualTo(JavaType.BOOLEAN);
    assertThat(symbols.booleanType.symbol.name).isEqualTo("boolean");
    assertThat(symbols.booleanType.symbol.flags()).isEqualTo(Flags.PUBLIC);
    assertThat(symbols.booleanType.symbol.owner()).isSameAs(symbols.rootPackage);
    assertThat(symbols.booleanType.primitiveType()).isNull();
    assertThat(symbols.booleanType.primitiveWrapperType()).isNotNull();

    assertThat(symbols.nullType.tag).isEqualTo(JavaType.BOT);
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
    JavaSymbol.VariableJavaSymbol lengthSymbol = (JavaSymbol.VariableJavaSymbol) symbols.arrayClass.members.lookup("length").get(0);
    assertThat(lengthSymbol.name).isEqualTo("length");
    assertThat(lengthSymbol.owner()).isSameAs(symbols.arrayClass);
    assertThat(lengthSymbol.flags()).isEqualTo(Flags.PUBLIC | Flags.FINAL);
    assertThat(lengthSymbol.type).isSameAs(symbols.intType);

    ClassJavaType arrayClassType = ((ClassJavaType) symbols.arrayClass.type);
    assertThat(arrayClassType.supertype).isSameAs(symbols.objectType);
    assertThat(arrayClassType.interfaces).containsOnly(symbols.cloneableType, symbols.serializableType);
  }

  @Test
  public void primitive_type_from_descriptor() {
    assertThat(symbols.getPrimitiveFromDescriptor('S')).isSameAs(symbols.shortType);
    assertThat(symbols.getPrimitiveFromDescriptor('I')).isSameAs(symbols.intType);
    assertThat(symbols.getPrimitiveFromDescriptor('C')).isSameAs(symbols.charType);
    assertThat(symbols.getPrimitiveFromDescriptor('Z')).isSameAs(symbols.booleanType);
    assertThat(symbols.getPrimitiveFromDescriptor('B')).isSameAs(symbols.byteType);
    assertThat(symbols.getPrimitiveFromDescriptor('J')).isSameAs(symbols.longType);
    assertThat(symbols.getPrimitiveFromDescriptor('F')).isSameAs(symbols.floatType);
    assertThat(symbols.getPrimitiveFromDescriptor('D')).isSameAs(symbols.doubleType);
    assertThat(symbols.getPrimitiveFromDescriptor('V')).isSameAs(symbols.voidType);
    try {
      symbols.getPrimitiveFromDescriptor('P');
      Fail.fail("should have thrown an exception");
    }catch (IllegalStateException ise) {
      assertThat(ise.getMessage()).contains("'P'");
    }

  }
}
