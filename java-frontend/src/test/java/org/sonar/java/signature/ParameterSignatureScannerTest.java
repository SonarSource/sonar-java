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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ParameterSignatureScannerTest {

  @Test
  public void testScanVoid() {
    Parameter param = ParameterSignatureScanner.scan("V");
    assertThat(param.isVoid()).isTrue();
  }

  @Test
  public void testScanObject() {
    Parameter param = ParameterSignatureScanner.scan("Ljava/lang/String;");
    assertThat(param.isOject()).isTrue();
    assertThat(param.getClassName()).isEqualTo("String");
  }

  @Test
  public void testScanSimpleGenericObject() {
    Parameter param = ParameterSignatureScanner.scan("TU;");
    assertThat(param.isOject()).isTrue();
    assertThat(param.getClassName()).isEqualTo("U");
  }

  @Test
  public void testScanComplexGenericObject() {
    Parameter param = ParameterSignatureScanner.scan("TU<TV;Ljava/util/Map$Entry<TY>>;");
    assertThat(param.isOject()).isTrue();
    assertThat(param.getClassName()).isEqualTo("U");
  }

  @Test
  public void testScanInnerClassObject() {
    Parameter param = ParameterSignatureScanner.scan("LMap$Entry;");
    assertThat(param.isOject()).isTrue();
    assertThat(param.getClassName()).isEqualTo("Entry");
  }

  @Test
  public void testScanPrimitiveType() {
    Parameter param = ParameterSignatureScanner.scan("B");
    assertThat(param.isOject()).isFalse();
    assertThat(param.getJvmJavaType()).isEqualTo(JvmJavaType.B);
  }

  @Test
  public void testScanArray() {
    Parameter param = ParameterSignatureScanner.scan("[B");
    assertThat(param.isArray()).isTrue();
    assertThat(param.getJvmJavaType()).isEqualTo(JvmJavaType.B);

    param = ParameterSignatureScanner.scan("B");
    assertThat(param.isArray()).isFalse();
    assertThat(param.getJvmJavaType()).isEqualTo(JvmJavaType.B);

    param = ParameterSignatureScanner.scan("[LString;");
    assertThat(param.isOject()).isTrue();
    assertThat(param.getClassName()).isEqualTo("String");
  }

  @Test
  public void testScanArrayOfArray() {
    Parameter param = ParameterSignatureScanner.scan("[[[[B");
    assertThat(param.isArray()).isTrue();
    assertThat(param.getJvmJavaType()).isEqualTo(JvmJavaType.B);
  }

  @Test
  public void testScanSeveralPrimitiveArguments() {
    List<Parameter> params = ParameterSignatureScanner.scanArguments("BIZ");
    assertThat(params.size()).isEqualTo(3);

    Parameter param1 = params.get(0);
    assertThat(param1.isOject()).isFalse();
    assertThat(param1.getJvmJavaType()).isEqualTo(JvmJavaType.B);
  }

  @Test
  public void testScanSeveralArgumentsWithGeneric() {
    List<Parameter> params = ParameterSignatureScanner
        .scanArguments("Ljava/lang/String;Ljava/util/List<Ljava/lang/String;>;[Ljava/util/Vector;Ljava/util/ArrayList<Ljava/lang/Integer;>;");
    assertThat(params.get(0).isOject()).isTrue();
    assertThat(params.get(0).getClassName()).isEqualTo("String");

    assertThat(params.get(1).isOject()).isTrue();
    assertThat(params.get(1).getClassName()).isEqualTo("List");

    assertThat(params.get(2).isOject()).isTrue();
    assertThat(params.get(2).isArray()).isTrue();
    assertThat(params.get(2).getClassName()).isEqualTo("Vector");

    assertThat(params.get(3).isOject()).isTrue();
    assertThat(params.get(3).isArray()).isFalse();
    assertThat(params.get(3).getClassName()).isEqualTo("ArrayList");
  }

  @Test
  public void testScanSeveralComplexArguments() {
    List<Parameter> params = ParameterSignatureScanner.scanArguments("B[LString;IZ");
    assertThat(params.size()).isEqualTo(4);

    Parameter param1 = params.get(0);
    assertThat(param1.isOject()).isFalse();
    assertThat(param1.getJvmJavaType()).isEqualTo(JvmJavaType.B);

    Parameter param2 = params.get(1);
    assertThat(param2.isOject()).isTrue();
    assertThat(param2.getClassName()).isEqualTo("String");

    Parameter param3 = params.get(2);
    assertThat(param3.isOject()).isFalse();
    assertThat(param3.getJvmJavaType()).isEqualTo(JvmJavaType.I);
  }

}
