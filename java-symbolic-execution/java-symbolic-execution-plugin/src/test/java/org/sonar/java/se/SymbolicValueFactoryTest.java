/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.se;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class SymbolicValueFactoryTest {

  private static class TestSymbolicValue extends SymbolicValue {

  }

  private static class TestSymbolicValueFactory implements SymbolicValueFactory {

    @Override
    public SymbolicValue createSymbolicValue() {
      return new TestSymbolicValue();
    }
  }

  @Test
  void testFactory() {
    final IdentifierTree tree = new IdentifierTreeImpl(new InternalSyntaxToken(1, 1, "id", Collections.<SyntaxTrivia>emptyList(), false));
    final ConstraintManager manager = new ConstraintManager();
    SymbolicValue symbolicValue = manager.createSymbolicValue(tree);
    assertThat(symbolicValue.getClass()).as("Created without factory").isSameAs(SymbolicValue.class);
    manager.setValueFactory(new TestSymbolicValueFactory());
    symbolicValue = manager.createSymbolicValue(tree);
    assertThat(symbolicValue.getClass()).as("Created with factory").isSameAs(TestSymbolicValue.class);
    assertThat(symbolicValue.references(symbolicValue)).isFalse();
    manager.setValueFactory(new TestSymbolicValueFactory());
    SymbolicValueFactory newFactory = new TestSymbolicValueFactory();
    try {
      manager.setValueFactory(newFactory);
      fail("Should not have been able to add a second factory to the contraints manager");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).as("Exception message").isEqualTo("The symbolic value factory has already been defined by another checker!");
    }
    symbolicValue = manager.createSymbolicValue(tree);
    assertThat(symbolicValue.getClass()).as("Created with first factory").isSameAs(TestSymbolicValue.class);
    symbolicValue = manager.createSymbolicValue(tree);
    assertThat(symbolicValue.getClass()).as("Created after factory usage").isSameAs(SymbolicValue.class);
  }
}
