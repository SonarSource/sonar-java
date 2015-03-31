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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ExecutionState {

  // FIXME(merciesa): find a better name...
  @VisibleForTesting
  static final Map<SymbolicRelation, Map<SymbolicRelation, SymbolicValue>> RELATION_RELATION_MAP = ImmutableMap
    .<SymbolicRelation, Map<SymbolicRelation, SymbolicValue>>builder()
    .put(SymbolicRelation.EQUAL_TO, ImmutableMap.<SymbolicRelation, SymbolicValue>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicValue.BOOLEAN_TRUE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.GREATER_THAN, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.LESS_THAN, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.UNKNOWN, SymbolicValue.UNKNOWN_VALUE)
      .build())
    .put(SymbolicRelation.GREATER_EQUAL, ImmutableMap.<SymbolicRelation, SymbolicValue>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicValue.BOOLEAN_TRUE)
      .put(SymbolicRelation.GREATER_THAN, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.LESS_THAN, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.UNKNOWN, SymbolicValue.UNKNOWN_VALUE)
      .build())
    .put(SymbolicRelation.GREATER_THAN, ImmutableMap.<SymbolicRelation, SymbolicValue>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.GREATER_THAN, SymbolicValue.BOOLEAN_TRUE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.LESS_THAN, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.UNKNOWN, SymbolicValue.UNKNOWN_VALUE)
      .build())
    .put(SymbolicRelation.LESS_EQUAL, ImmutableMap.<SymbolicRelation, SymbolicValue>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.GREATER_THAN, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicValue.BOOLEAN_TRUE)
      .put(SymbolicRelation.LESS_THAN, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.UNKNOWN, SymbolicValue.UNKNOWN_VALUE)
      .build())
    .put(SymbolicRelation.LESS_THAN, ImmutableMap.<SymbolicRelation, SymbolicValue>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.GREATER_THAN, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.LESS_THAN, SymbolicValue.BOOLEAN_TRUE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.UNKNOWN, SymbolicValue.UNKNOWN_VALUE)
      .build())
    .put(SymbolicRelation.NOT_EQUAL, ImmutableMap.<SymbolicRelation, SymbolicValue>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicValue.BOOLEAN_FALSE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.GREATER_THAN, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.LESS_THAN, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicValue.BOOLEAN_TRUE)
      .put(SymbolicRelation.UNKNOWN, SymbolicValue.UNKNOWN_VALUE)
      .build())
    .put(SymbolicRelation.UNKNOWN, ImmutableMap.<SymbolicRelation, SymbolicValue>builder()
      .put(SymbolicRelation.EQUAL_TO, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.GREATER_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.GREATER_THAN, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.LESS_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.LESS_THAN, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.NOT_EQUAL, SymbolicValue.UNKNOWN_VALUE)
      .put(SymbolicRelation.UNKNOWN, SymbolicValue.UNKNOWN_VALUE)
      .build())
    .build();

  @Nullable
  final ExecutionState parentState;
  final Table<SymbolicValue, SymbolicValue, SymbolicRelation> relations;
  final Map<Symbol.VariableSymbol, SymbolicValue> variables;

  public ExecutionState() {
    this.parentState = null;
    this.relations = HashBasedTable.create();
    this.variables = new HashMap<>();
  }

  ExecutionState(ExecutionState parentState) {
    this.parentState = parentState;
    this.relations = HashBasedTable.create();
    this.variables = new HashMap<>();
  }

  @VisibleForTesting
  SymbolicRelation getRelation(SymbolicValue leftValue, SymbolicValue rightValue) {
    SymbolicRelation result = relations.get(leftValue, rightValue);
    return result != null ? result : parentState != null ? parentState.getRelation(leftValue, rightValue) : SymbolicRelation.UNKNOWN;
  }

  SymbolicValue evaluateRelation(SymbolicValue leftValue, SymbolicRelation relation, SymbolicValue rightValue) {
    return RELATION_RELATION_MAP.get(getRelation(leftValue, rightValue)).get(relation);
  }

  void setRelation(SymbolicValue leftValue, SymbolicRelation relation, SymbolicValue rightValue) {
    if (!leftValue.equals(SymbolicValue.UNKNOWN_VALUE) && !rightValue.equals(SymbolicValue.UNKNOWN_VALUE)) {
      if (relation == SymbolicRelation.UNKNOWN) {
        throw new IllegalStateException("relation cannot be UNKNOWN");
      }
      relations.put(leftValue, rightValue, relation);
      relations.put(rightValue, leftValue, relation.swap());
    }
  }

  SymbolicValue getSymbolicValue(ExpressionTree tree) {
    Symbol.VariableSymbol symbol = extractLocalVariableSymbol(tree);
    if (symbol == null) {
      return SymbolicValue.UNKNOWN_VALUE;
    }
    for (ExecutionState state = this; state != null; state = state.parentState) {
      SymbolicValue result = state.variables.get(symbol);
      if (result != null) {
        return result;
      }
    }
    SymbolicValue result = new SymbolicValue();
    variables.put(symbol, result);
    return result;
  }

  @CheckForNull
  private Symbol.VariableSymbol extractLocalVariableSymbol(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) tree;
      Symbol symbol = ((IdentifierTree) identifierTree).symbol();
      if (symbol.owner().isMethodSymbol()) {
        return (Symbol.VariableSymbol) symbol;
      }
    }
    return null;
  }

}
