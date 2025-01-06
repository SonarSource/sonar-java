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

import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.semantic.Symbol;

import javax.annotation.Nullable;

/**
 * Variable declaration.
 *
 * JLS 8.3, 14.4
 *
 * <pre>
 *   {@link #modifiers()} {@link #type()} {@link #simpleName()} {@link #initializer()} {@link #endToken()}
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface VariableTree extends StatementTree {

  ModifiersTree modifiers();

  TypeTree type();

  IdentifierTree simpleName();

  @Nullable
  SyntaxToken equalToken();

  @Nullable
  ExpressionTree initializer();

  Symbol symbol();

  @Nullable
  SyntaxToken endToken();

}
