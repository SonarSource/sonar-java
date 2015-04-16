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
package org.sonar.java.symexec;

import org.sonar.plugins.java.api.semantic.Symbol;

public class SymbolicValue {

  static final SymbolicBooleanValue BOOLEAN_TRUE = new SymbolicBooleanValue();

  public static final class SymbolicBooleanValue extends SymbolicValue {
  }

  public static final class SymbolicVariableValue extends SymbolicValue {
    final Symbol.VariableSymbol variable;

    public SymbolicVariableValue(Symbol.VariableSymbol variable) {
      this.variable = variable;
    }

    @Override
    public boolean equals(Object that) {
      return that instanceof SymbolicVariableValue && variable.equals(((SymbolicVariableValue) that).variable);
    }

    @Override
    public int hashCode() {
      return variable.hashCode();
    }
  }

}
