/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.se;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExplodedGraphTest {

  @Test
  public void test_node_parents() {
    ExplodedGraph eg = new ExplodedGraph();
    ExplodedGraph.Node child = eg.node(mockProgramPoint("child"), null);
    assertThat(child.edges()).isEmpty();
    assertThat(child.parent()).isNull();

    ExplodedGraph.Node parent = eg.node(mockProgramPoint("parent"), null);
    child.addParent(parent, null);
    assertThat(child.edges()).hasSize(1);
    ExplodedGraph.Edge edge = child.edges().iterator().next();
    assertThat(edge.parent).isEqualTo(parent);
    assertThat(child.parent()).isEqualTo(parent);

    // adding same parent twice
    child.addParent(parent, null);
    assertThat(child.edges()).hasSize(1);


    assertThat(child.parents()).hasSize(1);
    ExplodedGraph.Node parent2 = eg.node(mockProgramPoint("parent2"), null);
    child.addParent(parent2, null);
    assertThat(child.edges()).hasSize(2);
    assertThat(child.edges()).extracting("parent").contains(parent, parent2);
    assertThat(child.parents()).hasSize(2);
  }

  private ProgramPoint mockProgramPoint(String toString) {
    ProgramPoint mock = mock(ProgramPoint.class);
    when(mock.toString()).thenReturn(toString);
    return mock;
  }

}
