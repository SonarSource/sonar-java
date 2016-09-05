/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.se;

import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;

import java.util.Arrays;
import java.util.List;

public class MethodYield {
  final List<ConstrainedSymbolicValue> parameters;
  final ConstrainedSymbolicValue result;

  public MethodYield(List<ConstrainedSymbolicValue> parameters, ConstrainedSymbolicValue result) {
    this.parameters = parameters;
    this.result = result;
  }

  @Override
  public String toString() {
    return "{params: " + Arrays.toString(parameters.toArray()) + ", result: " + result + "}";
  }

  public static class ConstrainedSymbolicValue {
    final SymbolicValue symbolicValue;
    final Constraint constraint;

    public ConstrainedSymbolicValue(SymbolicValue symbolicValue, Constraint constraint) {
      this.symbolicValue = symbolicValue;
      this.constraint = constraint;
    }

    @Override
    public String toString() {
      return "(" + symbolicValue + " > " + constraint + ")";
    }
  }

}
