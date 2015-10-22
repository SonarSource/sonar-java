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
package org.sonar.java.checks.helpers;

import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.fest.assertions.Assertions.assertThat;

public class JavaVersionHelperTest {

  @Test
  public void private_constructor() throws Exception {
    Constructor<JavaVersionHelper> constructor = JavaVersionHelper.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void java_7_compatible() throws Exception {
    assertThat(JavaVersionHelper.java7Compatible(6)).isFalse();
    assertThat(JavaVersionHelper.java7Compatible(7)).isTrue();
    assertThat(JavaVersionHelper.java7Compatible(8)).isTrue();
    assertThat(JavaVersionHelper.java7Compatible(null)).isTrue();
  }

  @Test
  public void java_8_compatible() throws Exception {
    assertThat(JavaVersionHelper.java8Compatible(6)).isFalse();
    assertThat(JavaVersionHelper.java8Compatible(7)).isFalse();
    assertThat(JavaVersionHelper.java8Compatible(8)).isTrue();
    assertThat(JavaVersionHelper.java8Compatible(null)).isTrue();
  }

  @Test
  public void java_8_required() throws Exception {
    assertThat(JavaVersionHelper.java8Guaranteed(6)).isFalse();
    assertThat(JavaVersionHelper.java8Guaranteed(7)).isFalse();
    assertThat(JavaVersionHelper.java8Guaranteed(8)).isTrue();
    assertThat(JavaVersionHelper.java8Guaranteed(null)).isFalse();
  }

}
