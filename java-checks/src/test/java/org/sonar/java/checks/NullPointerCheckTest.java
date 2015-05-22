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
package org.sonar.java.checks;

import org.junit.Test;
import org.sonar.java.checks.NullPointerCheck.AssignmentVisitor;
import org.sonar.java.checks.NullPointerCheck.ConditionalState;
import org.sonar.java.checks.NullPointerCheck.NullableState;
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.java.resolve.JavaSymbol.VariableJavaSymbol;
import org.sonar.java.symexecengine.ExecutionState;
import org.sonar.java.symexecengine.State;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Tree;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.checks.NullPointerCheck.NullableState.NOTNULL;
import static org.sonar.java.checks.NullPointerCheck.NullableState.NULL;
import static org.sonar.java.checks.NullPointerCheck.NullableState.UNKNOWN;
import static org.sonar.java.symexecengine.State.UNSET;

public class NullPointerCheckTest {

  @Test
  public void test() {
    JavaCheckVerifier.verify("src/test/files/checks/NullPointerCheck.java", new NullPointerCheck());
  }

  @Test
  public void test_state_hierarchy() {
    ExecutionState parentState = new ExecutionState();
    ExecutionState currentState = new ExecutionState(parentState);

    assertThat(currentState.parent).isSameAs(parentState);
  }

  @Test
  public void test_state_get_variable_value() {
    VariableJavaSymbol variable = mock(VariableJavaSymbol.class);

    ExecutionState parentState = new ExecutionState();
    parentState.createValueForSymbol(variable, mock(Tree.class));
    ExecutionState currentState = new ExecutionState(parentState);

    // undefined variable must be unset
    assertThat(parentState.mergePotentiallyReachableStates(variable)).isSameAs(UNSET);

    // variable defined in parent must be visible in current.
    parentState.markPotentiallyReachableValues(variable, NOTNULL);
    assertThat(parentState.mergePotentiallyReachableStates(variable)).isSameAs(NOTNULL);
    assertThat(currentState.mergePotentiallyReachableStates(variable)).isSameAs(NOTNULL);

    // variable redefined in current must not affect value in parent.
    currentState.markPotentiallyReachableValues(variable, NULL);
    assertThat(parentState.mergePotentiallyReachableStates(variable)).isSameAs(NOTNULL);
    assertThat(currentState.mergePotentiallyReachableStates(variable)).isSameAs(NULL);
  }

  private State testMerge(NullableState parentValue, NullableState trueValue, NullableState falseValue) {
    VariableJavaSymbol variable = mock(VariableJavaSymbol.class);
    ExecutionState parentState = new ExecutionState();
    parentState.createValueForSymbol(variable, mock(Tree.class));
    if (parentValue != null) {
      parentState.markPotentiallyReachableValues(variable, parentValue);
    }
    ExecutionState trueState = new ExecutionState(parentState);
    if (trueValue != null) {
      trueState.markPotentiallyReachableValues(variable, trueValue);
    }
    if (falseValue == null) {
      return parentState.merge(trueState).mergePotentiallyReachableStates(variable);
    }
    ExecutionState falseState = new ExecutionState(parentState);
    falseState.markPotentiallyReachableValues(variable, falseValue);
    return parentState.overrideBy(trueState.merge(falseState)).mergePotentiallyReachableStates(variable);
  }

