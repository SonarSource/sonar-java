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
import com.google.common.collect.Lists;
import org.sonar.java.collections.AVLTree;
import org.sonar.java.collections.PMap;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ProgramState {

  private int hashCode;
  private final int constraintSize;

  public static final ProgramState EMPTY_STATE = new ProgramState(
    AVLTree.<Symbol, SymbolicValue>create(),
    AVLTree.<SymbolicValue, Object>create()
      .put(SymbolicValue.NULL_LITERAL, ConstraintManager.NullConstraint.NULL)
      .put(SymbolicValue.TRUE_LITERAL, ConstraintManager.BooleanConstraint.TRUE)
      .put(SymbolicValue.FALSE_LITERAL, ConstraintManager.BooleanConstraint.FALSE),
    AVLTree.<ExplodedGraph.ProgramPoint, Integer>create(),
    Lists.<SymbolicValue>newLinkedList());

  private final PMap<ExplodedGraph.ProgramPoint, Integer> visitedPoints;
  private final Deque<SymbolicValue> stack;
  private final PMap<Symbol, SymbolicValue> values;
  private final PMap<SymbolicValue, Object> constraints;

  private ProgramState(PMap<Symbol, SymbolicValue> values, PMap<SymbolicValue, Object> constraints, PMap<ExplodedGraph.ProgramPoint, Integer> visitedPoints,
    Deque<SymbolicValue> stack) {
    this.values = values;
    this.constraints = constraints;
    this.visitedPoints = visitedPoints;
    this.stack = stack;
    constraintSize = 3;
  }

  private ProgramState(ProgramState ps, Deque<SymbolicValue> newStack) {
    values = ps.values;
    constraints = ps.constraints;
    constraintSize = ps.constraintSize;
    visitedPoints = ps.visitedPoints;
    stack = newStack;
  }

  private ProgramState(ProgramState ps, PMap<SymbolicValue, Object> newConstraints) {
    values = ps.values;
    constraints = newConstraints;
    constraintSize = ps.constraintSize +1;
    visitedPoints = ps.visitedPoints;
    this.stack = ps.stack;
  }

  ProgramState stackValue(SymbolicValue sv) {
    Deque<SymbolicValue> newStack = new LinkedList<>(stack);
    newStack.push(sv);
    return new ProgramState(this, newStack);
  }

  ProgramState clearStack() {
    return unstackValue(stack.size()).a;
  }

  Pair<ProgramState, List<SymbolicValue>> unstackValue(int nbElements) {
    if (nbElements == 0) {
      return new Pair<>(this, Collections.<SymbolicValue>emptyList());
    }
    Preconditions.checkArgument(stack.size() >= nbElements, nbElements);
    Deque<SymbolicValue> newStack = new LinkedList<>(stack);
    List<SymbolicValue> result = Lists.newArrayList();
    for (int i = 0; i < nbElements; i++) {
      result.add(newStack.pop());
    }
    return new Pair<>(new ProgramState(this, newStack), result);
  }

  public SymbolicValue peekValue() {
    return stack.peek();
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

  public ProgramState addConstraint(SymbolicValue symbolicValue, Object constraint) {
    PMap<SymbolicValue, Object> newConstraints = constraints.put(symbolicValue, constraint);
    if (newConstraints != constraints) {
      return new ProgramState(this, newConstraints);
    }
    return this;
  }

  ProgramState put(Symbol symbol, SymbolicValue value) {
    if (symbol.isUnknown()) {
      return this;
    }
    PMap<Symbol, SymbolicValue> newValues = values.put(symbol, value);
    if (newValues != values) {
      return new ProgramState(newValues, constraints, visitedPoints, stack);
    }
    return this;
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
    for (VariableTree variableTree : variableTrees) {
      newValues = newValues.put(variableTree.symbol(), constraintManager.createSymbolicValue(variableTree));
    }
    return new ProgramState(newValues, constraints, visitedPoints, stack);
  }

  private static boolean isField(Symbol symbol) {
    return symbol.isVariableSymbol() && !symbol.owner().isMethodSymbol();
  }

  public boolean canReach(final SymbolicValue symbolicValue) {
    final boolean[] reachable = new boolean[1];
    values.forEach(new PMap.Consumer<Symbol, SymbolicValue>() {
      @Override
      public void accept(Symbol symbol, SymbolicValue value) {
        if (value == symbolicValue) {
          reachable[0] = true;
        }
      }
    });
    return reachable[0];
  }

  public ProgramState visitingPoint(ExplodedGraph.ProgramPoint programPoint) {
    return new ProgramState(values, constraints, visitedPoints.put(programPoint, numberOfTimeVisited(programPoint) + 1), stack);
  }

  @CheckForNull
  public Object getConstraint(SymbolicValue sv) {
    return constraints.get(sv);
  }

  public int constraintsSize() {
    return constraintSize;
  }

  @CheckForNull
  public SymbolicValue getValue(Symbol symbol) {
    return values.get(symbol);
  }
}
