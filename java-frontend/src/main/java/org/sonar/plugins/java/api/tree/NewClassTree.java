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
package org.sonar.plugins.java.api.tree;

import com.google.common.annotations.Beta;
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

  Symbol constructorSymbol();

}