  @Test
  public void test_state_merge_values() {
    ExecutionState state = new ExecutionState();
    assertThat(state.overrideBy(new ExecutionState())).isSameAs(state);
    // variable defined in parentState only should not change
    assertThat(testMerge(NOTNULL, null, null)).isSameAs(NOTNULL);
    // variable defined in parentState and trueState, must match
    assertThat(testMerge(NOTNULL, NOTNULL, null)).isSameAs(NOTNULL);
    assertThat(testMerge(NULL, NULL, null)).isSameAs(NULL);
    assertThat(testMerge(NOTNULL, NULL, null)).isSameAs(UNKNOWN);
    // variable defined in parentState and falseState, must match
    assertThat(testMerge(NOTNULL, null, NOTNULL)).isSameAs(NOTNULL);
    assertThat(testMerge(NULL, null, NULL)).isSameAs(NULL);
    assertThat(testMerge(NOTNULL, null, NULL)).isSameAs(UNKNOWN);
    // variable defined in trueState and falseState, must match
    assertThat(testMerge(null, NOTNULL, NOTNULL)).isSameAs(NOTNULL);
    assertThat(testMerge(null, NULL, NULL)).isSameAs(NULL);
    assertThat(testMerge(null, NOTNULL, NULL)).isSameAs(UNKNOWN);
    // variable defined in parentState, trueState and falseState, trueState and falseState must match
    assertThat(testMerge(NOTNULL, NOTNULL, NOTNULL)).isSameAs(NOTNULL);
    assertThat(testMerge(NOTNULL, NULL, NULL)).isSameAs(NULL);
    assertThat(testMerge(NOTNULL, NOTNULL, NULL)).isSameAs(UNKNOWN);
    assertThat(testMerge(NULL, NOTNULL, NOTNULL)).isSameAs(NOTNULL);
    assertThat(testMerge(NULL, NULL, NULL)).isSameAs(NULL);
    assertThat(testMerge(NULL, NOTNULL, NULL)).isSameAs(UNKNOWN);
  }

  @Test
  public void test_state_copy_values_from() {
    VariableJavaSymbol parentVariable = mock(VariableJavaSymbol.class);
    VariableJavaSymbol childVariable = mock(VariableJavaSymbol.class);
    VariableJavaSymbol bothVariable = mock(VariableJavaSymbol.class);

    ExecutionState parentState = new ExecutionState();
    parentState.createValueForSymbol(parentVariable, mock(Tree.class));
    parentState.createValueForSymbol(bothVariable, mock(Tree.class));
    parentState.markPotentiallyReachableValues(parentVariable, NULL);
    parentState.markPotentiallyReachableValues(bothVariable, NULL);
    
    ExecutionState childState = new ExecutionState(parentState);
    childState.createValueForSymbol(childVariable, mock(Tree.class));
    childState.markPotentiallyReachableValues(childVariable, NOTNULL);
    childState.markPotentiallyReachableValues(bothVariable, NOTNULL);

    parentState.overrideBy(childState);

    assertThat(parentState.mergePotentiallyReachableStates(parentVariable)).isSameAs(NULL);
    assertThat(parentState.mergePotentiallyReachableStates(childVariable)).isSameAs(NOTNULL);
    assertThat(parentState.mergePotentiallyReachableStates(bothVariable)).isSameAs(NOTNULL);
  }

  @Test
  public void test_state_merge_conditional_and() {
    VariableJavaSymbol variable1 = mock(VariableJavaSymbol.class);
    VariableJavaSymbol variable2 = mock(VariableJavaSymbol.class);
    VariableJavaSymbol variable3 = mock(VariableJavaSymbol.class);

    ExecutionState currentState = new ExecutionState();
    currentState.createValueForSymbol(variable1, mock(Tree.class));
    currentState.createValueForSymbol(variable2, mock(Tree.class));
    currentState.createValueForSymbol(variable3, mock(Tree.class));
    currentState.markPotentiallyReachableValues(variable1, NOTNULL);
    currentState.markPotentiallyReachableValues(variable2, NULL);
    currentState.markPotentiallyReachableValues(variable3, NULL);

    ExecutionState conditionState = new ExecutionState(currentState);
    ConditionalState conditionalState = new ConditionalState(conditionState);

    // simulates variable1 != null && variable2 != null
    ConditionalState leftConditionalState = new ConditionalState(conditionState);
    leftConditionalState.trueState.markPotentiallyReachableValues(variable1, NOTNULL);
    leftConditionalState.falseState.markPotentiallyReachableValues(variable1, NULL);
    ConditionalState rightConditionalState = new ConditionalState(leftConditionalState.trueState);
    rightConditionalState.trueState.markPotentiallyReachableValues(variable2, NOTNULL);
    rightConditionalState.falseState.markPotentiallyReachableValues(variable2, NULL);
    conditionalState.mergeConditionalAnd(leftConditionalState, rightConditionalState);

    // in the resulting trueState both conditions are true
    assertThat(conditionalState.trueState.mergePotentiallyReachableStates(variable1)).isSameAs(NOTNULL);
    assertThat(conditionalState.trueState.mergePotentiallyReachableStates(variable2)).isSameAs(NOTNULL);
    // in the resulting falesState variable1 is unknown, since its condition was both true and false,
    // and variable2 is unknown, since its condition was either false or not tested.
    assertThat(conditionalState.falseState.mergePotentiallyReachableStates(variable1)).isSameAs(UNKNOWN);
    assertThat(conditionalState.falseState.mergePotentiallyReachableStates(variable2)).isSameAs(NULL);
    // variables not checked in conditions must remain unchanged.
    assertThat(conditionalState.trueState.mergePotentiallyReachableStates(variable3)).isSameAs(NULL);
    assertThat(conditionalState.falseState.mergePotentiallyReachableStates(variable3)).isSameAs(NULL);
  }

