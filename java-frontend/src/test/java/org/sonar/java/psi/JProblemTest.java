/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.psi;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.diagnostics.ProblemKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JProblemTest {

  @Test
  void test_equals() {
    PsiProblem p1 = new PsiProblem("message1", ProblemKind.UNDEFINED_TYPE);
    assertNotNull(p1);
    assertEquals(p1, p1);

    PsiProblem p2 = new PsiProblem("message1", ProblemKind.UNDEFINED_TYPE);
    assertEquals(p1, p2);
    p2 = new PsiProblem("message2", ProblemKind.UNDEFINED_TYPE);
    assertNotEquals(p1, p2);
    p2 = new PsiProblem("message1", ProblemKind.PREVIEW_FEATURE_USED);
    assertNotEquals(p1, p2);
  }

  @Test
  void test_hashcode() {
    assertEquals(
      new PsiProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode(),
      new PsiProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode());
    assertNotEquals(
      new PsiProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode(),
      new PsiProblem("b", ProblemKind.UNDEFINED_TYPE).hashCode());
    assertNotEquals(
      new PsiProblem("a", ProblemKind.UNDEFINED_TYPE).hashCode(),
      new PsiProblem("a", ProblemKind.PREVIEW_FEATURE_USED).hashCode());
  }

}
