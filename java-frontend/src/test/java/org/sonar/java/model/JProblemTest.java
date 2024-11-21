/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JProblemTest {

  @Test
  void test_equals() {
    JProblem p1 = new JProblem("message1", JProblem.Type.UNDEFINED_TYPE);
    assertFalse(p1.equals(null));
    assertTrue(p1.equals(p1));

    JProblem p2 = new JProblem("message1", JProblem.Type.UNDEFINED_TYPE);
    assertTrue(p1.equals(p2));
    p2 = new JProblem("message2", JProblem.Type.UNDEFINED_TYPE);
    assertFalse(p1.equals(p2));
    p2 = new JProblem("message1", JProblem.Type.PREVIEW_FEATURE_USED);
    assertFalse(p1.equals(p2));
  }

  @Test
  void test_hashcode() {
    assertEquals(
      new JProblem("a", JProblem.Type.UNDEFINED_TYPE).hashCode(),
      new JProblem("a", JProblem.Type.UNDEFINED_TYPE).hashCode());
    assertNotEquals(
      new JProblem("a", JProblem.Type.UNDEFINED_TYPE).hashCode(),
      new JProblem("b", JProblem.Type.UNDEFINED_TYPE).hashCode());
    assertNotEquals(
      new JProblem("a", JProblem.Type.UNDEFINED_TYPE).hashCode(),
      new JProblem("a", JProblem.Type.PREVIEW_FEATURE_USED).hashCode());
  }
}
