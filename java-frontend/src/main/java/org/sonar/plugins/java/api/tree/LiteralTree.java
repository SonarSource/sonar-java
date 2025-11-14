/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
 * {@link Tree.Kind#TEXT_BLOCK},
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
