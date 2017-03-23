/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;

import static org.mockito.Mockito.mock;

public class CastArithmeticOperandCheckTest {

  @Test
  public void test() {
    JavaCheckVerifier.verify("src/test/files/checks/CastArithmeticOperandCheck.java", new CastArithmeticOperandCheck());
  }

  @Test
  public void test_no_semantic() {
    JavaCheckVerifier.verifyNoIssueWithoutSemantic("src/test/files/checks/CastArithmeticOperandCheck.java", new CastArithmeticOperandCheck());
  }

  @Test
  public void test_unhandled_tree_kind() throws Exception {
    CastArithmeticOperandCheck check = new CastArithmeticOperandCheck() {
      @Override
      public boolean hasSemantic() {
        return true;
      }
    };
    Assertions.assertThatThrownBy(() -> check.visitNode(new ClassTreeImpl(Tree.Kind.CLASS, mock(SyntaxToken.class), Collections.emptyList(), mock(SyntaxToken.class))))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Tree CLASS not handled.");
  }
}
