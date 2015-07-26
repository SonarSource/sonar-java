/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.resolve;

import org.junit.Test;
import org.sonar.java.resolve.Scope.OrderedScope;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ScopeTest {

  private JavaSymbol owner = mock(JavaSymbol.class);

  @Test
  public void overloading() {
    Scope scope = new Scope(owner);

    JavaSymbol first = new JavaSymbol(0, 0, "overloaded", null);
    scope.enter(first);

    JavaSymbol second = new JavaSymbol(0, 0, "overloaded", null);
    scope.enter(second);

    assertThat(scope.lookup("overloaded")).containsOnly(first, second);
  }

  @Test
  public void shadowing() {
    Scope outerScope = new Scope(owner);
    Scope scope = new Scope(outerScope);

    JavaSymbol first = new JavaSymbol(0, 0, "shadowed", null);
    outerScope.enter(first);

    JavaSymbol second = new JavaSymbol(0, 0, "name", null);
    outerScope.enter(second);

    JavaSymbol third = new JavaSymbol(0, 0, "shadowed", null);
    scope.enter(third);

    assertThat(scope.lookup("shadowed")).containsOnly(third);
    assertThat(scope.lookup("name")).containsOnly(second);
    assertThat(scope.lookup("nonexistent")).isEmpty();
  }

  @Test
  public void ordered() {
    OrderedScope scope = new OrderedScope(owner);

    JavaSymbol first = new JavaSymbol(0, 0, "first", null);
    scope.enter(first);

    JavaSymbol second = new JavaSymbol(0, 0, "second", null);
    scope.enter(second);

    JavaSymbol third = new JavaSymbol(0, 0, "third", null);
    scope.enter(third);

    assertThat(scope.scopeSymbols()).containsExactly(first, second, third);
  }

}
