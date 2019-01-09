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
 * Binary expression.
 *
 * JLS from 15.17 to 15.24
 *
 * <pre>
 *   {@link #leftOperand()} {@link Tree.Kind#MULTIPLY *} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#DIVIDE /} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#REMAINDER %} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#PLUS +} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#MINUS -} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#LEFT_SHIFT <<} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#RIGHT_SHIFT >>} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#UNSIGNED_RIGHT_SHIFT >>>} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#LESS_THAN <} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#GREATER_THAN >} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#LESS_THAN_OR_EQUAL_TO <=} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#GREATER_THAN_OR_EQUAL_TO >=} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#EQUAL_TO ==} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#NOT_EQUAL_TO !=} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#AND &} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#XOR ^} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#OR |} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#CONDITIONAL_AND &&} {@link #rightOperand()}
 *   {@link #leftOperand()} {@link Tree.Kind#CONDITIONAL_OR ||} {@link #rightOperand()}
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface BinaryExpressionTree extends ExpressionTree {

  ExpressionTree leftOperand();

  SyntaxToken operatorToken();

  ExpressionTree rightOperand();

}
