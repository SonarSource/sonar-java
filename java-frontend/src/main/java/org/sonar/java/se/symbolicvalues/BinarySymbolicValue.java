/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.sonar.java.se.constraint.BooleanConstraint;

import java.util.List;

public abstract class BinarySymbolicValue extends SymbolicValue {

  SymbolicValue leftOp;
  SymbolicValue rightOp;

  public BinarySymbolicValue(int id) {
    super(id);
  }

  public abstract BooleanConstraint shouldNotInverse();

  @Override
  public boolean references(SymbolicValue other) {
    return leftOp.equals(other) || rightOp.equals(other) || leftOp.references(other) || rightOp.references(other);
  }

  @Override
  public void computedFrom(List<SymbolicValue> symbolicValues) {
    Preconditions.checkArgument(symbolicValues.size() == 2);
    rightOp = symbolicValues.get(0);
    leftOp = symbolicValues.get(1);
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
