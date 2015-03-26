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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.checks.NullPointerCheck.AbstractValue;
import org.sonar.java.checks.NullPointerCheck.AssignmentVisitor;
import org.sonar.java.checks.NullPointerCheck.ConditionalState;
import org.sonar.java.checks.NullPointerCheck.State;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.resolve.JavaSymbol.VariableJavaSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.checks.NullPointerCheck.AbstractValue.NOTNULL;
import static org.sonar.java.checks.NullPointerCheck.AbstractValue.NULL;
import static org.sonar.java.checks.NullPointerCheck.AbstractValue.UNKNOWN;
import static org.sonar.java.checks.NullPointerCheck.AbstractValue.UNSET;

public class NullPointerCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void check() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/NullPointerCheck.java"),
      new VisitorsBridge(new NullPointerCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(14).withMessage("null is dereferenced")
      .next().atLine(15).withMessage("null is dereferenced")
      .next().atLine(16).withMessage("null is dereferenced")
      .next().atLine(76).withMessage("NullPointerException might be thrown as 'array2' is nullable here")
      .next().atLine(78).withMessage("NullPointerException might be thrown as 'checkForNullMethod' is nullable here")
      .next().atLine(107).withMessage("NullPointerException might be thrown as 'a2' is nullable here")
      .next().atLine(144).withMessage("'checkForNullMethod' is nullable here and method 'method2' does not accept nullable argument")
      .next().atLine(145).withMessage("'checkForNullMethod' is nullable here and method 'method2' does not accept nullable argument")
      .next().atLine(146).withMessage("'checkForNullMethod' is nullable here and method 'method2' does not accept nullable argument")
      .next().atLine(151).withMessage("method 'method2' does not accept nullable argument")
      .next().atLine(152).withMessage("method 'method2' does not accept nullable argument")
      .next().atLine(153).withMessage("method 'method2' does not accept nullable argument")
      .next().atLine(159).withMessage("NullPointerException might be thrown as 'argument1' is nullable here")
      .next().atLine(165).withMessage("NullPointerException might be thrown as 'argument1' is nullable here")
      .next().atLine(172).withMessage("NullPointerException might be thrown as 'argument2' is nullable here")
      .next().atLine(174).withMessage("NullPointerException might be thrown as 'argument2' is nullable here")
      .next().atLine(209).withMessage("NullPointerException might be thrown as 'argument4' is nullable here")
      .next().atLine(215).withMessage("NullPointerException might be thrown as 'argument2' is nullable here")
      .next().atLine(217).withMessage("NullPointerException might be thrown as 'argument3' is nullable here")
      .next().atLine(225).withMessage("NullPointerException might be thrown as 'var1' is nullable here")
      .next().atLine(235).withMessage("NullPointerException might be thrown as 'object' is nullable here")
      .next().atLine(237).withMessage("NullPointerException might be thrown as 'object' is nullable here")
      .next().atLine(245).withMessage("NullPointerException might be thrown as 'object' is nullable here")
      .next().atLine(246).withMessage("NullPointerException might be thrown as 'object' is nullable here")
      .next().atLine(248).withMessage("NullPointerException might be thrown as 'str' is nullable here")
      .next().atLine(255).withMessage("NullPointerException might be thrown as 'object' is nullable here")
      .next().atLine(256).withMessage("NullPointerException might be thrown as 'object' is nullable here")
      .next().atLine(258).withMessage("NullPointerException might be thrown as 'str' is nullable here")
      .next().atLine(270).withMessage("NullPointerException might be thrown as 'object1' is nullable here")
      .next().atLine(278).withMessage("NullPointerException might be thrown as 'object' is nullable here")
      .next().atLine(278).withMessage("NullPointerException might be thrown as 'object' is nullable here")
      .next().atLine(283).withMessage("NullPointerException might be thrown as 'object2' is nullable here")
      .next().atLine(284).withMessage("NullPointerException might be thrown as 'object1' is nullable here")
      .next().atLine(292).withMessage("NullPointerException might be thrown as 'set' is nullable here")
      .next().atLine(293).withMessage("NullPointerException might be thrown as 'head' is nullable here")
      .next().atLine(295).withMessage("NullPointerException might be thrown as 'value' is nullable here")
      .next().atLine(297).withMessage("NullPointerException might be thrown as 'head' is nullable here")
      .next().atLine(303).withMessage("NullPointerException might be thrown as 'object1' is nullable here")
      .next().atLine(306).withMessage("NullPointerException might be thrown as 'object2' is nullable here")
      .next().atLine(310).withMessage("NullPointerException might be thrown as 'object3' is nullable here")
      .next().atLine(333).withMessage("NullPointerException might be thrown as 'str1' is nullable here")
      .next().atLine(335).withMessage("NullPointerException might be thrown as 'str2' is nullable here")
      .next().atLine(337).withMessage("NullPointerException might be thrown as 'str3' is nullable here")
      .next().atLine(354).withMessage("NullPointerException might be thrown as 'object' is nullable here")
      .next().atLine(361).withMessage("NullPointerException might be thrown as 'object12' is nullable here")
      .next().atLine(376).withMessage("NullPointerException might be thrown as 'object22' is nullable here")
      .next().atLine(414).withMessage("NullPointerException might be thrown as 'object3' is nullable here");
  }

  @Test
  public void test_state_hierarchy() {
    State parentState = new State();
    State currentState = new State(parentState);

    assertThat(currentState.parentState).isSameAs(parentState);
  }

  @Test
  public void test_state_get_variable_value() {
    VariableJavaSymbol variable = mock(VariableJavaSymbol.class);

    State parentState = new State();
    State currentState = new State(parentState);

    // undefined variable must be unset
    assertThat(parentState.getVariableValue(variable)).isSameAs(UNSET);

    // variable defined in parent must be visible in current.
    parentState.setVariableValue(variable, NOTNULL);
    assertThat(parentState.getVariableValue(variable)).isSameAs(NOTNULL);
    assertThat(currentState.getVariableValue(variable)).isSameAs(NOTNULL);

    // variable redefined in current must not affect value in parent.
    currentState.setVariableValue(variable, NULL);
    assertThat(parentState.getVariableValue(variable)).isSameAs(NOTNULL);
    assertThat(currentState.getVariableValue(variable)).isSameAs(NULL);
  }

  private AbstractValue testMerge(AbstractValue parentValue, AbstractValue trueValue, AbstractValue falseValue) {
    VariableJavaSymbol variable = mock(VariableJavaSymbol.class);
    State parentState = new State();
    if (parentValue != null) {
      parentState.setVariableValue(variable, parentValue);
    }
    State trueState = new State(parentState);
    if (trueValue != null) {
      trueState.setVariableValue(variable, trueValue);
    }
    if (falseValue == null) {
      return parentState.mergeValues(trueState, null).getVariableValue(variable);
    }
    State falseState = new State(parentState);
    falseState.setVariableValue(variable, falseValue);
    return parentState.mergeValues(trueState, falseState).getVariableValue(variable);
  }

  @Test
  public void test_state_merge_values() {
    State state = new State();
    assertThat(state.mergeValues(new State(), new State())).isSameAs(state);
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
  public void test_state_invalidate_values() {
    State parentState = new State();
    State currentState = new State(parentState);
    VariableJavaSymbol parentVariable = mock(VariableJavaSymbol.class);
    VariableJavaSymbol childVariable = mock(VariableJavaSymbol.class);
    parentState.setVariableValue(parentVariable, NULL);
    currentState.setVariableValue(childVariable, NULL);

    assertThat(currentState.invalidateValues()).isSameAs(currentState);

    assertThat(currentState.getVariableValue(childVariable)).isSameAs(UNKNOWN);
    assertThat(currentState.getVariableValue(parentVariable)).isSameAs(NULL);
  }

  @Test
  public void test_state_copy_values_from() {
    VariableJavaSymbol parentVariable = mock(VariableJavaSymbol.class);
    VariableJavaSymbol childVariable = mock(VariableJavaSymbol.class);
    VariableJavaSymbol bothVariable = mock(VariableJavaSymbol.class);
    State parentState = new State();
    parentState.setVariableValue(parentVariable, NULL);
    parentState.setVariableValue(bothVariable, NULL);
    State childState = new State(parentState);
    childState.setVariableValue(childVariable, NOTNULL);
    childState.setVariableValue(bothVariable, NOTNULL);

    parentState.copyValuesFrom(childState);

    assertThat(parentState.getVariableValue(parentVariable)).isSameAs(NULL);
    assertThat(parentState.getVariableValue(childVariable)).isSameAs(NOTNULL);
    assertThat(parentState.getVariableValue(bothVariable)).isSameAs(NOTNULL);
  }

  @Test
  public void test_state_merge_conditional_and() {
    VariableJavaSymbol variable1 = mock(VariableJavaSymbol.class);
    VariableJavaSymbol variable2 = mock(VariableJavaSymbol.class);
    VariableJavaSymbol variable3 = mock(VariableJavaSymbol.class);

    // initial state with variables set to null.
    State currentState = new State();
    currentState.setVariableValue(variable1, NULL);
    currentState.setVariableValue(variable2, NULL);
    currentState.setVariableValue(variable3, NULL);

    State conditionState = new State(currentState);
    ConditionalState conditionalState = new ConditionalState(conditionState);

    // simulates variable1 != null && variable2 != null
    ConditionalState leftConditionalState = new ConditionalState(conditionState);
    leftConditionalState.trueState.setVariableValue(variable1, NOTNULL);
    leftConditionalState.falseState.setVariableValue(variable1, NULL);
    ConditionalState rightConditionalState = new ConditionalState(leftConditionalState.trueState);
    rightConditionalState.trueState.setVariableValue(variable2, NOTNULL);
    rightConditionalState.falseState.setVariableValue(variable2, NULL);
    conditionalState.mergeConditionalAnd(leftConditionalState, rightConditionalState);

    // in the resulting trueState both conditions are true
    assertThat(conditionalState.trueState.getVariableValue(variable1)).isSameAs(NOTNULL);
    assertThat(conditionalState.trueState.getVariableValue(variable2)).isSameAs(NOTNULL);
    // in the resulting falesState variable1 is unknown, since its condition was both true and false,
    // and variable2 is unknown, since its condition was either false or not tested.
    assertThat(conditionalState.falseState.getVariableValue(variable1)).isSameAs(UNKNOWN);
    assertThat(conditionalState.falseState.getVariableValue(variable2)).isSameAs(UNKNOWN);
    // variables not checked in conditions must remain unchanged.
    assertThat(conditionalState.trueState.getVariableValue(variable3)).isSameAs(NULL);
    assertThat(conditionalState.falseState.getVariableValue(variable3)).isSameAs(NULL);
  }

  @Test
  public void test_state_merge_conditional_or() {
    VariableJavaSymbol variable1 = mock(VariableJavaSymbol.class);
    VariableJavaSymbol variable2 = mock(VariableJavaSymbol.class);
    VariableJavaSymbol variable3 = mock(VariableJavaSymbol.class);

    // initial state with variables set to not null.
    State currentState = new State();
    currentState.setVariableValue(variable1, NOTNULL);
    currentState.setVariableValue(variable2, NOTNULL);
    currentState.setVariableValue(variable3, NOTNULL);

    State conditionState = new State(currentState);
    ConditionalState conditionalState = new ConditionalState(conditionState);

    // simulates variable1 != null || variable2 != null
    ConditionalState leftConditionalState = new ConditionalState(conditionState);
    leftConditionalState.trueState.setVariableValue(variable1, NOTNULL);
    leftConditionalState.falseState.setVariableValue(variable1, NULL);
    ConditionalState rightConditionalState = new ConditionalState(leftConditionalState.trueState);
    rightConditionalState.trueState.setVariableValue(variable2, NOTNULL);
    rightConditionalState.falseState.setVariableValue(variable2, NULL);
    conditionalState.mergeConditionalOr(leftConditionalState, rightConditionalState);

    // in the resulting trueState variable1 is unknown, since its condition was both true and false,
    // and variable2 is unknown, since its conditiona was either true or not tested.
    assertThat(conditionalState.trueState.getVariableValue(variable1)).isSameAs(UNKNOWN);
    assertThat(conditionalState.trueState.getVariableValue(variable2)).isSameAs(UNKNOWN);
    // in the resulting falseState both conditions are false
    assertThat(conditionalState.falseState.getVariableValue(variable1)).isSameAs(NULL);
    assertThat(conditionalState.falseState.getVariableValue(variable2)).isSameAs(NULL);
    // variables not checked in conditions must remain unchanged.
    assertThat(conditionalState.trueState.getVariableValue(variable3)).isSameAs(NOTNULL);
    assertThat(conditionalState.falseState.getVariableValue(variable3)).isSameAs(NOTNULL);
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
