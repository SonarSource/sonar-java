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
package org.sonar.java.se;

import org.junit.jupiter.api.Test;

import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearnedAssociationTest {

  @Test
  void test_toString() {
    SymbolicValue sv = new SymbolicValue() {
      @Override
      public String toString() {
        return "SV_1";
      }
    };
    LearnedAssociation la = new LearnedAssociation(sv, variable("a"));
    assertThat(la).hasToString("SV_1 - a");
  }

  private static final Symbol.VariableSymbol variable(String name) {
    Symbol.VariableSymbol variable = mock(Symbol.VariableSymbol.class);
    when(variable.name()).thenReturn(name);
    return variable;
  }

}
