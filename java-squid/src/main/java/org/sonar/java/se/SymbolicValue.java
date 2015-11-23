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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.sonar.java.se.ConstraintManager.BooleanConstraint;
import org.sonar.java.se.ConstraintManager.NullConstraint;

import java.util.ArrayList;
import java.util.List;

public interface SymbolicValue {

  SymbolicValue NULL_LITERAL = new ObjectSymbolicValue(0) {
    @Override
    public String toString() {
      return super.toString() + "_NULL";
    }
  };
  SymbolicValue TRUE_LITERAL = new ObjectSymbolicValue(1) {
    @Override
    public String toString() {
      return super.toString() + "_TRUE";
    }
  };
  SymbolicValue FALSE_LITERAL = new ObjectSymbolicValue(2) {
    @Override
    public String toString() {
      return super.toString() + "_FALSE";
    }
  };

  void computedFrom(List<SymbolicValue> symbolicValues);

  ProgramState setSingleConstraint(ProgramState state, NullConstraint notNull);

  List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint);

  List<ProgramState> setConstraint(ProgramState programState, NullConstraint nullConstraint);

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
      return id == that.id;
    }

    @Override
    public int hashCode() {
      return id;
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
    public List<ProgramState> setConstraint(ProgramState programState, NullConstraint nullConstraint) {
      Object data = programState.getConstraint(this);
      if (data instanceof NullConstraint) {
        NullConstraint nc = (NullConstraint) data;
        if (!nc.equals(nullConstraint)) {
          // setting null where value is known to be non null or the contrary
          return ImmutableList.of();
        }
      }
      if (data == null || !data.equals(nullConstraint)) {
        return ImmutableList.of(programState.addConstraint(this, nullConstraint));
      }
      return ImmutableList.of(programState);
    }

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      Object data = programState.getConstraint(this);
      // update program state only for a different constraint
      if (data instanceof BooleanConstraint) {
        BooleanConstraint bc = (BooleanConstraint) data;
        if (!bc.equals(booleanConstraint)) {
          // setting null where value is known to be non null or the contrary
          return ImmutableList.of();
        }
      }
      if ((data == null || !data.equals(booleanConstraint)) && programState.canReach(this)) {
        // store constraint only if symbolic value can be reached by a symbol.
        return ImmutableList.of(programState.addConstraint(this, booleanConstraint));
      }
      return ImmutableList.of(programState);
    }

    @Override
    public ProgramState setSingleConstraint(ProgramState programState, NullConstraint nullConstraint) {
      final List<ProgramState> states = setConstraint(programState, nullConstraint);
      if (states.size() != 1) {
        throw new IllegalStateException("Only a single program state is expected at this location");
      }
      return states.get(0);
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

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      if (leftOp.equals(rightOp)) {
        if (shouldNotInverse().equals(booleanConstraint)) {
          return ImmutableList.of(programState);
        }
        return ImmutableList.of();
      }
      List<ProgramState> copiedConstraints = copyConstraint(leftOp, rightOp, programState, booleanConstraint);
      if (copiedConstraints.isEmpty()) {
        return ImmutableList.of();
      }
      List<ProgramState> results = new ArrayList<>();
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

    @Override
    public String toString() {
      return "EQ_TO_" + super.toString();
    }

    private List<ProgramState> copyConstraint(SymbolicValue from, SymbolicValue to, ProgramState programState, BooleanConstraint booleanConstraint) {
      Object constraintLeft = programState.getConstraint(from);
      if (constraintLeft instanceof BooleanConstraint) {
        BooleanConstraint boolConstraint = (BooleanConstraint) constraintLeft;
        return to.setConstraint(programState, shouldNotInverse().equals(booleanConstraint) ? boolConstraint : boolConstraint.inverse());
      } else if (constraintLeft instanceof NullConstraint) {
        NullConstraint nullConstraint = (NullConstraint) constraintLeft;
        if (nullConstraint.equals(NullConstraint.NULL)) {
          return to.setConstraint(programState, shouldNotInverse().equals(booleanConstraint) ? nullConstraint : nullConstraint.inverse());
        } else if (shouldNotInverse().equals(booleanConstraint)) {
          return to.setConstraint(programState, nullConstraint);
        }
      }
      return ImmutableList.of(programState);
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

  abstract class UnarySymbolicValue extends ObjectSymbolicValue {
    protected SymbolicValue operand;

    public UnarySymbolicValue(int id) {
      super(id);
    }

    @Override
    public void computedFrom(List<SymbolicValue> symbolicValues) {
      Preconditions.checkArgument(symbolicValues.size() == 1);
      this.operand = symbolicValues.get(0);
    }

  }

  class NotSymbolicValue extends UnarySymbolicValue {

    public NotSymbolicValue(int id) {
      super(id);
    }

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      return operand.setConstraint(programState, booleanConstraint.inverse());
    }
  }

  class InstanceOfSymbolicValue extends UnarySymbolicValue {
    public InstanceOfSymbolicValue(int id) {
      super(id);
    }

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      if (BooleanConstraint.TRUE.equals(booleanConstraint)) {
        if (NullConstraint.NULL.equals(programState.getConstraint(operand))) {
          // irrealizable constraint : instance of true if operand is null
          return ImmutableList.of();
        }
        // if instanceof is true then we know for sure that expression is not null.
        List<ProgramState> ps = operand.setConstraint(programState, NullConstraint.NOT_NULL);
        if (ps.size() == 1 && ps.get(0).equals(programState)) {
          // FIXME we already know that operand is NOT NULL, so we add a different constraint to distinguish program state. Typed Constraint
          // should store the deduced type.
          return ImmutableList.of(programState.addConstraint(this, new ConstraintManager.TypedConstraint()));
        }
        return ps;
      }
      return ImmutableList.of(programState);
    }
  }

  abstract class BooleanExpressionSymbolicValue extends BinarySymbolicValue {

    protected BooleanExpressionSymbolicValue(int id) {
      super(id);
    }

    @Override
    BooleanConstraint shouldNotInverse() {
      return BooleanConstraint.TRUE;
    }
  }

  class AndSymbolicValue extends BooleanExpressionSymbolicValue {

    public AndSymbolicValue(int id) {
      super(id);
    }

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      final List<ProgramState> states = new ArrayList<>();
      if (BooleanConstraint.TRUE.equals(booleanConstraint)) {
        List<ProgramState> trueFirstOp = leftOp.setConstraint(programState, BooleanConstraint.TRUE);
        for (ProgramState ps : trueFirstOp) {
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.TRUE));
        }
      } else {
        List<ProgramState> falseFirstOp = leftOp.setConstraint(programState, BooleanConstraint.FALSE);
        List<ProgramState> trueFirstOp = leftOp.setConstraint(programState, BooleanConstraint.TRUE);
        for (ProgramState ps : falseFirstOp) {
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.TRUE));
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.FALSE));
        }
        for (ProgramState ps : trueFirstOp) {
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.FALSE));
        }
      }
      return states;
    }

    @Override
    public String toString() {
      return leftOp + " & " + rightOp;
    }
  }

  class OrSymbolicValue extends BooleanExpressionSymbolicValue {

    public OrSymbolicValue(int id) {
      super(id);
    }

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      final List<ProgramState> states = new ArrayList<>();
      if (BooleanConstraint.TRUE.equals(booleanConstraint)) {
        List<ProgramState> trueFirstOp = leftOp.setConstraint(programState, BooleanConstraint.TRUE);
        List<ProgramState> falseFirstOp = leftOp.setConstraint(programState, BooleanConstraint.FALSE);
        for (ProgramState ps : trueFirstOp) {
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.TRUE));
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.FALSE));
        }
        for (ProgramState ps : falseFirstOp) {
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.TRUE));
        }
      } else {
        List<ProgramState> falseFirstOp = leftOp.setConstraint(programState, BooleanConstraint.FALSE);
        for (ProgramState ps : falseFirstOp) {
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.FALSE));
        }
      }
      return states;
    }

    @Override
    public String toString() {
      return leftOp + " | " + rightOp;
    }
  }

  class XorSymbolicValue extends BooleanExpressionSymbolicValue {

    public XorSymbolicValue(int id) {
      super(id);
    }

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      final List<ProgramState> states = new ArrayList<>();
      if (BooleanConstraint.TRUE.equals(booleanConstraint)) {
        List<ProgramState> trueFirstOp = leftOp.setConstraint(programState, BooleanConstraint.TRUE);
        List<ProgramState> falseFirstOp = leftOp.setConstraint(programState, BooleanConstraint.FALSE);
        for (ProgramState ps : trueFirstOp) {
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.FALSE));
        }
        for (ProgramState ps : falseFirstOp) {
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.TRUE));
        }
      } else {
        List<ProgramState> trueFirstOp = leftOp.setConstraint(programState, BooleanConstraint.TRUE);
        List<ProgramState> falseFirstOp = leftOp.setConstraint(programState, BooleanConstraint.FALSE);
        for (ProgramState ps : trueFirstOp) {
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.TRUE));
        }
        for (ProgramState ps : falseFirstOp) {
          states.addAll(rightOp.setConstraint(ps, BooleanConstraint.FALSE));
        }
      }
      return states;
    }

    @Override
    public String toString() {
      return leftOp + " ^ " + rightOp;
    }
  }
}
