/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.se.symbolicvalues;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sonar.java.se.ProgramState;
import org.sonar.plugins.java.api.semantic.Symbol;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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
    return ImmutableList.of(leftOp, rightOp);
  }

  public SymbolicValue getLeftOp() {
    return leftOp;
  }

  public SymbolicValue getRightOp() {
    return rightOp;
  }

}
