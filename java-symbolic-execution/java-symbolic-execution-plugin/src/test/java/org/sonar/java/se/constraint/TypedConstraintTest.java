/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.se.constraint;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.SESymbols;

import static org.assertj.core.api.Assertions.assertThat;

class TypedConstraintTest {

  @Test
  void test_equals_hashcode() {
    TypedConstraint object1 = new TypedConstraint("java.lang.String");
    TypedConstraint object2 = new TypedConstraint("java.lang.String");
    assertThat(object1)
      .isEqualTo(object1)
      .isEqualTo(object2)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .hasSameHashCodeAs(object2);

    TypedConstraint nullTC1 = new TypedConstraint(SESymbols.unknownType.fullyQualifiedName());
    TypedConstraint nullTC2 = new TypedConstraint(SESymbols.unknownType.fullyQualifiedName());
    assertThat(nullTC1).isNotEqualTo(nullTC2);
    assertThat(object1).isNotEqualTo(nullTC1);
  }
}