  @Test
  public void test_state_merge_conditional_or() {
    VariableJavaSymbol variable1 = mock(VariableJavaSymbol.class);
    VariableJavaSymbol variable2 = mock(VariableJavaSymbol.class);
    VariableJavaSymbol variable3 = mock(VariableJavaSymbol.class);

    ExecutionState currentState = new ExecutionState();
    currentState.createValueForSymbol(variable1, mock(Tree.class));
    currentState.createValueForSymbol(variable2, mock(Tree.class));
    currentState.createValueForSymbol(variable3, mock(Tree.class));
    currentState.markPotentiallyReachableValues(variable1, NOTNULL);
    currentState.markPotentiallyReachableValues(variable2, NULL);
    currentState.markPotentiallyReachableValues(variable3, NOTNULL);

    ExecutionState conditionState = new ExecutionState(currentState);
    ConditionalState conditionalState = new ConditionalState(conditionState);

    // simulates variable1 != null || variable2 != null
    ConditionalState leftConditionalState = new ConditionalState(conditionState);
    leftConditionalState.trueState.markPotentiallyReachableValues(variable1, NOTNULL);
    leftConditionalState.falseState.markPotentiallyReachableValues(variable1, NULL);
    ConditionalState rightConditionalState = new ConditionalState(leftConditionalState.trueState);
    rightConditionalState.trueState.markPotentiallyReachableValues(variable2, NOTNULL);
    rightConditionalState.falseState.markPotentiallyReachableValues(variable2, NULL);
    conditionalState.mergeConditionalOr(leftConditionalState, rightConditionalState);

    // in the resulting trueState variable1 is unknown, since its condition was both true and false,
    // and variable2 is unknown, since its conditiona was either true or not tested.
    assertThat(conditionalState.trueState.mergePotentiallyReachableStates(variable1)).isSameAs(NOTNULL);
    assertThat(conditionalState.trueState.mergePotentiallyReachableStates(variable2)).isSameAs(UNKNOWN);
    // in the resulting falseState both conditions are false
    assertThat(conditionalState.falseState.mergePotentiallyReachableStates(variable1)).isSameAs(NULL);
    assertThat(conditionalState.falseState.mergePotentiallyReachableStates(variable2)).isSameAs(NULL);
    // variables not checked in conditions must remain unchanged.
    assertThat(conditionalState.trueState.mergePotentiallyReachableStates(variable3)).isSameAs(NOTNULL);
    assertThat(conditionalState.falseState.mergePotentiallyReachableStates(variable3)).isSameAs(NOTNULL);
  }

  @Test
  public void test_assignment_visitor() {
    AssignmentVisitor visitor = new NullPointerCheck.AssignmentVisitor();

    Symbol methodSymbol = mock(Symbol.class);
    when(methodSymbol.isVariableSymbol()).thenReturn(false);
    visitor.registerAssignedSymbol(methodSymbol);
    assertThat(visitor.assignedSymbols).isEmpty();

    VariableJavaSymbol variableSymbol = mock(VariableJavaSymbol.class);
    when(variableSymbol.isVariableSymbol()).thenReturn(true);
    visitor.registerAssignedSymbol(variableSymbol);
    assertThat(visitor.assignedSymbols.size()).isEqualTo(1);
  }

}
