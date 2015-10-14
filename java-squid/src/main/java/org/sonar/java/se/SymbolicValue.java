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
package org.sonar.java.se;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.sonar.java.se.ConstraintManager.BooleanConstraint;
import org.sonar.java.se.ConstraintManager.NullConstraint;

import java.util.List;
import java.util.Map;

public interface SymbolicValue {

  SymbolicValue NULL_LITERAL = new ObjectSymbolicValue(0);
  SymbolicValue TRUE_LITERAL = new ObjectSymbolicValue(1);
  SymbolicValue FALSE_LITERAL = new ObjectSymbolicValue(2);

  void computedFrom(List<SymbolicValue> symbolicValues);

  ProgramState setConstraint(ProgramState programState, BooleanConstraint booleanConstraint);

  ProgramState setConstraint(ProgramState programState, NullConstraint nullConstraint);

  class ObjectSymbolicValue implements SymbolicValue {

    private final int id;

    public ObjectSymbolicValue(int id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ObjectSymbolicValue that = (ObjectSymbolicValue) o;
      return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(id);
    }

    @Override
    public String toString() {
      return "SV#" + id;
    }

    @Override
    public void computedFrom(List<SymbolicValue> symbolicValues) {
      // no op in general case
    }

    @Override
    public ProgramState setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      // store constraint only if symbolic value can be reached by a symbol.
      if (programState.values.containsValue(this)) {
        Map<SymbolicValue, Object> temp = Maps.newHashMap(programState.constraints);
        temp.put(this, booleanConstraint);
        return new ProgramState(programState.values, temp, programState.visitedPoints, programState.stack);
      }
      return programState;
    }

    @Override
    public ProgramState setConstraint(ProgramState programState, NullConstraint nullConstraint) {
      Object data = programState.constraints.get(this);
      if(data instanceof NullConstraint) {
        NullConstraint nc = (NullConstraint) data;
        if((NullConstraint.NULL.equals(nullConstraint) && NullConstraint.NOT_NULL.equals(nc)) ||
            (NullConstraint.NULL.equals(nc) && NullConstraint.NOT_NULL.equals(nullConstraint))) {
          //setting null where value is known to be non null or the contrary
          return null;
        }
      }
      if (data == null || !data.equals(nullConstraint)) {
        Map<SymbolicValue, Object> temp = Maps.newHashMap(programState.constraints);
        temp.put(this, nullConstraint);
        return new ProgramState(programState.values, temp, programState.visitedPoints, programState.stack);
      }
      return programState;
    }
  }

  abstract class BinarySymbolicValue extends ObjectSymbolicValue {

    SymbolicValue leftOp;
    SymbolicValue rightOp;

    public BinarySymbolicValue(int id) {
      super(id);
    }

    abstract BooleanConstraint shouldNotInverse();

    @Override
    public void computedFrom(List<SymbolicValue> symbolicValues) {
      Preconditions.checkArgument(symbolicValues.size() == 2);
      rightOp = symbolicValues.get(0);
      leftOp = symbolicValues.get(1);
    }

    public ProgramState setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      if (leftOp.equals(rightOp)) {
        return shouldNotInverse().equals(booleanConstraint) ? programState : null;
      }
      programState = copyConstraint(leftOp, rightOp, programState, booleanConstraint);
      if (programState == null) {
        return null;
      }
      programState = copyConstraint(rightOp, leftOp, programState, booleanConstraint);
      return programState;

    }

    @Override
    public String toString() {
      return "EQ_TO_" + super.toString();
    }


    private ProgramState copyConstraint(SymbolicValue from, SymbolicValue to, ProgramState programState, BooleanConstraint booleanConstraint) {
      ProgramState result = programState;
      Object constraintLeft = programState.constraints.get(from);
      if (constraintLeft instanceof BooleanConstraint) {
        BooleanConstraint boolConstraint = (BooleanConstraint) constraintLeft;
        result = to.setConstraint(result, shouldNotInverse().equals(booleanConstraint) ? boolConstraint : boolConstraint.inverse());
      } else if (constraintLeft instanceof NullConstraint) {
        NullConstraint nullConstraint = (NullConstraint) constraintLeft;
        result = to.setConstraint(result, shouldNotInverse().equals(booleanConstraint) ? nullConstraint : nullConstraint.inverse());
      }
      return result;
    }

  }
  class NotEqualToSymbolicValue extends BinarySymbolicValue {

    public NotEqualToSymbolicValue(int id) {
      super(id);
    }

    @Override
    public String toString() {
      return "NEQ_TO_" + super.toString();
    }

    @Override
    BooleanConstraint shouldNotInverse() {
      return BooleanConstraint.FALSE;
    }
  }
  class EqualToSymbolicValue extends BinarySymbolicValue {

    public EqualToSymbolicValue(int id) {
      super(id);
    }

    @Override
    BooleanConstraint shouldNotInverse() {
      return BooleanConstraint.TRUE;
    }

  }

}
