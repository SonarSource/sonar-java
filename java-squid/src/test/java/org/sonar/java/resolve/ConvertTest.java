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
package org.sonar.java.resolve;

import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.fest.assertions.Assertions.assertThat;

public class ConvertTest {

  @Test
  public void private_constructor() throws Exception {
    Constructor constructor = Convert.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void packagePart() {
    assertThat(Convert.packagePart("org")).isEqualTo("");
    assertThat(Convert.packagePart("org.example")).isEqualTo("org");
  }

  @Test
  public void shortName() {
    assertThat(Convert.shortName("org")).isEqualTo("org");
    assertThat(Convert.shortName("org.example")).isEqualTo("example");
  }

  @Test
  public void flatName() {
    assertThat(Convert.flatName("org/example/MyClass")).isEqualTo("org.example.MyClass");
    assertThat(Convert.flatName("org/example/MyClass$InnerClass")).isEqualTo("org.example.MyClass$InnerClass");
  }

  @Test
  public void bytecodeName() {
    assertThat(Convert.bytecodeName("org.example.MyClass")).isEqualTo("org/example/MyClass");
    assertThat(Convert.bytecodeName("org.example.MyClass$InnerClass")).isEqualTo("org/example/MyClass$InnerClass");
  }

  @Test
  public void enclosingClassName() throws Exception {
    assertThat(Convert.enclosingClassName("MyClass")).isEqualTo("");
    assertThat(Convert.enclosingClassName("MyClass$InnerClass")).isEqualTo("MyClass");
    assertThat(Convert.enclosingClassName("MyClass$$InnerClass$class")).isEqualTo("MyClass$");
    assertThat(Convert.enclosingClassName("MyClass$$InnerClass$")).isEqualTo("MyClass$");
  }

  @Test
  public void innerClassName() throws Exception {
    assertThat(Convert.innerClassName("MyClass")).isEqualTo("MyClass");
    assertThat(Convert.innerClassName("MyClass$InnerClass")).isEqualTo("InnerClass");
    assertThat(Convert.innerClassName("MyClass$InnerClass$")).isEqualTo("InnerClass$");
    assertThat(Convert.innerClassName("MyClass$InnerClass$class")).isEqualTo("InnerClass$class");
    assertThat(Convert.innerClassName("MyClass$$InnerClass$")).isEqualTo("InnerClass$");

  }

  @Test
  public void fullName() throws Exception {
    assertThat(Convert.fullName(null, "MyClass")).isEqualTo("MyClass");
    assertThat(Convert.fullName("","MyClass")).isEqualTo("MyClass");
    assertThat(Convert.fullName("org.example", "MyClass")).isEqualTo("org.example.MyClass");
  }
}