/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.model;

import org.junit.jupiter.api.Test;
import org.openjdk.jol.datamodel.Model64;
import org.openjdk.jol.datamodel.Model64_COOPS_CCPS;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ClassesLayoutTest {

  private static final int JDK_VERSION = 11;

  private static final Layouter X86_64 = new HotSpotLayouter(new Model64(), JDK_VERSION);
  private static final Layouter X86_64_COOPS = new HotSpotLayouter(new Model64_COOPS_CCPS(), JDK_VERSION);

  @Test
  void token() {
    assertAll(
      () -> assertThat(instanceSize(InternalSyntaxToken.class, X86_64)).isEqualTo(72),
      () -> assertThat(instanceSize(InternalSyntaxToken.class, X86_64_COOPS)).isEqualTo(48)
    );
  }

  @Test
  void identifier() {
    assertAll(
      () -> assertThat(instanceSize(IdentifierTreeImpl.class, X86_64)).isEqualTo(80),
      () -> assertThat(instanceSize(IdentifierTreeImpl.class, X86_64_COOPS)).isEqualTo(48)
    );
  }

  @Test
  void literal() {
    assertAll(
      () -> assertThat(instanceSize(LiteralTreeImpl.class, X86_64)).isEqualTo(64),
      () -> assertThat(instanceSize(LiteralTreeImpl.class, X86_64_COOPS)).isEqualTo(40)
    );
  }

  @Test
  void variable_declaration() {
    assertAll(
      () -> assertThat(instanceSize(VariableTreeImpl.class, X86_64)).isEqualTo(96),
      () -> assertThat(instanceSize(VariableTreeImpl.class, X86_64_COOPS)).isEqualTo(56)
    );
  }

  @Test
  void member_select() {
    assertAll(
      () -> assertThat(instanceSize(MemberSelectExpressionTreeImpl.class, X86_64)).isEqualTo(80),
      () -> assertThat(instanceSize(MemberSelectExpressionTreeImpl.class, X86_64_COOPS)).isEqualTo(48)
    );
  }

  @Test
  void method_invocation() {
    assertAll(
      () -> assertThat(instanceSize(MethodInvocationTreeImpl.class, X86_64)).isEqualTo(80),
      () -> assertThat(instanceSize(MethodInvocationTreeImpl.class, X86_64_COOPS)).isEqualTo(48)
    );
  }

  @Test
  void type() {
    assertAll(
      () -> assertThat(instanceSize(JType.class, X86_64)).isEqualTo(48),
      () -> assertThat(instanceSize(JType.class, X86_64_COOPS)).isEqualTo(32)
    );
  }

  @Test
  void symbol_type() {
    assertAll(
      () -> assertThat(instanceSize(JTypeSymbol.class, X86_64)).isEqualTo(96),
      () -> assertThat(instanceSize(JTypeSymbol.class, X86_64_COOPS)).isEqualTo(56)
    );
  }

  @Test
  void symbol_method() {
    assertAll(
      () -> assertThat(instanceSize(JMethodSymbol.class, X86_64)).isEqualTo(104),
      () -> assertThat(instanceSize(JMethodSymbol.class, X86_64_COOPS)).isEqualTo(56)
    );
  }

  @Test
  void symbol_variable() {
    assertAll(
      () -> assertThat(instanceSize(JVariableSymbol.class, X86_64)).isEqualTo(56),
      () -> assertThat(instanceSize(JVariableSymbol.class, X86_64_COOPS)).isEqualTo(32)
    );
  }

  @Test
  void annotation() {
    assertAll(
      () -> assertThat(instanceSize(JSymbolMetadata.JAnnotationInstance.class, X86_64)).isEqualTo(40),
      () -> assertThat(instanceSize(JSymbolMetadata.JAnnotationInstance.class, X86_64_COOPS)).isEqualTo(24)
    );
  }

  private static long instanceSize(Class<?> cls, Layouter layouter) {
    System.out.println("***** " + layouter);
    ClassLayout classLayout = ClassLayout.parseClass(cls, layouter);
    System.out.println(classLayout.toPrintable());
    return classLayout.instanceSize();
  }

}
