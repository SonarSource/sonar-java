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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MethodSignaturePrinterTest {

  @Test
  public void testPrint() {
    List<Parameter> argumentTypes = new ArrayList<>();
    MethodSignature method = new MethodSignature("read", new Parameter(JvmJavaType.V, false), argumentTypes);
    assertThat(MethodSignaturePrinter.print(method)).isEqualTo("read()V");

    argumentTypes.add(new Parameter("java/lang/String", true));
    method = new MethodSignature("read", new Parameter("org/sonar/squid/Squid", false), argumentTypes);
    assertThat(MethodSignaturePrinter.print(method)).isEqualTo("read([LString;)LSquid;");

    argumentTypes.add(new Parameter(JvmJavaType.B, false));
    method = new MethodSignature("write", new Parameter(JvmJavaType.I, true), argumentTypes);
    assertThat(MethodSignaturePrinter.print(method)).isEqualTo("write([LString;B)[I");

    argumentTypes.add(new Parameter(JvmJavaType.I, false));
    method = new MethodSignature("write", new Parameter(JvmJavaType.I, true), argumentTypes);
    assertThat(MethodSignaturePrinter.print(method)).isEqualTo("write([LString;BI)[I");
  }

}
