/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class ClassesLayoutTest {

  @Test
  void token() {
    assertThat(countFields(InternalSyntaxToken.class)).isEqualTo(7);
  }

  @Test
  void identifier() {
    assertThat(countFields(IdentifierTreeImpl.class)).isEqualTo(9);
  }

  @Test
  void literal() {
    assertThat(countFields(LiteralTreeImpl.class)).isEqualTo(6);
  }

  @Test
  void variable_declaration() {
    assertThat(countFields(VariableTreeImpl.class)).isEqualTo(10);
  }

  @Test
  void member_select() {
    assertThat(countFields(MemberSelectExpressionTreeImpl.class)).isEqualTo(8);
  }

  @Test
  void method_invocation() {
    assertThat(countFields(MethodInvocationTreeImpl.class)).isEqualTo(8);
  }

  @Test
  void type() {
    assertThat(countFields(JType.class)).isEqualTo(7);
  }

  @Test
  void symbol_type() {
    assertThat(countFields(JTypeSymbol.class)).isEqualTo(11);
  }

  @Test
  void symbol_method() {
    assertThat(countFields(JMethodSymbol.class)).isEqualTo(11);
  }

  @Test
  void symbol_variable() {
    assertThat(countFields(JVariableSymbol.class)).isEqualTo(7);
  }

  @Test
  void annotation() {
    assertThat(countFields(JSymbolMetadata.JAnnotationInstance.class)).isEqualTo(3);
  }

  /** Count number of fields in instances, including inherited fields. */
  private static int countFields(Class<?> clazz) {
    int count = 0;
    while (clazz != Object.class) {
      for (Field f : clazz.getDeclaredFields()) {
        if (!Modifier.isStatic(f.getModifiers())) {
          count++;
        }
      }
      clazz = clazz.getSuperclass();
    }
    return count;
  }

  static class C {
    private int x;
    public long y;
    HashSet<Object> hashSet;
  }

  static class D extends C {
    char z;
    static String staticField;
  }

  @Test
  void validateCountFields() {
    assertThat(countFields(D.class)).isEqualTo(4);
  }
}
