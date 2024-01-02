/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.checks.helpers.HardcodedStringExpressionChecker.isExpressionDerivedFromPlainText;

class HardcodedStringExpressionCheckerTest {

  @Test
  void isExpressionDerivedFromPlainTextTest() {
    IdentifierTree expression = mock(IdentifierTree.class);
    when(expression.kind()).thenReturn(Tree.Kind.IDENTIFIER);
    Symbol.VariableSymbol symbol = mockSymbol(true, false, true);
    Symbol owner = mockOwner(true);
    when(symbol.owner()).thenReturn(owner);
    VariableTree declaration = mock(VariableTree.class);
    when(symbol.declaration()).thenReturn(declaration);
    when(expression.symbol()).thenReturn(symbol);

    assertThat(isExpressionDerivedFromPlainText(expression, new ArrayList<>(), new HashSet<>())).isFalse();

    when(symbol.declaration()).thenReturn(null);
    when(symbol.constantValue()).thenReturn(Optional.of("SOME VALUE"));
    assertThat(isExpressionDerivedFromPlainText(expression, new ArrayList<>(), new HashSet<>())).isTrue();

    when(symbol.isVariableSymbol()).thenReturn(false);
    assertThat(isExpressionDerivedFromPlainText(expression, new ArrayList<>(), new HashSet<>())).isFalse();

    when(symbol.isVariableSymbol()).thenReturn(true);
    when(symbol.isParameter()).thenReturn(true);
    assertThat(isExpressionDerivedFromPlainText(expression, new ArrayList<>(), new HashSet<>())).isFalse();

  }

  private Symbol mockOwner(boolean isTypeSymbol){
    Symbol owner = mock(Symbol.class);
    when(owner.isTypeSymbol()).thenReturn(isTypeSymbol);
    return owner;
  }

  private Symbol.VariableSymbol mockSymbol(boolean isVariableSymbol, boolean isParameter, boolean isFinal){
    Symbol.VariableSymbol symbol = mock(Symbol.VariableSymbol.class);
    when(symbol.isVariableSymbol()).thenReturn(isVariableSymbol);
    when(symbol.isParameter()).thenReturn(isParameter);
    when(symbol.isFinal()).thenReturn(isFinal);
    return symbol;
  }

}
