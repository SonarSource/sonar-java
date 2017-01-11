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
package org.sonar.java.signature;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ParameterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testArgumentAndReturnTypeJavaTypeBoolean() {
    thrown.expect(IllegalArgumentException.class);
    new Parameter(JvmJavaType.L, false);
  }

  @Test
  public void test() {
    thrown.expect(IllegalArgumentException.class);
    new Parameter("", false);
  }

  @Test
  public void testExtractClassNameFromCanonicalClassName() {
    Parameter returnType = new Parameter("org/sonar/Squid", false);
    assertThat(returnType.getClassName()).isEqualTo("Squid");
  }

  @Test
  public void testExtractInnerClassNameFromCanonicalClassName() {
    Parameter returnType = new Parameter("org/sonar/Squid$Entry", false);
    assertThat(returnType.getClassName()).isEqualTo("Entry");
  }

}
