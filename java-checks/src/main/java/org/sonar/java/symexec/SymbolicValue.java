/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import com.google.common.base.Preconditions;
import org.sonar.plugins.java.api.semantic.Symbol;

abstract class SymbolicValue {

  private SymbolicValue() {
  }

  static final SymbolicBooleanValue BOOLEAN_TRUE = new SymbolicBooleanValue();

  static final class SymbolicBooleanValue extends SymbolicValue {
  }

  static final class SymbolicLongValue extends SymbolicValue {
    final long value;

    SymbolicLongValue(long value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object that) {
      return that instanceof SymbolicLongValue && value == ((SymbolicLongValue) that).value;
    }

    @Override
    public int hashCode() {
      return (int) (value ^ (value >>> 32));
    }
  }

  static final class SymbolicVariableValue extends SymbolicValue {
    final Symbol.VariableSymbol variable;

    SymbolicVariableValue(Symbol.VariableSymbol variable) {
      Preconditions.checkNotNull(variable);
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
