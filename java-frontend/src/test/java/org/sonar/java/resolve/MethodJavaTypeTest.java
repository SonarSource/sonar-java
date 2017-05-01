/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodJavaTypeTest {

  @Test
  public void methodJavaType_return_type() {
    JavaType intType = new Symbols(new BytecodeCompleter(Lists.<File>newArrayList(), new ParametrizedTypeCache())).intType;

    MethodJavaType methodJavaType = new MethodJavaType(ImmutableList.<JavaType>of(), intType, ImmutableList.<JavaType>of(), null);
    assertThat(methodJavaType.resultType()).isSameAs(intType);

    MethodJavaType constructor = new MethodJavaType(ImmutableList.<JavaType>of(), null, ImmutableList.<JavaType>of(), null);
    assertThat(constructor.resultType()).isNull();
  }

  @Test
  public void to_string_on_type() throws Exception {
    assertThat(new JavaType(JavaType.VOID, null).toString()).isEmpty();
    String methodToString = new MethodJavaType(ImmutableList.<JavaType>of(), new Symbols(new BytecodeCompleter(Lists.<File>newArrayList(), new ParametrizedTypeCache())).intType,
      ImmutableList.<JavaType>of(), null).toString();
    assertThat(methodToString).isEqualTo("returns int");

    String constructorToString = new MethodJavaType(ImmutableList.<JavaType>of(), null, ImmutableList.<JavaType>of(), null).toString();
    assertThat(constructorToString).isEqualTo("constructor");
  }

}
