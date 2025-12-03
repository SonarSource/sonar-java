/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;

public class LearnedAssociation {
  final SymbolicValue sv;
  final Symbol symbol;

  public LearnedAssociation(SymbolicValue sv, Symbol symbol) {
    this.sv = sv;
    this.symbol = symbol;
  }

  public Symbol symbol() {
    return symbol;
  }

  public SymbolicValue symbolicValue() {
    return sv;
  }

  @Override
  public String toString() {
    return sv + " - " + symbol.name();
  }
}
