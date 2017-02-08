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
import org.sonar.java.collections.PMap;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;

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

  protected List<ProgramState> copyConstraint(SymbolicValue from, SymbolicValue to, ProgramState programState, BooleanConstraint booleanConstraint) {
    List<ProgramState> states = new ArrayList<>();
    states.add(programState);
    PMap<Class<? extends Constraint>, Constraint> leftConstraints = programState.getConstraints(from);
    if (leftConstraints != null) {
      leftConstraints.forEach((d, c) -> {
        List<ProgramState> newStates = new ArrayList<>();
        for (ProgramState state : states) {
          if(ObjectConstraint.class.equals(d)) {
            if(((ObjectConstraint) c).isNull()) {
              newStates.addAll(to.setConstraint(state, shouldNotInverse().equals(booleanConstraint) ? c : c.inverse()));
            } else if(shouldNotInverse().equals(booleanConstraint)) {
              newStates.addAll(to.setConstraint(state, c));
            } else {
              newStates.add(state);
            }
            continue;
          } else if(BooleanConstraint.class.equals(d)) {
            newStates.addAll(to.setConstraint(state, shouldNotInverse().equals(booleanConstraint) ? c : c.inverse()));
            continue;
          }
          newStates.addAll(to.setConstraint(state, shouldNotInverse().equals(booleanConstraint) ? c : c.inverse()));
        }
        states.clear();
        states.addAll(newStates);
      });
    }
    return states;
  }

  public SymbolicValue getLeftOp() {
    return leftOp;
  }

  public SymbolicValue getRightOp() {
    return rightOp;
  }

}
