/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import javax.annotation.Nullable;
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

  /**
   * @return the raw source code value of the literal (the parsed token), including quotes for string literals.
   */
  String value();

  /**
   * @return The source code value of the literal without the surrounding quotes around char or string literals.
   * The escaped characters stay escaped, {@link String#translateEscapes()} is not used.
   * If the string literal is a text block, indentation, first line break and all trailing whitespaces are also removed.
   */
  default String unquotedValue() {
    return value();
  }

  /**
   * @return the literal runtime value. It could be: null, Boolean, String, Character, Long, Integer.
   * Note, you can access the matching return type and support primitives by using the subtypes of {@link LiteralTree}:
   * {@link BooleanLiteralTree#booleanValue()}, {@link StringLiteralTree#stringValue()}, {@link CharLiteralTree#charValue()},
   * {@link LongLiteralTree#longValue()}, {@link IntLiteralTree#intValue()}.
   */
  @Nullable
  Object parsedValue();

  SyntaxToken token();

}
