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

import static org.fest.assertions.Assertions.assertThat;

public class AsmClassTest {

  @Test
  public void testGetFieldOrCreateIt() {
    AsmClass asmClass = new AsmClass("java/lang/String");

    assertThat(asmClass.getField("internalString")).isNull();
    assertThat(asmClass.getFieldOrCreateIt("internalString")).isNotNull();
    assertThat(asmClass.getField("internalString")).isNotNull();
  }

  @Test
  public void testGetMethoddOrCreateIt() {
    AsmClass asmClass = new AsmClass("java/lang/String");

    assertThat(asmClass.getMethod("toString()Ljava/lang/String;")).isNull();
    assertThat(asmClass.getMethodOrCreateIt("toString()Ljava/lang/String;")).isNotNull();
    assertThat(asmClass.getMethod("toString()Ljava/lang/String;")).isNotNull();
  }

  @Test
  public void testEqualsAndHashcode() {
    assertThat(new AsmClass("java/lang/String")).isEqualTo(new AsmClass("java/lang/String"));
    assertThat(new AsmClass("java/lang/String").hashCode()).isEqualTo(new AsmClass("java/lang/String").hashCode());
    assertThat(new AsmClass("java/lang/String")).isNotEqualTo(new AsmClass("java/lang/Number"));
    assertThat(new AsmClass("java/lang/String").hashCode()).isNotEqualTo(new AsmClass("java/lang/Number").hashCode());
  }

  @Test
  public void getDisplayName() {
    assertThat(new AsmClass("java/lang/String").getDisplayName()).isEqualTo("java.lang.String");
  }

}
