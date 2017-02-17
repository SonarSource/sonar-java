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
package org.sonar.java.se;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.sonar.java.collections.PCollections;
import org.sonar.java.collections.PMap;
import org.sonar.java.collections.PStack;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.BinaryRelation;
import org.sonar.java.se.symbolicvalues.BinarySymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

public class ProgramState {

  public static class Pop {

    public final ProgramState state;
    public final List<SymbolicValue> values;

    public Pop(ProgramState programState, List<SymbolicValue> result) {
      state = programState;
      values = result;
    }

  }

  private int hashCode;

  private final int constraintSize;
  public static final ProgramState EMPTY_STATE = new ProgramState(
    PCollections.emptyMap(),
    PCollections.emptyMap(),
    PCollections.<SymbolicValue, Constraint>emptyMap()
      .put(SymbolicValue.NULL_LITERAL, ObjectConstraint.nullConstraint())
      .put(SymbolicValue.TRUE_LITERAL, BooleanConstraint.TRUE)
      .put(SymbolicValue.FALSE_LITERAL, BooleanConstraint.FALSE),
    PCollections.emptyMap(),
    PCollections.emptyStack(),
    null);

  private final PMap<ProgramPoint, Integer> visitedPoints;

  @Nullable
  Symbol lastEvaluated;

  private final PStack<SymbolicValue> stack;
  private final PMap<SymbolicValue, Integer> references;
  private SymbolicValue exitSymbolicValue;
  final PMap<Symbol, SymbolicValue> values;
  final PMap<SymbolicValue, Constraint> constraints;

  private ProgramState(PMap<Symbol, SymbolicValue> values, PMap<SymbolicValue, Integer> references,
    PMap<SymbolicValue, Constraint> constraints, PMap<ProgramPoint, Integer> visitedPoints,
    PStack<SymbolicValue> stack, SymbolicValue exitSymbolicValue) {
    this.values = values;
    this.references = references;
    this.constraints = constraints;
    this.visitedPoints = visitedPoints;
    this.stack = stack;
    this.exitSymbolicValue = exitSymbolicValue;
    constraintSize = 3;
  }
  private ProgramState(Symbol symbol, PMap<Symbol, SymbolicValue> values, PMap<SymbolicValue, Integer> references,
                       PMap<SymbolicValue, Constraint> constraints, PMap<ProgramPoint, Integer> visitedPoints,
                       PStack<SymbolicValue> stack, SymbolicValue exitSymbolicValue) {
    this(values, references, constraints, visitedPoints, stack, exitSymbolicValue);
    lastEvaluated = symbol;
  }

  private ProgramState(ProgramState ps, PStack<SymbolicValue> newStack) {
    values = ps.values;
    references = ps.references;
    constraints = ps.constraints;
    constraintSize = ps.constraintSize;
    visitedPoints = ps.visitedPoints;
    exitSymbolicValue = ps.exitSymbolicValue;
    stack = newStack;
  }

  private ProgramState(ProgramState ps, PMap<SymbolicValue, Constraint> newConstraints) {
    values = ps.values;
    references = ps.references;
    constraints = newConstraints;
    constraintSize = ps.constraintSize + 1;
    visitedPoints = ps.visitedPoints;
    exitSymbolicValue = ps.exitSymbolicValue;
    this.stack = ps.stack;
  }

  public ProgramState stackValue(SymbolicValue sv) {
    return new ProgramState(this, stack.push(sv));
  }

  ProgramState clearStack() {
    return stack.isEmpty() ? this : new ProgramState(this, PCollections.emptyStack());
  }

  public Pop unstackValue(int nbElements) {
    if (nbElements == 0 || stack.isEmpty()) {
      return new Pop(this, Collections.emptyList());
    }

    // FIXME can be made more efficient by reusing sub collection of PStack instead of copying to the new list
    PStack<SymbolicValue> newStack = stack;
    List<SymbolicValue> result = Lists.newArrayList();
    for (int i = 0; i < nbElements && !newStack.isEmpty(); i++) {
      result.add(newStack.peek());
      newStack = newStack.pop();
    }
    return new Pop(new ProgramState(this, newStack), result);
  }

