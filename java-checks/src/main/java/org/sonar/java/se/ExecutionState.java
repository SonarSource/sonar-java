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
package org.sonar.java.se;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

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
  final Map<SymbolicValue, Map<SymbolicValue, SymbolicRelation>> relations;
  final Map<Symbol.VariableSymbol, SymbolicValue> variables;

  public ExecutionState() {
    this.parentState = null;
    this.relations = new HashMap<>();
    this.variables = new HashMap<>();
  }

  ExecutionState(ExecutionState parentState) {
    this.parentState = parentState;
    this.relations = new HashMap<>();
    this.variables = new HashMap<>();
  }

  @VisibleForTesting
  SymbolicRelation getRelation(SymbolicValue leftValue, SymbolicValue rightValue) {
    Map<SymbolicValue, SymbolicRelation> map = relations.get(leftValue);
    if (map != null) {
      SymbolicRelation result = map.get(rightValue);
      if (result != null) {
        return result;
      }
    }
    return parentState != null ? parentState.getRelation(leftValue, rightValue) : SymbolicRelation.UNKNOWN;
  }

  SymbolicValue evaluateRelation(SymbolicValue leftValue, SymbolicRelation relation, SymbolicValue rightValue) {
    return RELATION_RELATION_MAP.get(getRelation(leftValue, rightValue)).get(relation);
  }

  void setRelation(SymbolicValue leftValue, SymbolicRelation relation, SymbolicValue rightValue) {
    if (!leftValue.equals(SymbolicValue.UNKNOWN_VALUE) && !rightValue.equals(SymbolicValue.UNKNOWN_VALUE)) {
      if (relation == SymbolicRelation.UNKNOWN) {
        throw new IllegalStateException("relation cannot be UNKNOWN");
      }
      Map<SymbolicValue, SymbolicRelation> leftMap = relations.get(leftValue);
      if (leftMap == null) {
        leftMap = new HashMap<SymbolicValue, SymbolicRelation>();
        relations.put(leftValue, leftMap);
      }
      leftMap.put(rightValue, relation);
      Map<SymbolicValue, SymbolicRelation> rightMap = relations.get(rightValue);
      if (rightMap == null) {
        rightMap = new HashMap<SymbolicValue, SymbolicRelation>();
        relations.put(rightValue, rightMap);
      }
      rightMap.put(leftValue, relation.swap());
    }
  }

  SymbolicValue getSymbolicValue(ExpressionTree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) tree;
      Symbol symbol = ((IdentifierTree) identifierTree).symbol();
      if (symbol.owner().isMethodSymbol()) {
        Symbol.VariableSymbol variableSymbol = (Symbol.VariableSymbol) symbol;
        for (ExecutionState state = this; state != null; state = state.parentState) {
          SymbolicValue result = state.variables.get(variableSymbol);
          if (result != null) {
            return result;
          }
        }
        SymbolicValue result = new SymbolicValue();
        variables.put(variableSymbol, result);
        return result;
      }
    }
    return SymbolicValue.UNKNOWN_VALUE;
  }

}
