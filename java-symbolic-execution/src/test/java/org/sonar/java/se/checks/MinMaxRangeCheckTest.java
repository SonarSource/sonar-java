/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.se.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.se.CheckerDispatcher;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.utils.SETestUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class MinMaxRangeCheckTest {

  @Test
  void test() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/MinMaxRangeCheck.java"))
      .withCheck(new MinMaxRangeCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_missing_semantics() {
    var identifier = mock(IdentifierTree.class);
    var checkerDispatcher = mock(CheckerDispatcher.class);
    var mockedPS = mock(ProgramState.class);
    when(mockedPS.getValue(any(Symbol.class))).thenReturn(null);
    when(checkerDispatcher.getState()).thenReturn(mockedPS);
    assertEquals(MinMaxRangeCheck.handleNumericalConstant(checkerDispatcher, identifier), mockedPS);
  }
}
