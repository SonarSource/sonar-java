/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
 * Class instance creation expression.
 *
 * JLS 15.9
 *
 * <pre>
 *   new {@link #identifier()} ( )
 *   new {@link #identifier()} ( {@link #arguments()} )
 *   new {@link #typeArguments()} {@link #identifier()} ( {@link #arguments()} ) {@link #classBody()}
 *   {@link #enclosingExpression()} . new {@link #identifier()} ( {@link #arguments()} )
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface NewClassTree extends ExpressionTree {

  @Nullable
  ExpressionTree enclosingExpression();

  /**
   * "dot" is not null only when the enclosing expression is present  
   */
  @Nullable
  SyntaxToken dotToken();

  /**
   * "new" keyword is null for {@link EnumConstantTree #initializer()}
   */
  @Nullable
  SyntaxToken newKeyword();

  /**
   * @since Java 1.5
   */
  @Nullable
  TypeArguments typeArguments();

  TypeTree identifier();

  Arguments arguments();

  @Nullable
  ClassTree classBody();

  /**
   * @deprecated in favor of {@link #methodSymbol()}, which returns the narrower type {@link Symbol.MethodSymbol} instead of {@link Symbol}.
   */
  @Deprecated(since = "7.16", forRemoval = true)
  Symbol constructorSymbol();

  Symbol.MethodSymbol methodSymbol();

}
