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
package org.sonar.java.model;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;

import java.util.List;
import java.util.Optional;

public final class Hacks {
  private Hacks() {
  }

  /**
   * Replacement for {@link JavaSymbol.VariableJavaSymbol#constantValue()}
   */
  public static Optional<Object> constantValue(Symbol.VariableSymbol symbol) {
    Object value = ((IVariableBinding) ((JVariableSymbol) symbol).binding).getConstantValue();
    return Optional.ofNullable(value);
  }

  /**
   * Replacement for {@link JavaSymbol.MethodJavaSymbol#isVarArgs()}
   */
  public static boolean isVarArgs(Symbol.MethodSymbol symbol) {
    return ((IMethodBinding) ((JMethodSymbol) symbol).binding).isVarargs();
  }

  public static boolean isNative(Symbol.MethodSymbol symbol) {
    if (symbol.isUnknown()) {
      return false;
    }
    return Modifier.isNative(
      ((JMethodSymbol) symbol).binding.getModifiers()
    );
  }

  /**
   * Replacement for {@link JavaSymbol.MethodJavaSymbol#isConstructor()}
   */
  public static boolean isConstructor(Symbol.MethodSymbol symbol) {
    return "<init>".equals(symbol.name());
  }

  /**
   * Replacement for {@link JavaSymbol.MethodJavaSymbol#getParameters()}
   */
  public static List<Symbol> getParameters(Symbol.MethodSymbol symbol) {
    return ((JMethodSymbol) symbol).getParameters();
  }

}
