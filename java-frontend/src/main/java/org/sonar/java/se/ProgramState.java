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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.java.collections.AVLTree;
import org.sonar.java.collections.PMap;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.BinaryRelation;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    AVLTree.<Symbol, SymbolicValue>create(),
    AVLTree.<SymbolicValue, Integer>create(),
    AVLTree.<SymbolicValue, Constraint>create()
      .put(SymbolicValue.NULL_LITERAL, ObjectConstraint.nullConstraint())
      .put(SymbolicValue.TRUE_LITERAL, BooleanConstraint.TRUE)
      .put(SymbolicValue.FALSE_LITERAL, BooleanConstraint.FALSE),
    AVLTree.<ExplodedGraph.ProgramPoint, Integer>create(),
    Lists.<SymbolicValue>newLinkedList());

  private final PMap<ExplodedGraph.ProgramPoint, Integer> visitedPoints;

  private final Deque<SymbolicValue> stack;
  private final PMap<Symbol, SymbolicValue> values;
  private final PMap<SymbolicValue, Integer> references;
  private final PMap<SymbolicValue, Constraint> constraints;

  private ProgramState(PMap<Symbol, SymbolicValue> values, PMap<SymbolicValue, Integer> references,
    PMap<SymbolicValue, Constraint> constraints, PMap<ExplodedGraph.ProgramPoint, Integer> visitedPoints,
    Deque<SymbolicValue> stack) {
    this.values = values;
    this.references = references;
    this.constraints = constraints;
    this.visitedPoints = visitedPoints;
    this.stack = stack;
    constraintSize = 3;
  }

  private ProgramState(ProgramState ps, Deque<SymbolicValue> newStack) {
    values = ps.values;
    references = ps.references;
    constraints = ps.constraints;
    constraintSize = ps.constraintSize;
    visitedPoints = ps.visitedPoints;
    stack = newStack;
  }

  private ProgramState(ProgramState ps, PMap<SymbolicValue, Constraint> newConstraints) {
    values = ps.values;
    references = ps.references;
    constraints = newConstraints;
    constraintSize = ps.constraintSize + 1;
    visitedPoints = ps.visitedPoints;
    this.stack = ps.stack;
  }

  ProgramState stackValue(SymbolicValue sv) {
    Deque<SymbolicValue> newStack = new LinkedList<>(stack);
    newStack.push(sv);
    return new ProgramState(this, newStack);
  }

  ProgramState clearStack() {
    return unstackValue(stack.size()).state;
  }

  public Pop unstackValue(int nbElements) {
    if (nbElements == 0) {
      return new Pop(this, Collections.<SymbolicValue>emptyList());
    }
    Preconditions.checkArgument(stack.size() >= nbElements, nbElements);
    Deque<SymbolicValue> newStack = new LinkedList<>(stack);
    List<SymbolicValue> result = Lists.newArrayList();
    for (int i = 0; i < nbElements; i++) {
      result.add(newStack.pop());
    }
    return new Pop(new ProgramState(this, newStack), result);
  }

  public SymbolicValue peekValue() {
    return stack.peek();
  }

  public List<SymbolicValue> peekValues(int n) {
    if (n > stack.size()) {
      throw new IllegalStateException("At least " + n + " values were expected on the stack!");
    }
    return ImmutableList.copyOf(stack).subList(0, n);
  }

  int numberOfTimeVisited(ExplodedGraph.ProgramPoint programPoint) {
    Integer count = visitedPoints.get(programPoint);
    return count == null ? 0 : count;
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
    return "{" + values.toString() + "}  {" + constraints.toString() + "}" + " { " + stack.toString() + " }";
  }

  public ProgramState addConstraint(SymbolicValue symbolicValue, Constraint constraint) {
    PMap<SymbolicValue, Constraint> newConstraints = constraints.put(symbolicValue, constraint);
    if (newConstraints != constraints) {
      return new ProgramState(this, newConstraints);
    }
    return this;
  }

  ProgramState put(Symbol symbol, SymbolicValue value) {
    if (symbol.isUnknown()) {
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
      return new ProgramState(newValues, newReferences, constraints, visitedPoints, stack);
    }
    return this;
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

  private static boolean inStack(Deque<SymbolicValue> stack, SymbolicValue symbolicValue) {
    for (SymbolicValue value : stack) {
      if (value.equals(symbolicValue) || value.references(symbolicValue)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isLocalVariable(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.owner().isMethodSymbol();
  }

  public ProgramState cleanupDeadSymbols(Set<Symbol> liveVariables) {
    PMap<Symbol, SymbolicValue> newValues = values;
    PMap<SymbolicValue, Integer> newReferences = references;
    PMap<SymbolicValue, Constraint> newConstraints = constraints;
    boolean newProgramState = false;
    for (Iterator<Map.Entry<Symbol, SymbolicValue>> iter = newValues.entriesIterator(); iter.hasNext();) {
      Map.Entry<Symbol, SymbolicValue> next = iter.next();
      Symbol symbol = next.getKey();
      if (isLocalVariable(symbol) && !liveVariables.contains(symbol)) {
        if (!newProgramState) {
          newProgramState = true;
        }
        SymbolicValue symbolicValue = next.getValue();
        newValues = newValues.remove(symbol);
        newReferences = decreaseReference(newReferences, symbolicValue);
        if (!isReachable(symbolicValue, newReferences) && isDisposable(symbolicValue, newConstraints.get(symbolicValue)) && !inStack(stack, symbolicValue)) {
          newConstraints = newConstraints.remove(symbolicValue);
          newReferences = newReferences.remove(symbolicValue);
        }
      }
    }
    return newProgramState ? new ProgramState(newValues, newReferences, newConstraints, visitedPoints, stack) : this;
  }

  public ProgramState cleanupConstraints() {
    PMap<SymbolicValue, Constraint> newConstraints = constraints;
    PMap<SymbolicValue, Integer> newReferences = references;
    boolean newProgramState = false;
    for (Iterator<Map.Entry<SymbolicValue, Constraint>> iter = newConstraints.entriesIterator(); iter.hasNext();) {
      Map.Entry<SymbolicValue, Constraint> next = iter.next();
      SymbolicValue symbolicValue = next.getKey();
      if (!isReachable(symbolicValue, newReferences) && isDisposable(symbolicValue, next.getValue()) && !inStack(stack, symbolicValue)) {
        if (!newProgramState) {
          newProgramState = true;
        }
        newConstraints = newConstraints.remove(symbolicValue);
        newReferences = newReferences.remove(symbolicValue);
      }
    }
    return newProgramState ? new ProgramState(values, newReferences, newConstraints, visitedPoints, stack) : this;
  }

  public ProgramState resetFieldValues(ConstraintManager constraintManager) {
    final List<VariableTree> variableTrees = new ArrayList<>();
    values.forEach(new PMap.Consumer<Symbol, SymbolicValue>() {
      @Override
      public void accept(Symbol symbol, SymbolicValue value) {
        if (isField(symbol)) {
          VariableTree variable = ((Symbol.VariableSymbol) symbol).declaration();
          if (variable != null) {
            variableTrees.add(variable);
          }
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
    return new ProgramState(newValues, newReferences, constraints, visitedPoints, stack);
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

  public ProgramState visitedPoint(ExplodedGraph.ProgramPoint programPoint, int nbOfVisit) {
    return new ProgramState(values, references, constraints, visitedPoints.put(programPoint, nbOfVisit), stack);
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

  public Map<SymbolicValue, ObjectConstraint> getValuesWithConstraints(final Object state) {
    final Map<SymbolicValue, ObjectConstraint> result = new HashMap<>();
    constraints.forEach(new PMap.Consumer<SymbolicValue, Constraint>() {
      @Override
      public void accept(SymbolicValue value, Constraint valueConstraint) {
        if (valueConstraint instanceof ObjectConstraint) {
          ObjectConstraint constraint = (ObjectConstraint) valueConstraint;
          if (constraint.hasStatus(state)) {
            result.put(value, constraint);
          }
        }
      }
    });
    return result;
  }

  public List<ObjectConstraint> getFieldConstraints(final Object state) {
    final Set<SymbolicValue> valuesAssignedToFields = getFieldValues();
    final List<ObjectConstraint> result = new ArrayList<>();
    constraints.forEach(new PMap.Consumer<SymbolicValue, Constraint>() {
      @Override
      public void accept(SymbolicValue value, Constraint valueConstraint) {
        if (valueConstraint instanceof ObjectConstraint && !valuesAssignedToFields.contains(value)) {
          ObjectConstraint constraint = (ObjectConstraint) valueConstraint;
          if (constraint.hasStatus(state)) {
            result.add(constraint);
          }
        }
      }
    });
    return result;
  }

  public Set<SymbolicValue> getFieldValues() {
    final Set<SymbolicValue> fieldValues = new HashSet<>();
    values.forEach(new PMap.Consumer<Symbol, SymbolicValue>() {
      @Override
      public void accept(Symbol key, SymbolicValue value) {
        if (isField(key)) {
          fieldValues.add(value);
        }
      }
    });
    return fieldValues;
  }

  public List<BinaryRelation> getKnownRelations() {
    final List<BinaryRelation> knownRelations = new ArrayList<>();
    constraints.forEach(new PMap.Consumer<SymbolicValue, Constraint>() {
      @Override
      public void accept(SymbolicValue value, Constraint constraint) {
        BinaryRelation relation = value.binaryRelation();
        if (relation != null) {
          if (BooleanConstraint.TRUE.equals(constraint)) {
            knownRelations.add(relation);
          } else if (BooleanConstraint.FALSE.equals(constraint)) {
            knownRelations.add(relation.inverse());
          }
        }
      }
    });
    return knownRelations;
  }

  public ObjectConstraint getConstraintWithStatus(SymbolicValue value, Object aState) {
    final Object constraint = getConstraint(value.wrappedValue());
    if (constraint instanceof ObjectConstraint) {
      ObjectConstraint oConstraint = (ObjectConstraint) constraint;
      if (oConstraint.hasStatus(aState)) {
        return oConstraint;
      }
    }
    return null;
  }
}
