/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.checks;

import org.junit.Test;
import org.sonar.java.bytecode.asm.AsmMethod;

import java.lang.reflect.Constructor;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SerializableContractTest {

  @Test
  public void testMethodMatch() {
    AsmMethod method = mock(AsmMethod.class);
    when(method.getName()).thenReturn("writeObject", "readObject", "writeReplace", "readResolve", "getParameter");
    assertThat(SerializableContract.methodMatch(method)).isTrue();
    assertThat(SerializableContract.methodMatch(method)).isTrue();
    assertThat(SerializableContract.methodMatch(method)).isTrue();
    assertThat(SerializableContract.methodMatch(method)).isTrue();
    assertThat(SerializableContract.methodMatch(method)).isFalse();
  }

  @Test
  public void private_constructor() throws Exception {
    Constructor constructor = SerializableContract.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

}
