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
package org.sonar.java.se.symbolicvalues;

import org.sonar.java.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.se.ProgramState;
import org.sonar.plugins.java.api.semantic.Symbol;

public abstract class BinarySymbolicValue extends SymbolicValue {

  SymbolicValue leftOp;
  @Nullable
  Symbol leftSymbol;
  SymbolicValue rightOp;
  @Nullable
  Symbol rightSymbol;

  @Override
  public boolean references(SymbolicValue other) {
    return leftOp.equals(other) || rightOp.equals(other) || leftOp.references(other) || rightOp.references(other);
  }

  @Override
  public void computedFrom(List<ProgramState.SymbolicValueSymbol> symbolicValues) {
    Preconditions.checkArgument(symbolicValues.size() == 2);
    Preconditions.checkState(leftOp == null && rightOp == null, "Operands already set!");
    rightOp = symbolicValues.get(0).symbolicValue();
    rightSymbol = symbolicValues.get(0).symbol();
    leftOp = symbolicValues.get(1).symbolicValue();
    leftSymbol = symbolicValues.get(1).symbol();
  }

  @Override
  public List<Symbol> computedFromSymbols() {
    List<Symbol> result = new ArrayList<>();
    if (leftSymbol == null) {
      result.addAll(leftOp.computedFromSymbols());
    } else {
      result.add(leftSymbol);
    }
    if (rightSymbol == null) {
      result.addAll(rightOp.computedFromSymbols());
    } else {
      result.add(rightSymbol);
    }
    return result;
  }

  @CheckForNull
  public Symbol leftSymbol() {
    return leftSymbol;
  }

  @CheckForNull
  public Symbol rightSymbol() {
    return rightSymbol;
  }

  @Override
  public List<SymbolicValue> computedFrom() {
    return Arrays.asList(leftOp, rightOp);
  }

  public SymbolicValue getLeftOp() {
    return leftOp;
  }

  public SymbolicValue getRightOp() {
    return rightOp;
  }

}
