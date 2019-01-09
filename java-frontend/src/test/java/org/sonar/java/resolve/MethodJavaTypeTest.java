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

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodJavaTypeTest {

  private static final Symbols SYMBOLS = new Symbols(new BytecodeCompleter(new SquidClassLoader(Collections.emptyList()), new ParametrizedTypeCache()));

  @Test
  public void methodJavaType_return_type() {
    MethodJavaType methodJavaType = new MethodJavaType(Collections.emptyList(), SYMBOLS.intType, Collections.emptyList(), null);
    assertThat(methodJavaType.resultType()).isSameAs(SYMBOLS.intType);

    MethodJavaType constructor = new MethodJavaType(Collections.emptyList(), null, Collections.emptyList(), null);
    assertThat(constructor.resultType()).isNull();
  }

  @Test
  public void to_string_on_type() throws Exception {
    assertThat(new JavaType(JavaType.VOID, null).toString()).isEmpty();
    String methodToString = new MethodJavaType(Collections.emptyList(), SYMBOLS.intType, Collections.emptyList(), null).toString();
    assertThat(methodToString).isEqualTo("returns int");

    String constructorToString = new MethodJavaType(Collections.emptyList(), null, Collections.emptyList(), null).toString();
    assertThat(constructorToString).isEqualTo("constructor");
  }

  @Test
  public void thrown_types() {
    MethodJavaType m1 = new MethodJavaType(Collections.emptyList(), SYMBOLS.voidType, Collections.emptyList(), null);
    assertThat(m1.thrownTypes()).isEmpty();

    MethodJavaType m2 = new MethodJavaType(Collections.emptyList(), SYMBOLS.voidType, Collections.singletonList(Symbols.unknownType), null);
    assertThat(m2.thrownTypes()).hasSize(1);
    assertThat(m2.thrownTypes()).containsOnly(Symbols.unknownType);

    JavaType t1 = new JavaType(JavaType.CLASS, Symbols.unknownSymbol);
    JavaType t2 = new JavaType(JavaType.CLASS, Symbols.unknownSymbol);
    MethodJavaType m3 = new MethodJavaType(Collections.emptyList(), SYMBOLS.voidType, Arrays.asList(t1, t2), null);
    assertThat(m3.thrownTypes()).hasSize(2);
    assertThat(m3.thrownTypes()).containsExactly(t1, t2);
  }

  @Test
  public void methodJavaType_arg_types() {
    MethodJavaType m1 = new MethodJavaType(Collections.emptyList(), SYMBOLS.voidType, Collections.emptyList(), null);
    assertThat(m1.argTypes()).isEmpty();

    MethodJavaType m2 = new MethodJavaType(Collections.singletonList(Symbols.unknownType), SYMBOLS.voidType, Collections.emptyList(), null);
    assertThat(m2.argTypes()).hasSize(1);
    assertThat(m2.argTypes()).containsOnly(Symbols.unknownType);

    JavaType t1 = new JavaType(JavaType.CLASS, Symbols.unknownSymbol);
    JavaType t2 = new JavaType(JavaType.CLASS, Symbols.unknownSymbol);
    MethodJavaType m3 = new MethodJavaType(Arrays.asList(t1, t2), SYMBOLS.voidType, Collections.emptyList(), null);
    assertThat(m3.argTypes()).hasSize(2);
    assertThat(m3.argTypes()).containsExactly(t1, t2);
  }
}
