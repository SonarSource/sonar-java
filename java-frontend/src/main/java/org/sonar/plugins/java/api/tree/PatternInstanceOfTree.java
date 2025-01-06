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

import javax.annotation.CheckForNull;
import org.sonar.java.annotations.Beta;

/**
 * 'instanceof' expression with pattern-matching.
 *
 * JLS 15.20.2
 *
 * <pre>
 *   {@link #expression()} instanceof {@link #pattern()}
 * </pre>
 *
 * @since Java 16
 */
@Beta
public interface PatternInstanceOfTree extends ExpressionTree {

  ExpressionTree expression();

  SyntaxToken instanceofKeyword();

  /**
   * @since Java 16
   * @deprecated Use {@link PatternInstanceOfTree#pattern()}
   * @return null for all patterns that are not {@link TypePatternTree}.
   */
  @Deprecated(since = "7.19", forRemoval = true)
  @CheckForNull
  VariableTree variable();

  /**
   * @since Java 19
   */
  PatternTree pattern();

}
