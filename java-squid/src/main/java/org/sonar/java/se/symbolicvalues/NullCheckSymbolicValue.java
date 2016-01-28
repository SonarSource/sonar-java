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
package org.sonar.java.se.symbolicvalues;

import com.google.common.collect.ImmutableList;
import org.sonar.java.se.ConstraintManager.BooleanConstraint;
import org.sonar.java.se.ObjectConstraint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.symbolicvalues.SymbolicValue.UnarySymbolicValue;

import java.util.List;

public class NullCheckSymbolicValue extends UnarySymbolicValue {

  private boolean isNull;

  public NullCheckSymbolicValue(int id, boolean isNull) {
    super(id);
    this.isNull = isNull;
  }

  @Override
  public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
    final ObjectConstraint constraint;
    if (BooleanConstraint.TRUE.equals(booleanConstraint)) {
      constraint = isNull ? ObjectConstraint.NULL : ObjectConstraint.NOT_NULL;
    } else {
      constraint = isNull ? ObjectConstraint.NOT_NULL : ObjectConstraint.NULL;
    }
    return ImmutableList.of(programState.addConstraint(operand, constraint));
  }
}
