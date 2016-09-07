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

import javax.annotation.Nullable;

import java.util.Arrays;

public class MethodYield {
  Constraint[] parametersConstraints;
  int resultIndex;
  @Nullable
  Constraint resultConstraint;

  public MethodYield(int arity) {
    this.parametersConstraints = new Constraint[arity];
    this.resultIndex = -1;
    this.resultConstraint = null;
  }

  @Override
  public String toString() {
    return "{params: " + Arrays.toString(parametersConstraints) + ", result: " + resultConstraint + " (" + resultIndex + ")}";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(parametersConstraints);
    result = prime * result + ((resultConstraint == null) ? 0 : resultConstraint.hashCode());
    result = prime * result + resultIndex;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MethodYield other = (MethodYield) obj;
    if (!Arrays.equals(parametersConstraints, other.parametersConstraints)) {
      return false;
    }
    if (resultConstraint == null) {
      if (other.resultConstraint != null) {
        return false;
      }
    } else if (!resultConstraint.equals(other.resultConstraint) || resultIndex != other.resultIndex) {
      return false;
    }
    return true;
  }
}
