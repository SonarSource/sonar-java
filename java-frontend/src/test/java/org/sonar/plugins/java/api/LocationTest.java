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
package org.sonar.plugins.java.api;

import org.junit.Test;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class LocationTest {

  @Test
  public void testLocation() throws Exception {
    String message = "message";
    Tree node = mock(Tree.class);
    JavaFileScannerContext.Location location = new JavaFileScannerContext.Location(message, node);
    assertThat(location.msg).isEqualTo(message);
    assertThat(location.syntaxNode).isEqualTo(node);
  }

  @Test
  public void testEquality() {
    String message = "message";
    Tree node = mock(Tree.class);
    JavaFileScannerContext.Location location = new JavaFileScannerContext.Location(message, node);

    // same message, same node
    assertThat(location).isEqualTo(new JavaFileScannerContext.Location(message, node));
    // same object
    assertThat(location).isEqualTo(location);

    assertThat(location).isNotEqualTo(new JavaFileScannerContext.Location("msg", node));
    assertThat(location).isNotEqualTo(new JavaFileScannerContext.Location(message, mock(Tree.class)));
    assertThat(location).isNotEqualTo(null);
    assertThat(location).isNotEqualTo(new Object());
  }

  @Test
  public void testHashCode() {
    String message = "message";
    Tree node = mock(Tree.class);
    JavaFileScannerContext.Location location = new JavaFileScannerContext.Location(message, node);

    // same message, same node
    assertThat(location.hashCode()).isEqualTo(new JavaFileScannerContext.Location(message, node).hashCode());
    // different location
    assertThat(location.hashCode()).isNotEqualTo(new JavaFileScannerContext.Location("msg", node).hashCode());
  }
}
