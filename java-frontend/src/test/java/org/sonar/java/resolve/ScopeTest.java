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
package org.sonar.java.resolve;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class ScopeTest {

  private JavaSymbol owner = mock(JavaSymbol.class);

  @Test
  public void overloading_for_methods_only() {
    Scope scope = new Scope(owner);

    JavaSymbol firstMethod = new JavaSymbol(JavaSymbol.MTH, 0, "overloaded", null);
    scope.enter(firstMethod);

    JavaSymbol secondMethod = new JavaSymbol(JavaSymbol.MTH, 0, "overloaded", null);
    scope.enter(secondMethod);

    assertThat(scope.lookup("overloaded")).containsOnly(firstMethod, secondMethod);

    try {
      JavaSymbol firstVar = new JavaSymbol(JavaSymbol.VAR, 0, "overloaded", null);
      scope.enter(firstVar);

      JavaSymbol second = new JavaSymbol(JavaSymbol.VAR, 0, "overloaded", null);
      scope.enter(second);
      fail("second symbol should not be accepted by scope");
    } catch (IllegalStateException iae) {
      assertThat(iae).hasMessage("Registering symbol: 'overloaded' twice in the same scope");
    } catch (Exception e) {
      fail("second symbol should not be accepted by scope");
    }
  }

  @Test
  public void namedImport_should_accept_multiple_symbols() throws Exception {
    Scope scope = new Scope.ImportScope(owner);
    JavaSymbol firstMethod = new JavaSymbol(JavaSymbol.MTH, 0, "overloaded", null);
    scope.enter(firstMethod);

    JavaSymbol secondMethod = new JavaSymbol(JavaSymbol.MTH, 0, "overloaded", null);
    scope.enter(secondMethod);

    JavaSymbol firstVar = new JavaSymbol(JavaSymbol.VAR, 0, "overloaded", null);
    scope.enter(firstVar);

    JavaSymbol second = new JavaSymbol(JavaSymbol.VAR, 0, "overloaded", null);
    scope.enter(second);
    assertThat(scope.lookup("overloaded")).containsOnly(firstMethod, secondMethod, firstVar, second);
  }

  @Test
  public void starImport_should_accept_multiple_symbol() throws Exception {
    Scope scope = new Scope.StarImportScope(owner, null);
    JavaSymbol packagePck = new JavaSymbol(JavaSymbol.PCK, 0, "my.package", null);
    scope.enter(packagePck);
    scope.enter(packagePck);
    assertThat(scope.scopeSymbols).hasSize(2);
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
    Scope scope = new Scope(owner);

    JavaSymbol first = new JavaSymbol(0, 0, "first", null);
    scope.enter(first);

    JavaSymbol second = new JavaSymbol(0, 0, "second", null);
    scope.enter(second);

    JavaSymbol third = new JavaSymbol(0, 0, "third", null);
    scope.enter(third);

    assertThat(scope.scopeSymbols()).containsExactly(first, second, third);
  }

}
