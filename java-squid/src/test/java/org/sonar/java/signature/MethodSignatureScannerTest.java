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
package org.sonar.java.signature;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class MethodSignatureScannerTest {

  @Test
  public void scan() {
    MethodSignature method = MethodSignatureScanner.scan("read(Ljava/lang/String;[S)V");
    assertThat(method.getMethodName()).isEqualTo("read");

    assertThat(method.getReturnType().getJvmJavaType()).isEqualTo(JvmJavaType.V);
    assertThat(method.getArgumentTypes().size()).isEqualTo(2);

    Parameter param1 = method.getArgumentTypes().get(0);
    assertThat(param1.isOject()).isTrue();
    assertThat(param1.getClassName()).isEqualTo("String");

    Parameter param2 = method.getArgumentTypes().get(1);
    assertThat(param2.isOject()).isFalse();
    assertThat(param2.isArray()).isTrue();
    assertThat(param2.getJvmJavaType()).isEqualTo(JvmJavaType.S);
  }

  @Test
  public void scanMethodWithReturnType() {
    MethodSignature method = MethodSignatureScanner.scan("read(Ljava/lang/String;S)[Ljava/util/Vector;");

    assertThat(method.getReturnType().isOject()).isTrue();
    assertThat(method.getReturnType().isArray()).isTrue();
    assertThat(method.getReturnType().getClassName()).isEqualTo("Vector");
  }

  @Test
  public void scanGenericMethod(){
    MethodSignature method = MethodSignatureScanner.scan("transactionValidation(Ljava/lang/String;Ljava/util/List<Ljava/lang/String;>;)V");

    Parameter param1 = method.getArgumentTypes().get(0);
    assertThat(param1.isOject()).isTrue();
    assertThat(param1.getClassName()).isEqualTo("String");

    Parameter param2 = method.getArgumentTypes().get(1);
    assertThat(param2.isOject()).isTrue();
    assertThat(param2.getClassName()).isEqualTo("List");
  }

}
