/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.resolve.Symbols;
import org.sonar.java.se.ExplodedGraphWalker;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.constraint.TypedConstraint;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

public class SymbolicValue {

  public static final SymbolicValue NULL_LITERAL = new SymbolicValue() {

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      return Collections.emptyList();
    }

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, Constraint constraint) {
      if(constraint instanceof ObjectConstraint) {
        return super.setConstraint(programState, (ObjectConstraint) constraint);
      }
      return Collections.singletonList(programState);
    }

    @Override
    public String toString() {
      return "SV_NULL";
    }
  };

  public static final SymbolicValue TRUE_LITERAL = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_TRUE";
    }
  };

  public static final SymbolicValue FALSE_LITERAL = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_FALSE";
    }
  };

  public static final List<SymbolicValue> PROTECTED_SYMBOLIC_VALUES = ImmutableList.of(
    NULL_LITERAL,
    TRUE_LITERAL,
    FALSE_LITERAL
  );

  private static int idGenerator;
  private final int id;

  public SymbolicValue() {
    id = idGenerator;
    idGenerator++;
  }

  @Override
  public int hashCode() {
    return 31 * id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SymbolicValue that = (SymbolicValue) o;
    Preconditions.checkState(id != that.id, "Impossible to have two SV with same id");
    return false;
  }

  public static boolean isDisposable(SymbolicValue symbolicValue) {
    if (symbolicValue instanceof NotSymbolicValue) {
      NotSymbolicValue notSV = (NotSymbolicValue) symbolicValue;
      return !(notSV.operand instanceof RelationalSymbolicValue);
    }
    return !PROTECTED_SYMBOLIC_VALUES.contains(symbolicValue) && !(symbolicValue instanceof RelationalSymbolicValue);
  }

  public boolean references(SymbolicValue other) {
    return false;
  }

  @Override
  public String toString() {
    return "SV_" + id;
  }

  public void computedFrom(List<ProgramState.SymbolicValueSymbol> symbolicValues) {
    // no op in general case
  }

  public List<Symbol> computedFromSymbols() {
    return Collections.emptyList();
  }

  public List<SymbolicValue> computedFrom() {
    return Collections.emptyList();
  }

  public List<ProgramState> setConstraint(ProgramState programState, ObjectConstraint nullConstraint) {
    Constraint constraint = programState.getConstraint(this, nullConstraint.getClass());
    if (constraint == null) {
      if(nullConstraint.isNull()) {
        // null constraints get rid of all other constraints
        ConstraintsByDomain onlyNullConstraint = ConstraintsByDomain.empty().put(ObjectConstraint.NULL);
        return Collections.singletonList(programState.addConstraints(this, onlyNullConstraint));
      } else {
        return Collections.singletonList(programState.addConstraint(this, nullConstraint));
      }
    } else if (constraint != nullConstraint) {
      return Collections.emptyList();
    }
    return Collections.singletonList(programState);
  }

  public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
    Constraint cstraint = programState.getConstraint(this, booleanConstraint.getClass());
    if (!booleanConstraint.isValidWith(cstraint)) {
      return Collections.emptyList();
    }
    return Collections.singletonList(programState.addConstraint(this, booleanConstraint));
  }

  public List<ProgramState> setConstraint(ProgramState programState, Constraint constraint) {
    if(constraint instanceof BooleanConstraint) {
      return setConstraint(programState, (BooleanConstraint) constraint);
    } else if(constraint instanceof ObjectConstraint) {
      return setConstraint(programState, (ObjectConstraint) constraint);
    }
    Constraint csrtaint = programState.getConstraint(this, constraint.getClass());
    if (constraint.isValidWith(csrtaint)) {
      return Collections.singletonList(programState.addConstraint(this, constraint));
    }
    return Collections.emptyList();
  }

  public ProgramState setSingleConstraint(ProgramState programState, ObjectConstraint nullConstraint) {
    final List<ProgramState> states = setConstraint(programState, nullConstraint);
    if (states.size() != 1) {
      throw new IllegalStateException("Only a single program state is expected at this location");
    }
    return states.get(0);
  }

  protected List<ProgramState> setConstraint(ProgramState state, Constraint constraint, Set<RelationalSymbolicValue> knownRelations) {
    return setConstraint(state, constraint);
  }

  public SymbolicValue wrappedValue() {
    return this;
  }

  public abstract static class UnarySymbolicValue extends SymbolicValue {
    protected SymbolicValue operand;
    private Symbol operandSymbol;


    @Override
    public boolean references(SymbolicValue other) {
      return operand.equals(other) || operand.references(other);
    }

    @Override
    public void computedFrom(List<ProgramState.SymbolicValueSymbol> symbolicValues) {
      Preconditions.checkArgument(symbolicValues.size() == 1);
      this.operand = symbolicValues.get(0).symbolicValue();
      this.operandSymbol = symbolicValues.get(0).symbol();
    }

    @Override
    public List<SymbolicValue> computedFrom() {
      return Collections.singletonList(operand);
    }

    @Override
    public List<Symbol> computedFromSymbols() {
      return operandSymbol == null ? operand.computedFromSymbols() : Collections.singletonList(operandSymbol);
    }
  }

  public static class ExceptionalSymbolicValue extends SymbolicValue {
    @Nullable
    private final Type exceptionType;

    public ExceptionalSymbolicValue(@Nullable Type exceptionType) {
      this.exceptionType = exceptionType;
    }

    @CheckForNull
    public Type exceptionType() {
      return exceptionType;
    }

    @Override
    public String toString() {
      return super.toString() + "_" + (exceptionType == null ? "!unknownException" : exceptionType.fullyQualifiedName()) + "!";
    }

  }

  public static class CaughtExceptionSymbolicValue extends SymbolicValue {
    private final ExceptionalSymbolicValue thrownSV;

    public CaughtExceptionSymbolicValue(ExceptionalSymbolicValue thrownSV) {
      this.thrownSV = thrownSV;
    }

    public ExceptionalSymbolicValue exception() {
      return thrownSV;
    }
  }

  public static class NotSymbolicValue extends UnarySymbolicValue {


    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      return operand.setConstraint(programState, booleanConstraint.inverse());
    }

    @Override
    public String toString() {
      return "!(" + operand + ")";
    }
  }

  public static class InstanceOfSymbolicValue extends UnarySymbolicValue {

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      if (booleanConstraint.isTrue()) {
        ObjectConstraint constraint = programState.getConstraint(operand, ObjectConstraint.class);
        if (constraint !=null && constraint.isNull()) {
          // irrealizable constraint : instance of true if operand is null
          return Collections.emptyList();
        }
        // if instanceof is true then we know for sure that expression is not null.
        List<ProgramState> ps = operand.setConstraint(programState, ObjectConstraint.NOT_NULL);
        if (ps.size() == 1 && ps.get(0).equals(programState)) {
          // FIXME we already know that operand is NOT NULL, so we add a different constraint to distinguish program state. Typed Constraint
          // should store the deduced type.
          return Collections.singletonList(programState.addConstraint(this, new TypedConstraint(Symbols.unknownType.fullyQualifiedName())));
        }
        return ps;
      }
      return Collections.singletonList(programState);
    }
  }

  public abstract static class BooleanExpressionSymbolicValue extends BinarySymbolicValue {

    protected static void addStates(List<ProgramState> states, List<ProgramState> newStates) {
      if (states.size() > ExplodedGraphWalker.MAX_NESTED_BOOLEAN_STATES || newStates.size() > ExplodedGraphWalker.MAX_NESTED_BOOLEAN_STATES) {
        throw new ExplodedGraphWalker.TooManyNestedBooleanStatesException();
      }
      states.addAll(newStates);
    }
  }

  public static class AndSymbolicValue extends BooleanExpressionSymbolicValue {


    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      final List<ProgramState> states = new ArrayList<>();
      if (booleanConstraint.isFalse()) {
        List<ProgramState> falseFirstOp = leftOp.setConstraint(programState, BooleanConstraint.FALSE);
        for (ProgramState ps : falseFirstOp) {
          addStates(states, rightOp.setConstraint(ps, BooleanConstraint.TRUE));
          addStates(states, rightOp.setConstraint(ps, BooleanConstraint.FALSE));
        }
      }
      List<ProgramState> trueFirstOp = leftOp.setConstraint(programState, BooleanConstraint.TRUE);
      for (ProgramState ps : trueFirstOp) {
        addStates(states, rightOp.setConstraint(ps, booleanConstraint));
      }
      return states;
    }

    @Override
    public String toString() {
      return leftOp + " & " + rightOp;
    }
  }

  public static class OrSymbolicValue extends BooleanExpressionSymbolicValue {


    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      List<ProgramState> states = new ArrayList<>();
      if (booleanConstraint.isTrue()) {
        List<ProgramState> trueFirstOp = leftOp.setConstraint(programState, BooleanConstraint.TRUE);
        for (ProgramState ps : trueFirstOp) {
          addStates(states, rightOp.setConstraint(ps, BooleanConstraint.TRUE));
          addStates(states, rightOp.setConstraint(ps, BooleanConstraint.FALSE));
        }
      }
      List<ProgramState> falseFirstOp = leftOp.setConstraint(programState, BooleanConstraint.FALSE);
      for (ProgramState ps : falseFirstOp) {
        addStates(states, rightOp.setConstraint(ps, booleanConstraint));
      }
      return states;
    }

    @Override
    public String toString() {
      return leftOp + " | " + rightOp;
    }
  }

  public static class XorSymbolicValue extends BooleanExpressionSymbolicValue {


    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      List<ProgramState> states = new ArrayList<>();
      List<ProgramState> trueFirstOp = leftOp.setConstraint(programState, BooleanConstraint.TRUE);
      for (ProgramState ps : trueFirstOp) {
        addStates(states, rightOp.setConstraint(ps, booleanConstraint.inverse()));
      }
      List<ProgramState> falseFirstOp = leftOp.setConstraint(programState, BooleanConstraint.FALSE);
      for (ProgramState ps : falseFirstOp) {
        addStates(states, rightOp.setConstraint(ps, booleanConstraint));
      }
      return states;
    }

    @Override
    public String toString() {
      return leftOp + " ^ " + rightOp;
    }
  }

}
