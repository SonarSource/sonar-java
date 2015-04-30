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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import java.util.Map;
import java.util.Set;

public class ExecutionState {

  private final IssuableSubscriptionVisitor check;
  ExecutionState parent;
  private Map<Symbol, Value> valuesBySymbol = Maps.newHashMap();

  public ExecutionState(ExecutionState executionState) {
    this.parent = executionState;
    this.check = executionState.check;
  }

  public ExecutionState(IssuableSubscriptionVisitor check) {
    this.check = check;
  }

  public ExecutionState merge(ExecutionState executionState) {
    for (Map.Entry<Symbol, Value> entry : executionState.valuesBySymbol.entrySet()) {
      Symbol symbol = entry.getKey();
      Value currentValue = getValue(symbol);
      Value valueToMerge = entry.getValue();
      if (currentValue != null) {
        currentValue.state = currentValue.state.merge(valueToMerge.state);
        valuesBySymbol.put(symbol, currentValue);
      } else {
        if (valueToMerge.shouldRaiseIssue()) {
          insertIssue(valueToMerge.treeNode);
        }
      }
    }
    return this;
  }

  public ExecutionState overrideBy(ExecutionState executionState) {
    for (Map.Entry<Symbol, Value> entry : executionState.valuesBySymbol.entrySet()) {
      Symbol symbol = entry.getKey();
      Value value = entry.getValue();
      if (getValue(symbol) != null) {
        markValueAs(symbol, value.state);
      } else {
        valuesBySymbol.put(symbol, value);
      }
    }
    return this;
  }

  public void newValueForSymbol(Symbol symbol, Tree tree, State state) {
    Value knownValue = getValue(symbol);
    if (knownValue != null) {
      if (knownValue.shouldRaiseIssue()) {
        insertIssue(knownValue.treeNode);
      }
    } else {
      // no known occurence, means its a field or a method param.
      createValueInTopExecutionState(symbol, tree, new State() {
        @Override
        public State merge(State s) {
          return s;
        }

        @Override
        public boolean shouldRaiseIssue() {
          return false;
        }
      });
    }
    valuesBySymbol.put(symbol, new Value(tree, state));
  }

  private void createValueInTopExecutionState(Symbol symbol, Tree tree, State state) {
    ExecutionState top = this;
    while (top.parent != null) {
      top = top.parent;
    }
    top.valuesBySymbol.put(symbol, new Value(tree, state));
  }

  private void insertIssue(Tree treeNode) {
    check.addIssue(treeNode, "");
  }

  public void markValueAs(Symbol symbol, State state) {
    Value value = getValue(symbol);
    if (value != null) {
      valuesBySymbol.put(symbol, new Value(value.treeNode, state));
    }
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
      insertIssue(tree);
    }
  }

  private Set<Tree> getIssuableTrees() {
    Set<Tree> results = Sets.newHashSet();
    for (Value value: valuesBySymbol.values()) {
      if (value.shouldRaiseIssue()) {
        results.add(value.treeNode);
      }
    }
    return results;
  }

  @CheckForNull
  private Value getValue(Symbol symbol) {
    Value value = valuesBySymbol.get(symbol);
    if (value != null) {
      return new Value(value.treeNode, value.state);
    } else if (parent != null) {
      return parent.getValue(symbol);
    }
    return null;
  }

  @CheckForNull
  public State getStateOf(Symbol symbol) {
    Value value = getValue(symbol);
    if(value != null) {
      return value.state;
    }
    return null;
  }

  private static class Value {

    State state;
    Tree treeNode;

    public Value(Tree treeNode, State state) {
      this.treeNode = treeNode;
      this.state = state;
    }

    public boolean shouldRaiseIssue() {
      return state.shouldRaiseIssue();
    }
  }

}
