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
import org.sonar.java.checks.NullPointerCheck.State;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.resolve.Symbol.VariableSymbol;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class NullPointerCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void check() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/NullPointerCheck.java"),
      new VisitorsBridge(new NullPointerCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(72).withMessage("array1 can be null.")
      .next().atLine(73).withMessage("array1 can be null.")
      .next().atLine(74).withMessage("array1 can be null.")
      .next().atLine(81).withMessage("array2 can be null.")
      .next().atLine(82).withMessage("array2 can be null.")
      .next().atLine(83).withMessage("array2 can be null.")
      .next().atLine(85).withMessage("Value returned by method 'checkForNullMethod' can be null.")
      .next().atLine(86).withMessage("Value returned by method 'checkForNullMethod' can be null.")
      .next().atLine(87).withMessage("Value returned by method 'checkForNullMethod' can be null.")
      .next().atLine(95).withMessage("array1 can be null.")
      .next().atLine(96).withMessage("array1 can be null.")
      .next().atLine(97).withMessage("array1 can be null.")
      .next().atLine(104).withMessage("array2 can be null.")
      .next().atLine(105).withMessage("array2 can be null.")
      .next().atLine(106).withMessage("array2 can be null.")
      .next().atLine(108).withMessage("Value returned by method 'nullableMethod' can be null.")
      .next().atLine(109).withMessage("Value returned by method 'nullableMethod' can be null.")
      .next().atLine(110).withMessage("Value returned by method 'nullableMethod' can be null.")
      .next().atLine(161).withMessage("Value returned by method 'checkForNullMethod' can be null.")
      .next().atLine(162).withMessage("Value returned by method 'checkForNullMethod' can be null.")
      .next().atLine(163).withMessage("Value returned by method 'checkForNullMethod' can be null.")
      .next().atLine(168).withMessage("null is dereferenced or passed as argument.")
      .next().atLine(169).withMessage("null is dereferenced or passed as argument.")
      .next().atLine(170).withMessage("null is dereferenced or passed as argument.")
      .next().atLine(176).withMessage("argument1 can be null.")
      .next().atLine(182).withMessage("argument1 can be null.")
      .next().atLine(187).withMessage("argument1 can be null.")
      .next().atLine(189).withMessage("argument1 can be null.")
      .next().atLine(198).withMessage("argument can be null.")
      .next().atLine(199).withMessage("argument can be null.")
      .next().atLine(230).withMessage("argument4 can be null.")
      .next().atLine(235).withMessage("var1 can be null.")
      .next().atLine(237).withMessage("var2 can be null.")
      .next().atLine(245).withMessage("object can be null.")
      .next().atLine(247).withMessage("object can be null.");
  }

  @Test
  public void test_state() {
    VariableSymbol variable = mock(VariableSymbol.class);

    State parentState = new State();
    State currentState = new State(parentState);

    assertThat(currentState.parentState).isSameAs(parentState);

    // undefined variable must be unknown
    assertThat(parentState.getVariableValue(variable)).isSameAs(AbstractValue.UNKNOWN);

    // variable defined in parent must be visible in current.
    parentState.setVariableValue(variable, AbstractValue.NOTNULL);
    assertThat(parentState.getVariableValue(variable)).isSameAs(AbstractValue.NOTNULL);
    assertThat(currentState.getVariableValue(variable)).isSameAs(AbstractValue.NOTNULL);

    // variable redefined in current must not affect value in parent.
    currentState.setVariableValue(variable, AbstractValue.NULL);
    assertThat(parentState.getVariableValue(variable)).isSameAs(AbstractValue.NOTNULL);
    assertThat(currentState.getVariableValue(variable)).isSameAs(AbstractValue.NULL);
  }

  private AbstractValue testMerge(AbstractValue parentValue, AbstractValue trueValue, AbstractValue falseValue) {
    VariableSymbol variable = mock(VariableSymbol.class);
    State parentState = new State();
    if (parentValue != null) {
      parentState.setVariableValue(variable, parentValue);
    }
    State trueState = new State(parentState);
    if (trueValue != null) {
      trueState.setVariableValue(variable, trueValue);
    }
    if (falseValue == null) {
      return parentState.merge(trueState, null).getVariableValue(variable);
    }
    State falseState = new State(parentState);
    falseState.setVariableValue(variable, falseValue);
    return parentState.merge(trueState, falseState).getVariableValue(variable);
  }

  @Test
  public void test_state_merge() {
    // variable defined in parentState only should not change
    assertThat(testMerge(AbstractValue.NOTNULL, null, null)).isSameAs(AbstractValue.NOTNULL);
    // variable defined in parentState and trueState, must match
    assertThat(testMerge(AbstractValue.NOTNULL, AbstractValue.NOTNULL, null)).isSameAs(AbstractValue.NOTNULL);
    assertThat(testMerge(AbstractValue.NULL, AbstractValue.NULL, null)).isSameAs(AbstractValue.NULL);
    assertThat(testMerge(AbstractValue.NOTNULL, AbstractValue.NULL, null)).isSameAs(AbstractValue.UNKNOWN);
    // variable defined in parentState and falseState, must match
    assertThat(testMerge(AbstractValue.NOTNULL, null, AbstractValue.NOTNULL)).isSameAs(AbstractValue.NOTNULL);
    assertThat(testMerge(AbstractValue.NULL, null, AbstractValue.NULL)).isSameAs(AbstractValue.NULL);
    assertThat(testMerge(AbstractValue.NOTNULL, null, AbstractValue.NULL)).isSameAs(AbstractValue.UNKNOWN);
    // variable defined in trueState and falseState, must match
    assertThat(testMerge(null, AbstractValue.NOTNULL, AbstractValue.NOTNULL)).isSameAs(AbstractValue.NOTNULL);
    assertThat(testMerge(null, AbstractValue.NULL, AbstractValue.NULL)).isSameAs(AbstractValue.NULL);
    assertThat(testMerge(null, AbstractValue.NOTNULL, AbstractValue.NULL)).isSameAs(AbstractValue.UNKNOWN);
    // variable defined in parentState, trueState and falseState, trueState and falseState must match
    assertThat(testMerge(AbstractValue.NOTNULL, AbstractValue.NOTNULL, AbstractValue.NOTNULL)).isSameAs(AbstractValue.NOTNULL);
    assertThat(testMerge(AbstractValue.NOTNULL, AbstractValue.NULL, AbstractValue.NULL)).isSameAs(AbstractValue.NULL);
    assertThat(testMerge(AbstractValue.NOTNULL, AbstractValue.NOTNULL, AbstractValue.NULL)).isSameAs(AbstractValue.UNKNOWN);
    assertThat(testMerge(AbstractValue.NULL, AbstractValue.NOTNULL, AbstractValue.NOTNULL)).isSameAs(AbstractValue.NOTNULL);
    assertThat(testMerge(AbstractValue.NULL, AbstractValue.NULL, AbstractValue.NULL)).isSameAs(AbstractValue.NULL);
    assertThat(testMerge(AbstractValue.NULL, AbstractValue.NOTNULL, AbstractValue.NULL)).isSameAs(AbstractValue.UNKNOWN);
  }

}
