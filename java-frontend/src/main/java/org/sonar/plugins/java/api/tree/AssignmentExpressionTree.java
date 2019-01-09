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
 * Assignment expression.
 *
 * JLS 15.26.1
 *
 * <pre>
 *   {@link #variable()} {@link Tree.Kind#ASSIGNMENT =} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#MULTIPLY_ASSIGNMENT *=} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#DIVIDE_ASSIGNMENT /=} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#REMAINDER_ASSIGNMENT %=} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#PLUS_ASSIGNMENT +=} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#MINUS_ASSIGNMENT -=} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#LEFT_SHIFT_ASSIGNMENT <<=} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#RIGHT_SHIFT_ASSIGNMENT >>=} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#UNSIGNED_RIGHT_SHIFT_ASSIGNMENT >>>=} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#AND_ASSIGNMENT &=} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#XOR_ASSIGNMENT ^=} {@link #expression()}
 *   {@link #variable()} {@link Tree.Kind#OR_ASSIGNMENT |=} {@link #expression()}
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface AssignmentExpressionTree extends ExpressionTree {

  ExpressionTree variable();

  SyntaxToken operatorToken();

  ExpressionTree expression();

}
