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
package org.sonar.java.se.symbolicvalues;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RelationalSymbolicValueTest {

  ConstraintManager constraintManager = new ConstraintManager();
  SymbolicValue a = new SymbolicValue(1);
  SymbolicValue b = new SymbolicValue(2);
  int id = 3;

  @Test
  public void test_create() throws Exception {
    ImmutableList<SymbolicValue> computedFrom = ImmutableList.of(b, a);
    assertThat(create(Tree.Kind.EQUAL_TO, computedFrom)).hasToString("SV_1==SV_2");
    assertThat(create(Tree.Kind.NOT_EQUAL_TO, computedFrom)).hasToString("!SV_1==SV_2");
    assertThat(create(Tree.Kind.GREATER_THAN, computedFrom)).hasToString("SV_2<SV_1");
    assertThat(create(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, computedFrom)).hasToString("!SV_1<SV_2");
    assertThat(create(Tree.Kind.LESS_THAN, computedFrom)).hasToString("SV_1<SV_2");
    assertThat(create(Tree.Kind.LESS_THAN_OR_EQUAL_TO, computedFrom)).hasToString("!SV_2<SV_1");
  }

  private SymbolicValue create(Tree.Kind kind, ImmutableList<SymbolicValue> computedFrom) {
    return constraintManager
      .createBinarySymbolicValue(new BinaryExpressionTreeImpl(kind, mock(ExpressionTree.class), mock(InternalSyntaxToken.class), mock(ExpressionTree.class)), computedFrom);
  }
}
