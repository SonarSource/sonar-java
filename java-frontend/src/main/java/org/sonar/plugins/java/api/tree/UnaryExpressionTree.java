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

/**
 * Postfix or unary expression.
 *
 * JLS 15.14 and 15.15
 *
 * <pre>
 *   {@link #expression()} {@link Tree.Kind#POSTFIX_INCREMENT ++}
 *   {@link #expression()} {@link Tree.Kind#POSTFIX_DECREMENT --}
 *   {@link Tree.Kind#PREFIX_INCREMENT ++} {@link #expression()}
 *   {@link Tree.Kind#PREFIX_DECREMENT --} {@link #expression()}
 *   {@link Tree.Kind#UNARY_PLUS +} {@link #expression()}
 *   {@link Tree.Kind#UNARY_MINUS -} {@link #expression()}
 *   {@link Tree.Kind#BITWISE_COMPLEMENT ~} {@link #expression()}
 *   {@link Tree.Kind#LOGICAL_COMPLEMENT !} {@link #expression()}
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface UnaryExpressionTree extends ExpressionTree {

  SyntaxToken operatorToken();

  ExpressionTree expression();

}
