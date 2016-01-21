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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.sonar.java.se.ConstraintManager.BooleanConstraint;
import org.sonar.java.se.ObjectConstraint;
import org.sonar.java.se.ProgramState;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
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
  public List<ProgramState> setConstraint(ProgramState initialProgramState, BooleanConstraint booleanConstraint) {
    ProgramState programState = initialProgramState;
    if (leftOp.equals(rightOp)) {
      if (shouldNotInverse().equals(booleanConstraint)) {
        return ImmutableList.of(programState);
      }
      return ImmutableList.of();
    }
    programState = checkRelation(booleanConstraint, programState);
    if (programState == null) {
      return ImmutableList.of();
    }
    List<ProgramState> results = new ArrayList<>();
    List<ProgramState> copiedConstraints = copyConstraint(leftOp, rightOp, programState, booleanConstraint);
    for (ProgramState ps : copiedConstraints) {
      List<ProgramState> copiedConstraintsRightToLeft = copyConstraint(rightOp, leftOp, ps, booleanConstraint);
      if (copiedConstraintsRightToLeft.size() == 1 && copiedConstraintsRightToLeft.get(0).equals(programState)) {
        results.add(programState.addConstraint(this, booleanConstraint));
      } else {
        results.addAll(copiedConstraintsRightToLeft);
      }
    }
    return results;
  }

  @CheckForNull
  protected ProgramState checkRelation(BooleanConstraint booleanConstraint, ProgramState programState) {
    return programState;
  }

  @Override
  public String toString() {
    return "EQ_TO_" + super.toString();
  }

  protected List<ProgramState> copyConstraint(SymbolicValue from, SymbolicValue to, ProgramState programState, BooleanConstraint booleanConstraint) {
    Object constraintLeft = programState.getConstraint(from);
    if (constraintLeft instanceof BooleanConstraint) {
      BooleanConstraint boolConstraint = (BooleanConstraint) constraintLeft;
      return to.setConstraint(programState, shouldNotInverse().equals(booleanConstraint) ? boolConstraint : boolConstraint.inverse());
    } else if (constraintLeft instanceof ObjectConstraint) {
      ObjectConstraint nullConstraint = (ObjectConstraint) constraintLeft;
      if (nullConstraint.equals(ObjectConstraint.NULL)) {
        return to.setConstraint(programState, shouldNotInverse().equals(booleanConstraint) ? nullConstraint : nullConstraint.inverse());
      } else if (shouldNotInverse().equals(booleanConstraint)) {
        return to.setConstraint(programState, nullConstraint);
      }
    }
    return ImmutableList.of(programState);
  }

}