  @CheckForNull
  public SymbolicValue peekValue() {
    return stack.isEmpty() ? null : stack.peek();
  }

  public SymbolicValue peekValue(int i) {
    return stack.peek(i);
  }

  public List<SymbolicValue> peekValues(int n) {
    ImmutableList.Builder<SymbolicValue> result = ImmutableList.builder();
    PStack<SymbolicValue> tmpStack = this.stack;
    for (int i = 0; i < n; i++) {
      result.add(tmpStack.peek());
      tmpStack = tmpStack.pop();
    }
    return result.build();
  }

  int numberOfTimeVisited(ProgramPoint programPoint) {
    Integer count = visitedPoints.get(programPoint);
    return count == null ? 0 : count;
  }

  @CheckForNull
  public Symbol getLastEvaluated() {
    return lastEvaluated;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProgramState that = (ProgramState) o;
    return Objects.equals(values, that.values) &&
      Objects.equals(constraints, that.constraints) &&
      Objects.equals(exitSymbolicValue, that.exitSymbolicValue) &&
      Objects.equals(peekValue(), that.peekValue());
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      hashCode = Objects.hash(values, constraints, peekValue());
    }
    return hashCode;
  }

  @Override
  public String toString() {
    return "{" + values.toString() + "}  {" + constraints.toString() + "}" + " { " + stack.toString() + " }" + " { " + getLastEvaluated() + " } ";
  }

  public ProgramState addConstraint(SymbolicValue symbolicValue, Constraint constraint) {
    PMap<SymbolicValue, Constraint> newConstraints = constraints.put(symbolicValue, constraint);
    if (newConstraints != constraints) {
      return new ProgramState(this, newConstraints);
    }
    return this;
  }

  @VisibleForTesting
  public ProgramState put(Symbol symbol, SymbolicValue value) {
    if (symbol.isUnknown() || isVolatileField(symbol)) {
      return this;
    }
    SymbolicValue oldValue = values.get(symbol);
    if (oldValue == null || oldValue != value) {
      PMap<SymbolicValue, Integer> newReferences = references;
      if (oldValue != null) {
        newReferences = decreaseReference(newReferences, oldValue);
      }
      newReferences = increaseReference(newReferences, value);
      PMap<Symbol, SymbolicValue> newValues = values.put(symbol, value);
      return new ProgramState(symbol, newValues, newReferences, constraints, visitedPoints, stack, exitSymbolicValue);
    }
    if(lastEvaluated == null) {
      lastEvaluated = symbol;
    }
    return this;
  }

  private static boolean isVolatileField(Symbol symbol) {
    return isField(symbol) && symbol.isVolatile();
  }

  private static PMap<SymbolicValue, Integer> decreaseReference(PMap<SymbolicValue, Integer> givenReferences, SymbolicValue sv) {
    Integer value = givenReferences.get(sv);
    Preconditions.checkNotNull(value);
    return givenReferences.put(sv, value - 1);
  }

  private static PMap<SymbolicValue, Integer> increaseReference(PMap<SymbolicValue, Integer> givenReferences, SymbolicValue sv) {
    Integer value = givenReferences.get(sv);
    if (value == null) {
      return givenReferences.put(sv, 1);
    } else {
      return givenReferences.put(sv, value + 1);
    }
  }

  private static boolean isDisposable(SymbolicValue symbolicValue, @Nullable Object constraint) {
    return SymbolicValue.isDisposable(symbolicValue) && (constraint == null || !(constraint instanceof ObjectConstraint) || ((ObjectConstraint) constraint).isDisposable());
  }

  private static boolean inStack(PStack<SymbolicValue> stack, SymbolicValue symbolicValue) {
    return stack.anyMatch(sv -> sv.equals(symbolicValue) || sv.references(symbolicValue));
  }

  private static boolean isLocalVariable(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.owner().isMethodSymbol();
  }

  public ProgramState cleanupDeadSymbols(Set<Symbol> liveVariables, Collection<SymbolicValue> protectedSymbolicValues) {
    class CleanAction implements BiConsumer<Symbol, SymbolicValue> {
      boolean newProgramState = false;
      PMap<Symbol, SymbolicValue> newValues = values;
      PMap<SymbolicValue, Integer> newReferences = references;
      PMap<SymbolicValue, Constraint> newConstraints = constraints;

      @Override
      public void accept(Symbol symbol, SymbolicValue symbolicValue) {
        if (isLocalVariable(symbol) && !liveVariables.contains(symbol) && !protectedSymbolicValues.contains(symbolicValue)) {
          newProgramState = true;
          newValues = newValues.remove(symbol);
          newReferences = decreaseReference(newReferences, symbolicValue);
          if (!isReachable(symbolicValue, newReferences) && isDisposable(symbolicValue, newConstraints.get(symbolicValue)) && !inStack(stack, symbolicValue)) {
            newConstraints = newConstraints.remove(symbolicValue);
            newReferences = newReferences.remove(symbolicValue);
          }
        }
      }
    }
    CleanAction cleanAction = new CleanAction();
    values.forEach(cleanAction);
    return cleanAction.newProgramState ? new ProgramState(cleanAction.newValues, cleanAction.newReferences, cleanAction.newConstraints, visitedPoints, stack, exitSymbolicValue)
      : this;
  }

  public ProgramState cleanupConstraints(Collection<SymbolicValue> protectedSymbolicValues) {
    class CleanAction implements BiConsumer<SymbolicValue, Constraint> {
      boolean newProgramState = false;
      PMap<SymbolicValue, Constraint> newConstraints = constraints;
      PMap<SymbolicValue, Integer> newReferences = references;

      @Override
      public void accept(SymbolicValue symbolicValue, Constraint constraint) {
        if (!protectedSymbolicValues.contains(symbolicValue)
          && !isReachable(symbolicValue, newReferences)
          && isDisposable(symbolicValue, constraint)
          && !inStack(stack, symbolicValue)) {
          newProgramState = true;
          newConstraints = newConstraints.remove(symbolicValue);
          newReferences = newReferences.remove(symbolicValue);
        }
      }
    }
    CleanAction cleanAction = new CleanAction();
    constraints.forEach(cleanAction);
    return cleanAction.newProgramState ? new ProgramState(values, cleanAction.newReferences, cleanAction.newConstraints, visitedPoints, stack, exitSymbolicValue) : this;
  }

  public ProgramState resetFieldValues(ConstraintManager constraintManager) {
    final List<VariableTree> variableTrees = new ArrayList<>();
    values.forEach((symbol, symbolicValue) -> {
      if (isField(symbol)) {
        VariableTree variable = ((Symbol.VariableSymbol) symbol).declaration();
        if (variable != null) {
          variableTrees.add(variable);
        }
      }
    });
    if (variableTrees.isEmpty()) {
      return this;
    }
    PMap<Symbol, SymbolicValue> newValues = values;
    PMap<SymbolicValue, Integer> newReferences = references;
    for (VariableTree variableTree : variableTrees) {
      Symbol symbol = variableTree.symbol();
      SymbolicValue oldValue = newValues.get(symbol);
      if (oldValue != null) {
        newReferences = decreaseReference(newReferences, oldValue);
      }
      SymbolicValue newValue = constraintManager.createSymbolicValue(variableTree);
      newValues = newValues.put(symbol, newValue);
      newReferences = increaseReference(newReferences, newValue);
    }
    return new ProgramState(newValues, newReferences, constraints, visitedPoints, stack, exitSymbolicValue);
  }

  public static boolean isField(Symbol symbol) {
    return symbol.isVariableSymbol() && !symbol.owner().isMethodSymbol();
  }

  private static boolean isReachable(SymbolicValue symbolicValue, PMap<SymbolicValue, Integer> references) {
    Integer integer = references.get(symbolicValue);
    return integer != null && integer > 0;
  }

  public boolean canReach(SymbolicValue symbolicValue) {
    return isReachable(symbolicValue, references);
  }

  public ProgramState visitedPoint(ProgramPoint programPoint, int nbOfVisit) {
    return new ProgramState(values, references, constraints, visitedPoints.put(programPoint, nbOfVisit), stack, exitSymbolicValue);
  }

  @CheckForNull
  public Constraint getConstraint(SymbolicValue sv) {
    return constraints.get(sv);
  }

  public int constraintsSize() {
    return constraintSize;
  }

  @CheckForNull
  public SymbolicValue getValue(Symbol symbol) {
    return values.get(symbol);
  }

  public Map<SymbolicValue, ObjectConstraint<ObjectConstraint.Status>> getValuesWithConstraints(final Object state) {
    final Map<SymbolicValue, ObjectConstraint<ObjectConstraint.Status>> result = new HashMap<>();
    constraints.forEach((symbolicValue, valueConstraint) -> {
      if (valueConstraint instanceof ObjectConstraint) {
        ObjectConstraint<ObjectConstraint.Status> constraint = (ObjectConstraint<ObjectConstraint.Status>) valueConstraint;
        if (constraint.hasStatus(state)) {
          result.put(symbolicValue, constraint);
        }
      }
    });
    return result;
  }

  public List<BinaryRelation> getKnownRelations() {
    final List<BinaryRelation> knownRelations = new ArrayList<>();
    constraints.forEach((symbolicValue, constraint) -> {
      BinaryRelation relation = symbolicValue.binaryRelation();
      if (relation != null) {
        if (BooleanConstraint.TRUE.equals(constraint)) {
          knownRelations.add(relation);
        } else if (BooleanConstraint.FALSE.equals(constraint)) {
          knownRelations.add(relation.inverse());
        }
      }
    });
    return knownRelations;
  }

  @CheckForNull
  public <S extends ObjectConstraint.Status> ObjectConstraint<S> getConstraintWithStatus(SymbolicValue value, S aState) {
    final Object constraint = getConstraint(value.wrappedValue());
    if (constraint instanceof ObjectConstraint) {
      ObjectConstraint oConstraint = (ObjectConstraint) constraint;
      if (oConstraint.hasStatus(aState)) {
        return oConstraint;
      }
    }
    return null;
  }

  public void storeExitValue() {
    this.exitSymbolicValue = peekValue();
  }

  @CheckForNull
  public SymbolicValue exitValue() {
    return this.exitSymbolicValue;
  }

  public boolean exitingOnRuntimeException() {
    return exitSymbolicValue instanceof SymbolicValue.ExceptionalSymbolicValue && ((SymbolicValue.ExceptionalSymbolicValue) exitSymbolicValue).exceptionType() == null;
  }

  Set<LearnedConstraint> learnedConstraints(ProgramState parent) {
    ImmutableSet.Builder<LearnedConstraint> result = ImmutableSet.builder();
    constraints.forEach((sv, childConstraint) -> {
      Constraint parentConstraint = parent.getConstraint(sv);
      if (parentConstraint == null || !parentConstraint.equals(childConstraint)) {
        addLearnedConstraint(result, sv, childConstraint);
      }
    });
    return result.build();
  }

  private static void addLearnedConstraint(ImmutableSet.Builder<LearnedConstraint> result, SymbolicValue sv, Constraint c) {
    result.add(new LearnedConstraint(sv, c));
    // FIXME this might end up adding twice the same SV in learned constraints. Safe because null constraints are filtered anyway
    if (sv instanceof BinarySymbolicValue) {
      BinarySymbolicValue binarySymbolicValue = (BinarySymbolicValue) sv;
      addLearnedConstraint(result, binarySymbolicValue.getLeftOp(), null);
      addLearnedConstraint(result, binarySymbolicValue.getRightOp(), null);
    }
  }

  Set<LearnedAssociation> learnedAssociations(ProgramState parent) {
    ImmutableSet.Builder<LearnedAssociation> result = ImmutableSet.builder();
    values.forEach((s, sv) -> {
      if (parent.getValue(s) != sv) {
        result.add(new LearnedAssociation(sv, s));
      }
    });
    return result.build();
  }

}
