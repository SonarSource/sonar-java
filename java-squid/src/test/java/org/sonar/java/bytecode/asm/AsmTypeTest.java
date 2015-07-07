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
package org.sonar.java.bytecode.asm;

import org.junit.Test;
import org.objectweb.asm.Type;

import static org.fest.assertions.Assertions.assertThat;

public class AsmTypeTest {

  @Test
  public void testIsArray() {
    assertThat(AsmType.isArray(Type.getType("[Ljava/lang/String;"))).isTrue();
    assertThat(AsmType.isArray(Type.getType("[I"))).isTrue();
    assertThat(AsmType.isArray(Type.getType("I"))).isFalse();
  }

  @Test
  public void testIsObject() {
    assertThat(AsmType.isObject(Type.getType("Ljava/lang/Number;"))).isTrue();
    assertThat(AsmType.isObject(Type.getType("B"))).isFalse();
  }

  @Test
  public void testIsArrayOfObject() {
    assertThat(AsmType.isArrayOfObject(Type.getType("[Ljava/lang/Number;"))).isTrue();
    assertThat(AsmType.isArrayOfObject(Type.getType("[[Ljava/lang/Number;"))).isTrue();
  }

  @Test
  public void testIsVoid() {
    assertThat(AsmType.isVoid(Type.getType("V"))).isTrue();
    assertThat(AsmType.isVoid(Type.getType("B"))).isFalse();
  }

  @Test
  public void testGetInternalName() {
    assertThat(AsmType.getObjectInternalName(Type.getType("[[[Ljava/lang/String;"))).isEqualTo("java/lang/String");
    assertThat(AsmType.getObjectInternalName(Type.getType("Ljava/lang/String;"))).isEqualTo("java/lang/String");
  }

  @Test
  public void testContainsObject() {
    assertThat(AsmType.containsObject(Type.getType("[[[Ljava/lang/String;"))).isTrue();
    assertThat(AsmType.containsObject(Type.getType("Ljava/lang/String;"))).isTrue();
    assertThat(AsmType.containsObject(Type.getType("B"))).isFalse();
    assertThat(AsmType.containsObject(Type.getType("[B"))).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void testGetInternalNameOnPrimitiveDescriptor() {
    AsmType.getObjectInternalName(Type.getType("[[[I"));
  }

}
