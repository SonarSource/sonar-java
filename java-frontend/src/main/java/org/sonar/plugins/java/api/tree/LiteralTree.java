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
 * Literal expression.
 *
 * JLS 15.8.1
 *
 * {@link Tree.Kind#INT_LITERAL},
 * {@link Tree.Kind#LONG_LITERAL},
 * {@link Tree.Kind#FLOAT_LITERAL},
 * {@link Tree.Kind#DOUBLE_LITERAL},
 * {@link Tree.Kind#BOOLEAN_LITERAL},
 * {@link Tree.Kind#CHAR_LITERAL},
 * {@link Tree.Kind#STRING_LITERAL},
 * {@link Tree.Kind#NULL_LITERAL}
 *
 * <pre>
 *   {@link #value()}
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface LiteralTree extends ExpressionTree {

  String value();

  SyntaxToken token();
}
