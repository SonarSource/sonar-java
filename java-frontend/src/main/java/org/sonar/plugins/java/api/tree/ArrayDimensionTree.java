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

import java.util.List;

/**
 * Describe an array dimension.
 * 
 * JLS 15.10.1
 * 
 * <pre>
 *   {@link #annotations()} [ {@link #expression()} ]
 * </pre>
 *
 */
@Beta
public interface ArrayDimensionTree extends Tree {

  List<AnnotationTree> annotations();

  SyntaxToken openBracketToken();

  @Nullable
  ExpressionTree expression();

  SyntaxToken closeBracketToken();
}
