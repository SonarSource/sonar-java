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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import org.sonar.java.collections.PMap;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RelationalSymbolicValue extends BinarySymbolicValue {

  public enum Kind {
    EQUAL("=="),
    NOT_EQUAL("!="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    METHOD_EQUALS(".EQ."),
    NOT_METHOD_EQUALS(".NE.");

    final String operand;

    Kind(String operand) {
      this.operand = operand;
    }

    public Kind inverse() {
      switch (this) {
        case EQUAL:
          return NOT_EQUAL;
        case NOT_EQUAL:
          return EQUAL;
        case GREATER_THAN:
          return LESS_THAN_OR_EQUAL;
        case GREATER_THAN_OR_EQUAL:
          return LESS_THAN;
        case LESS_THAN:
          return GREATER_THAN_OR_EQUAL;
        case LESS_THAN_OR_EQUAL:
          return GREATER_THAN;
        case METHOD_EQUALS:
          return NOT_METHOD_EQUALS;
        case NOT_METHOD_EQUALS:
          return METHOD_EQUALS;
        default:
          throw new IllegalStateException("Unsupported relation!");
      }
    }

    public Kind symmetric() {
      Kind sym;
      switch (this) {
        case GREATER_THAN:
          sym = LESS_THAN;
          break;
        case GREATER_THAN_OR_EQUAL:
          sym = LESS_THAN_OR_EQUAL;
          break;
        case LESS_THAN:
          sym = GREATER_THAN;
          break;
        case LESS_THAN_OR_EQUAL:
          sym = GREATER_THAN_OR_EQUAL;
          break;
        default:
          sym = this;
      }
      return sym;
    }
  }

  final Kind kind;
  private BinaryRelation binaryRelation;

  public RelationalSymbolicValue(Kind kind) {
    this.kind = kind;
  }

  @VisibleForTesting
  RelationalSymbolicValue(Kind kind, SymbolicValue leftOp, SymbolicValue rightOp) {
    this.kind = kind;
    this.leftOp = leftOp;
    this.rightOp = rightOp;
  }

  @Override
  public List<ProgramState> setConstraint(ProgramState initialProgramState, BooleanConstraint booleanConstraint) {
    if (!checkRelation(booleanConstraint, initialProgramState)) {
      return ImmutableList.of();
    }
    return booleanConstraint == BooleanConstraint.TRUE ? getNewProgramStates(initialProgramState) : inverse().getNewProgramStates(initialProgramState);
  }

  private List<ProgramState> getNewProgramStates(ProgramState initialProgramState) {
    List<RelationalSymbolicValue> newRelations = transitiveRelations(initialProgramState);
    newRelations.add(this);

    List<ProgramState> programStates = new ArrayList<>();
    programStates.add(initialProgramState);
    for (RelationalSymbolicValue relationalSymbolicValue : newRelations) {
      List<ProgramState> intermediateStates = new ArrayList<>();
      for (ProgramState programState: programStates) {
        intermediateStates.addAll(relationalSymbolicValue.copyAllConstraints(programState));
      }
      programStates = intermediateStates;
    }
    return programStates;
  }

  RelationalSymbolicValue inverse() {
    return new RelationalSymbolicValue(kind.inverse(), leftOp, rightOp);
  }

  private List<ProgramState> copyAllConstraints(ProgramState programState) {
    List<ProgramState> results = new ArrayList<>();
    List<ProgramState> copiedConstraints = copyConstraint(leftOp, rightOp, programState);
    if (Kind.METHOD_EQUALS == kind || Kind.NOT_METHOD_EQUALS == kind) {
      copiedConstraints = addNullConstraintsForBooleanWrapper(programState, copiedConstraints);
    }
    for (ProgramState ps : copiedConstraints) {
      List<ProgramState> copiedConstraintsRightToLeft = copyConstraint(rightOp, leftOp, ps);
      if (copiedConstraintsRightToLeft.size() == 1 && copiedConstraintsRightToLeft.get(0).equals(programState)) {
        results.add(programState.addConstraint(this, BooleanConstraint.TRUE));
      } else {
        results.addAll(copiedConstraintsRightToLeft);
      }
    }
    return results;
  }

  private List<ProgramState> addNullConstraintsForBooleanWrapper(ProgramState initialProgramState, List<ProgramState> copiedConstraints) {
    BooleanConstraint leftConstraint = initialProgramState.getConstraint(leftOp, BooleanConstraint.class);
    BooleanConstraint rightConstraint = initialProgramState.getConstraint(rightOp, BooleanConstraint.class);
    if (leftConstraint != null && rightConstraint == null && !isEquality()) {
      List<ProgramState> nullConstraints = copiedConstraints.stream()
        .flatMap(ps -> rightOp.setConstraint(ps, ObjectConstraint.NULL).stream())
        .map(ps -> ps.removeConstraintsOnDomain(rightOp, BooleanConstraint.class)
      ).collect(Collectors.toList());
      return ImmutableList.<ProgramState>builder().addAll(copiedConstraints).addAll(nullConstraints).build();
    }
    return copiedConstraints;
  }

  private List<ProgramState> copyConstraint(SymbolicValue from, SymbolicValue to, ProgramState programState) {
    ProgramState newState = programState;
    if (programState.canReach(from) || programState.canReach(to)) {
      newState = programState.addConstraint(this, BooleanConstraint.TRUE);
    }
    return copyConstraintFromTo(from, to, newState);
  }

  private List<ProgramState> copyConstraintFromTo(SymbolicValue from, SymbolicValue to, ProgramState programState) {
    List<ProgramState> states = new ArrayList<>();
    states.add(programState);
    PMap<Class<? extends Constraint>, Constraint> leftConstraints = programState.getConstraints(from);
    if (leftConstraints != null) {
      leftConstraints.forEach((d, c) -> {
        List<ProgramState> newStates = new ArrayList<>();
        Constraint constraint = c.copyOver(kind);
        states.forEach(state -> {
          if (constraint == null) {
            PMap<Class<? extends Constraint>, Constraint> constraints = state.getConstraints(to);
            if (constraints != null) {
              newStates.add(state.removeConstraintsOnDomain(to, c.getClass()));
            } else {
              newStates.add(state);
            }
          } else {
            // special handling of copying inversed non-null constraint
            if (ObjectConstraint.NULL == constraint && c != constraint) {
              newStates.add(state);
            } else {
              newStates.addAll(to.setConstraint(state, constraint));
            }
          }
        });
        states.clear();
        states.addAll(newStates);
      });
    }
    return states;
  }

  private boolean checkRelation(BooleanConstraint booleanConstraint, ProgramState programState) {
    RelationState relationState = binaryRelation().resolveState(programState.getKnownRelations());
    return !relationState.rejects(booleanConstraint);
  }

  private List<RelationalSymbolicValue> transitiveRelations(ProgramState programState) {
    BinaryRelation relation = binaryRelation();
    return programState.getKnownRelations().stream()
      .map(r -> r.deduceTransitiveOrSimplified(relation))
      .filter(Objects::nonNull)
      .map(RelationalSymbolicValue::binaryRelationToSymbolicValue)
      .collect(Collectors.toList());
  }

  private static RelationalSymbolicValue binaryRelationToSymbolicValue(BinaryRelation binaryRelation) {
    switch (binaryRelation.kind) {
      case EQUAL:
      case METHOD_EQUALS:
      case NOT_EQUAL:
      case NOT_METHOD_EQUALS:
      case LESS_THAN:
      case GREATER_THAN_OR_EQUAL:
        return new RelationalSymbolicValue(binaryRelation.kind, binaryRelation.leftOp, binaryRelation.rightOp);
      case GREATER_THAN:
      case LESS_THAN_OR_EQUAL:
        return new RelationalSymbolicValue(binaryRelation.kind.symmetric(), binaryRelation.rightOp, binaryRelation.leftOp);
      default:
        throw new IllegalStateException("Unable to convert to relational SV " + binaryRelation);
    }
  }

  @Override
  public BinaryRelation binaryRelation() {
    if (binaryRelation == null) {
      binaryRelation = BinaryRelation.binaryRelation(kind, leftOp, rightOp);
    }
    return binaryRelation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RelationalSymbolicValue that = (RelationalSymbolicValue) o;
    if (kind != that.kind) {
      return false;
    }
    if (leftOp.equals(that.leftOp) && rightOp.equals(that.rightOp)) {
      return true;
    }
    return isEquality() && leftOp.equals(that.rightOp) && rightOp.equals(that.leftOp);
  }

  private boolean isEquality() {
    return kind == Kind.EQUAL || kind == Kind.METHOD_EQUALS;
  }

  @Override
  public int hashCode() {
    // hashCode doesn't depend on order of operands, to make commutative operators equal when operands are swapped
    return kind.hashCode() + leftOp.hashCode() + rightOp.hashCode();
  }

  @Override
  public String toString() {
    return leftOp.toString() + kind.operand + rightOp.toString();
  }
}
