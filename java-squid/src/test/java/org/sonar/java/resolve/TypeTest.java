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
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class TypeTest {

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
  public void to_string_on_type() throws Exception {
    assertThat(new Type(Type.VOID, null).toString()).isEmpty();
    String methodToString  = new Type.MethodType(ImmutableList.<Type>of(), new Symbols().intType, ImmutableList.<Type>of(), null).toString();
    assertThat(methodToString).isEqualTo("returns int");
  }
}
