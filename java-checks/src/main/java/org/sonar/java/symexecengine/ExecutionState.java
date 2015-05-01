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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExecutionState {

  private final IssuableSubscriptionVisitor check;
  ExecutionState parent;
  private SetMultimap<Symbol, Value> reachableValues = HashMultimap.create();
  private SetMultimap<Symbol, Value> unreachableValues = HashMultimap.create();
  /**
   * List of symbol that were declared within this execution state.
   */
  private List<Symbol> definedInState = Lists.newArrayList();
  private Map<Value, State> stateOfValue = Maps.newHashMap();

  public ExecutionState(ExecutionState executionState) {
    this.parent = executionState;
    this.check = executionState.check;
    this.reachableValues = HashMultimap.create(executionState.reachableValues);
    this.unreachableValues = HashMultimap.create(executionState.unreachableValues);
  }

  public ExecutionState(IssuableSubscriptionVisitor check) {
    this.check = check;
  }

  public void defineSymbol(Symbol symbol) {
    definedInState.add(symbol);
  }

  public ExecutionState merge(ExecutionState executionState) {
    for (Symbol symbol : executionState.reachableValues.keys()) {
      this.reachableValues.putAll(symbol, executionState.reachableValues.get(symbol));
    }
    for (Symbol symbol : executionState.unreachableValues.keys()) {
      this.unreachableValues.putAll(symbol, executionState.unreachableValues.get(symbol));
    }

    for (Symbol symbol : unreachableValues.keys()) {
      //cleanup after merge of reachable/unreachable values
      for (Value value : unreachableValues.get(symbol)) {
        reachableValues.remove(symbol, value);
      }
    }
    //Merge states of values
    for (Map.Entry<Value, State> valueStateEntry : executionState.stateOfValue.entrySet()) {
      Value value = valueStateEntry.getKey();
      State state = valueStateEntry.getValue();
      State valueState = getStateOfValue(value);
      if(valueState == null) {
        valueState = state;
      } else {
        valueState = valueState.merge(state);
      }
      this.stateOfValue.put(value, valueState);
    }
    return this;
  }

  public ExecutionState overrideBy(ExecutionState executionState) {
    this.unreachableValues.putAll(executionState.unreachableValues);
    this.reachableValues = executionState.reachableValues;
    this.stateOfValue.putAll(executionState.stateOfValue);
    return this;
  }

  public ExecutionState restoreParent() {
    if (parent != null) {
      insertIssues();
      return parent.merge(this);
    }
    return this;
  }

  public void insertIssues() {
    for (Tree tree : getIssuableTrees()) {
      check.addIssue(tree, "");
    }
  }

  private Set<Tree> getIssuableTrees() {
    Set<Tree> results = Sets.newHashSet();
    for (Symbol symbol : definedInState) {
      for (Value value : unreachableValues.get(symbol)) {
        State state = stateOfValue.get(value);
        if (state.shouldRaiseIssue()) {
          results.addAll(state.reportingTrees());
        }
      }
      for (Value value : reachableValues.get(symbol)) {
        State state = stateOfValue.get(value);
        if (state.shouldRaiseIssue()) {
          results.addAll(state.reportingTrees());
        }
      }
    }
    return results;
  }

  //FIXME : Hideous hack for closeable to get "Ignored" variables
  public List<State> getStatesOf(Symbol symbol) {
    List<State> states = Lists.newArrayList();
    List<Value> values = Lists.newArrayList(reachableValues.get(symbol));
    values.addAll(unreachableValues.get(symbol));
    for (Value value : values) {
      State state = stateOfValue.get(value);
      if(state != null) {
        states.add(state);
      }
    }
    return states;
  }

  @CheckForNull
  private State getStateOfValue(Value value) {
    ExecutionState currentState = this;
    while (currentState != null) {
      State state = currentState.stateOfValue.get(value);
      if(state != null) {
        return state;
      }
      currentState = currentState.parent;
    }
    return null;
  }

  private Iterable<Value> getValues(Symbol symbol) {
    return reachableValues.get(symbol);
  }

  public void createValueForSymbol(Symbol symbol, Tree tree) {
    // When creating a new value, all reachable values are now unreachable.
    Set<Value> values = this.reachableValues.get(symbol);
    unreachableValues.putAll(symbol, values);
    values.clear();
    Value value = new Value(tree);
    reachableValues.put(symbol, value);
    stateOfValue.put(value, State.UNSET);
  }

  public void markValueAs(Symbol symbol, State state) {
    for (Value value : getValues(symbol)) {
      stateOfValue.put(value, state);
    }
  }

  private static class Value {
    final Tree treeNode;

    public Value(Tree treeNode) {
      this.treeNode = treeNode;
    }
  }

}
