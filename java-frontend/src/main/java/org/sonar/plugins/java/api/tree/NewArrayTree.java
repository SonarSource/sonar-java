/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;

/**
 * Array creation expression.
 *
 * JLS 15.10
 *
 * <pre>
 *   new {@link #type()} {@link #dimensions()} {@link #initializers()}
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface NewArrayTree extends ExpressionTree {

  @Nullable
  TypeTree type();

  @Nullable
  SyntaxToken newKeyword();

  List<ArrayDimensionTree> dimensions();

  @Nullable
  SyntaxToken openBraceToken();

  ListTree<ExpressionTree> initializers();

  @Nullable
  SyntaxToken closeBraceToken();
}
