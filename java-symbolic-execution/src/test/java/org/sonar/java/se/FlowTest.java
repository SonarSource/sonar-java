/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FlowTest {

  @Test
  void test_first_flow_location() {
    Flow flow1 = Flow.builder()
      .add(locationWithMockTree("last"))
      .add(locationWithMockTree("first"))
      .build();
    List<JavaFileScannerContext.Location> collect = flow1.firstFlowLocation().toList();
    assertThat(collect).hasSize(1);
    assertThat(collect.get(0).msg).isEqualTo("first");

    Stream<JavaFileScannerContext.Location> empty = Flow.empty().firstFlowLocation();
    assertThat(empty).isEmpty();
  }

  @Test
  void testEquals() {

    Flow flow = Flow.builder()
      .add(locationWithMockTree("first"))
      .add(locationWithMockTree("second"))
      .build();

    Flow flow1 = Flow.builder().addAll(flow).build();
    Flow flow2 = Flow.builder().addAll(flow).setAsExceptional().build();

    assertThat(flow1)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      .isNotEqualTo(Flow.empty())
      .isNotEqualTo(flow2)
      .isEqualTo(flow1)
      .isEqualTo(Flow.of(flow1));
  }

  private static JavaFileScannerContext.Location locationWithMockTree(String message) {
    return new JavaFileScannerContext.Location(message, mock(Tree.class));
  }

}
