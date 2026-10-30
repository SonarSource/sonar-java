/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.function.Supplier;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

public final class SemanticValues {

  public static final Supplier<Type> UNKNOWN_TYPE = () -> Type.UNKNOWN;
  public static final Supplier<Symbol> UNKNOWN_SYMBOL = () -> Symbol.UNKNOWN_SYMBOL;
  public static final Supplier<Symbol.TypeSymbol> UNKNOWN_TYPE_SYMBOL = () -> Symbol.TypeSymbol.UNKNOWN_TYPE;
  public static final Supplier<Symbol.MethodSymbol> UNKNOWN_METHOD = () -> Symbol.MethodSymbol.UNKNOWN_METHOD;

  private SemanticValues() {
  }

}
