/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.symexec;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.sonar.plugins.java.api.semantic.Symbol;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.sonar.java.symexec.SymbolicRelation.UNKNOWN;

public class ExecutionState {

  @Nullable
  @VisibleForTesting
  final ExecutionState parentState;
  @VisibleForTesting
  final Table<SymbolicValue, SymbolicValue, SymbolicRelation> relations;

  private StackEntry stack;

  public ExecutionState() {
    parentState = null;
    relations = HashBasedTable.create();
  }

  ExecutionState(ExecutionState parentState) {
    this.parentState = parentState;
    this.relations = HashBasedTable.create();
    this.stack = parentState.stack;
  }

  SymbolicValue peek() {
    return stack.value;
  }

  SymbolicValue pop() {
    SymbolicValue result = stack.value;
    stack = stack.parent;
    return result;
  }

  SymbolicValue popLast() {
    SymbolicValue result = stack.value;
    stack = stack.parent;
    if (stack != null) {
      throw new IllegalStateException("stack is not empty");
    }
    return result;
  }

  ExecutionState push(SymbolicValue value) {
    stack = new StackEntry(stack, value);
    return this;
  }

  @VisibleForTesting
  SymbolicRelation getRelation(SymbolicValue leftValue, SymbolicValue rightValue) {
    for (ExecutionState executionState = this; executionState != null; executionState = executionState.parentState) {
      SymbolicRelation result = executionState.relations.get(leftValue, rightValue);
      if (result != null) {
        return result;
      }
    }
    return UNKNOWN;
  }

  @CheckForNull
  SymbolicValue evaluateRelation(SymbolicValue leftValue, SymbolicRelation relation, SymbolicValue rightValue) {
    switch (getRelation(leftValue, rightValue).combine(relation)) {
      case FALSE:
        return SymbolicValue.BOOLEAN_FALSE;
      case TRUE:
        return SymbolicValue.BOOLEAN_TRUE;
      default:
        return null;
    }
  }

  ExecutionState setRelation(SymbolicValue leftValue, SymbolicRelation relation, SymbolicValue rightValue) {
    if (!leftValue.equals(rightValue)) {
      if (leftValue.equals(SymbolicValue.BOOLEAN_FALSE)) {
        relations.put(SymbolicValue.BOOLEAN_TRUE, rightValue, relation.negate());
        relations.put(rightValue, SymbolicValue.BOOLEAN_TRUE, relation.negate().swap());
      } else if (rightValue.equals(SymbolicValue.BOOLEAN_FALSE)) {
        relations.put(leftValue, SymbolicValue.BOOLEAN_TRUE, relation.negate());
        relations.put(SymbolicValue.BOOLEAN_TRUE, leftValue, relation.negate().swap());
      } else {
        relations.put(leftValue, rightValue, relation);
        relations.put(rightValue, leftValue, relation.swap());
      }
    }
    return this;
  }

  public SymbolicBooleanConstraint getBooleanConstraint(SymbolicValue variable) {
    if (variable.equals(SymbolicValue.BOOLEAN_TRUE)) {
      return SymbolicBooleanConstraint.TRUE;
    } else if (variable.equals(SymbolicValue.BOOLEAN_FALSE)) {
      return SymbolicBooleanConstraint.FALSE;
    }
    switch (getRelation(variable, SymbolicValue.BOOLEAN_TRUE)) {
      case EQUAL_TO:
        return SymbolicBooleanConstraint.TRUE;
      case NOT_EQUAL:
        return SymbolicBooleanConstraint.FALSE;
      default:
        return SymbolicBooleanConstraint.UNKNOWN;
    }
  }

  ExecutionState setBooleanConstraint(SymbolicValue variable, SymbolicBooleanConstraint constraint) {
    switch (constraint) {
      case FALSE:
        setRelation(variable, SymbolicRelation.NOT_EQUAL, SymbolicValue.BOOLEAN_TRUE);
        break;
      case TRUE:
        setRelation(variable, SymbolicRelation.EQUAL_TO, SymbolicValue.BOOLEAN_TRUE);
        break;
      default:
        setRelation(variable, SymbolicRelation.UNKNOWN, SymbolicValue.BOOLEAN_TRUE);
        break;
    }
    return this;
  }

  void invalidateFields(SymbolicEvaluator evaluator) {
    Set<Symbol> assignedVariables = new HashSet<>();
    for (ExecutionState state = this; state != null; state = state.parentState) {
      assignedVariables.addAll(state.values.keySet());
    }
    for (Symbol variable : assignedVariables) {
      if (isField(variable)) {
        assignValue(variable, evaluator.createSymbolicInstanceValue());
      }
    }
  }

  private boolean isField(Symbol symbol) {
    return symbol.owner().isTypeSymbol();
  }

  private final Map<Symbol, SymbolicValue> values = new HashMap<>();

  public void assignValue(Symbol variable, SymbolicValue value) {
    values.put(variable, value);
  }

  @CheckForNull
  public SymbolicValue getValue(Symbol variable) {
    ExecutionState executionState = this;
    while (true) {
      SymbolicValue result = executionState.values.get(variable);
      if (result != null) {
        return result;
      } else if (executionState.parentState == null) {
        return null;
      }
      executionState = executionState.parentState;
    }
  }

  static final class StackEntry {
    @Nullable
    final StackEntry parent;
    final SymbolicValue value;

    StackEntry(StackEntry parent, SymbolicValue value) {
      this.parent = parent;
      this.value = value;
    }
  }

}
