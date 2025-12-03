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

import javax.annotation.Nullable;

/**
 * 'yield' statement.
 *
 * JLS14 14.21
 *
 * <pre>
 *   yield {@link #expression()} ;
 * </pre>
 *
 * @since Java 14
 */
@Beta
public interface YieldStatementTree extends StatementTree {

  /**
   * @return null in case of implicit yield-statement
   */
  @Nullable
  SyntaxToken yieldKeyword();

  ExpressionTree expression();

  SyntaxToken semicolonToken();

}
