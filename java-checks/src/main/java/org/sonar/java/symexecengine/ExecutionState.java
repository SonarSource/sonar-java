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
package org.sonar.java.symexecengine;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExecutionState {

  public final ExecutionState parent;

  /*
   * definitelyReachableValues and potentiallyReachableValues contains the set of possible values from a given symbol.
   * - definitelyReachableValues contains the values that are definitely retrieved by a read operation.
   * - potentiallyReachableValues contains all the values that can be retrieved by a read operation.
   * this is illustrated in the examples below:
   * example 1                      example 2                        example 3
   * a = V1;                        a = V1;                          a = V1;
   * if(condition) {                if(condition) {                  a = V2;
   *   a = V2;                        a = V2;
   * } else {                       }
   *   a = V3;
   * }
   * a.hashCode();                  a.hashCode();                    a.hashCode();
   * a.unlock();                    a.unlock();                      a.unlock();
   * definitely reachable: V2, V3   definitely reachable: V2         definitely reachable: V2
   * potentially reachable: V2, V3  potentially reachable: V1, V2    potentially reachable: V2
   */

  // all values that are definitely modifiable (i.e. through an update).
  private SetMultimap<Symbol, SymbolicValue> definitelyReachableValues = HashMultimap.create();
  // all values that are reachable (i.e. through a read).
  private final SetMultimap<Symbol, SymbolicValue> potentiallyReachableValues = HashMultimap.create();
  private SetMultimap<Symbol, SymbolicValue> unreachableValues = HashMultimap.create();
  /**
   * List of symbol that were declared within this execution state.
   */
  // FIXME(merciesa): this rather relates to scope and should eventually be moved to the visitor.
  private List<Symbol> definedInState = Lists.newArrayList();
  private Map<SymbolicValue, State> stateOfValue = Maps.newHashMap();

  public ExecutionState(ExecutionState executionState) {
    this.parent = executionState;
    this.definitelyReachableValues = HashMultimap.create(executionState.definitelyReachableValues);
    this.unreachableValues = HashMultimap.create(executionState.unreachableValues);
  }

  /**
   * ParentState constructor.
   */
  public ExecutionState() {
    this.parent = null;
  }

  public void defineSymbol(Symbol symbol) {
    definedInState.add(symbol);
  }

  public ExecutionState merge(ExecutionState executionState) {
    for (Symbol symbol : executionState.potentiallyReachableValues.keys()) {
      // FIXME(merciesa) this copies the accessible values from the parent state and is memory inefficient.
      potentiallyReachableValues.putAll(symbol, getPotentiallyReachableValues(symbol));
    }
    potentiallyReachableValues.putAll(executionState.potentiallyReachableValues);

    for (Symbol symbol : executionState.definitelyReachableValues.keys()) {
      if (!executionState.definedInState.contains(symbol)) {
        this.definitelyReachableValues.putAll(symbol, executionState.definitelyReachableValues.get(symbol));
      }
    }
    for (Symbol symbol : executionState.unreachableValues.keys()) {
      if (!executionState.definedInState.contains(symbol)) {
        this.unreachableValues.putAll(symbol, executionState.unreachableValues.get(symbol));
      }
    }

    for (Symbol symbol : unreachableValues.keys()) {
      // cleanup after merge of reachable/unreachable values
      for (SymbolicValue value : unreachableValues.get(symbol)) {
        definitelyReachableValues.remove(symbol, value);
      }
    }
    // Merge states of values
    for (Map.Entry<SymbolicValue, State> valueStateEntry : executionState.stateOfValue.entrySet()) {
      SymbolicValue value = valueStateEntry.getKey();
      State state = valueStateEntry.getValue();
      State valueState = getStateOfValue(value);
      if (valueState == null) {
        valueState = state;
      } else {
        valueState = valueState.merge(state);
      }
      this.stateOfValue.put(value, valueState);
    }
    return this;
  }

  public ExecutionState overrideBy(ExecutionState executionState) {
    for (Symbol symbol : executionState.potentiallyReachableValues.keys()) {
      potentiallyReachableValues.get(symbol).clear();
    }
    potentiallyReachableValues.putAll(executionState.potentiallyReachableValues);

    this.unreachableValues.putAll(executionState.unreachableValues);
    this.definitelyReachableValues = executionState.definitelyReachableValues;
    this.stateOfValue.putAll(executionState.stateOfValue);
    return this;
  }

  public ExecutionState restoreParent() {
    return parent.merge(this);
  }

  Set<State> getStatesOfCurrentExecutionState() {
    Set<State> results = Sets.newHashSet();
    for (Symbol symbol : definedInState) {
      for (SymbolicValue value : Iterables.concat(definitelyReachableValues.get(symbol), unreachableValues.get(symbol))) {
        State state = stateOfValue.get(value);
        if (state != null) {
          results.add(state);
        }
      }
    }
    return results;
  }

  // FIXME : Hideous hack for closeable to get "Ignored" variables
  public List<State> getStatesOf(Symbol symbol) {
    List<State> states = Lists.newArrayList();
    for (SymbolicValue value : Iterables.concat(definitelyReachableValues.get(symbol), unreachableValues.get(symbol))) {
      State state = stateOfValue.get(value);
      if (state != null) {
        states.add(state);
      }
    }
    return states;
  }

  @CheckForNull
  private State getStateOfValue(SymbolicValue value) {
    ExecutionState currentState = this;
    while (currentState != null) {
      State state = currentState.stateOfValue.get(value);
      if (state != null) {
        return state;
      }
      currentState = currentState.parent;
    }
    return null;
  }

  Iterable<SymbolicValue> getDefinitelyReachableValues(Symbol symbol) {
    return definitelyReachableValues.get(symbol);
  }

  public SymbolicValue createValueForSymbol(Symbol symbol, Tree tree) {
    // When creating a new value, all reachable values are now unreachable.
    potentiallyReachableValues.get(symbol).clear();
    Set<SymbolicValue> values = this.definitelyReachableValues.get(symbol);
    unreachableValues.putAll(symbol, values);
    values.clear();
    SymbolicValue value = new SymbolicValue(tree);
    potentiallyReachableValues.put(symbol, value);
    definitelyReachableValues.put(symbol, value);
    stateOfValue.put(value, State.UNSET);
    return value;
  }

  public void markDefinitelyReachableValues(Symbol symbol, State state) {
    for (SymbolicValue value : getDefinitelyReachableValues(symbol)) {
      stateOfValue.put(value, state);
    }
  }

  public void markValueAs(SymbolicValue value, State state) {
    stateOfValue.put(value, state);
  }

  public void markPotentiallyReachableValues(Symbol symbol, State state) {
    for (SymbolicValue value : getPotentiallyReachableValues(symbol)) {
      markValueAs(value, state);
    }
  }

  /**
   * returns the merged state of all potentially reachable values.
   *
   * @param symbol symbol whose merged state must be retrieved.
   * @return merged state of all all potentially reachable values.
   */
  public State mergePotentiallyReachableStates(Symbol symbol) {
    State result = null;
    for (SymbolicValue value : getPotentiallyReachableValues(symbol)) {
      State state = getStateOfValue(value);
      if (state != null) {
        result = result != null ? state.merge(result) : state;
      }
    }
    return result != null ? result : State.UNSET;
  }

  private Set<SymbolicValue> getPotentiallyReachableValues(Symbol symbol) {
    ExecutionState executionState = this;
    while (executionState != null) {
      Set<SymbolicValue> result = executionState.potentiallyReachableValues.get(symbol);
      if (!result.isEmpty()) {
        return result;
      }
      executionState = executionState.parent;
    }
    return ImmutableSet.<SymbolicValue>of();
  }

}
