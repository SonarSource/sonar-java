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
package org.sonar.plugins.java.api.tree;

import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.semantic.Symbol;

import javax.annotation.Nullable;

/**
 * Method invocation expression.
 *
 * JLS 15.12
 *
 * <pre>
 *   {@link #methodSelect()} ( {@link #arguments()} )
 *   this . {@link #typeArguments} {@link #methodSelect()} ( {@link #arguments} )
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface MethodInvocationTree extends ExpressionTree {

  /**
   * @since Java 1.5
   */
  @Nullable
  TypeArguments typeArguments();

  ExpressionTree methodSelect();

  Arguments arguments();

  /**
   * @deprecated in favor of {@link #methodSymbol()}, which returns the narrower type {@link Symbol.MethodSymbol} instead of {@link Symbol}.
   */
  @Deprecated(since = "7.16", forRemoval = true)
  Symbol symbol();

  Symbol.MethodSymbol methodSymbol();
}
