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
package org.sonar.java.se;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExplodedGraphTest {

  @Test
  void test_node_parents() {
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

  @Test
  void test_node_equality() {
    ExplodedGraph eg = new ExplodedGraph();

    ProgramPoint pp1 = mockProgramPoint("pp1");
    ProgramPoint pp2 = mockProgramPoint("pp2");
    ProgramState ps1 = mock(ProgramState.class);
    ProgramState ps2 = mock(ProgramState.class);

    ExplodedGraph.Node node1 = eg.node(pp1, ps1);
    ExplodedGraph.Node node2 = eg.node(pp1, ps1);
    assertThat(node1).isEqualTo(node2);

    node2 = eg.node(pp1, ps2);
    assertThat(node1).isNotEqualTo(node2);

    node2 = eg.node(pp2, ps1);
    assertThat(node1).isNotEqualTo(node2);

    String notANode = "not a node";
    assertThat(node1).isNotEqualTo(notANode);
  }

  private ProgramPoint mockProgramPoint(String toString) {
    ProgramPoint mock = mock(ProgramPoint.class);
    when(mock.toString()).thenReturn(toString);
    return mock;
  }

}
