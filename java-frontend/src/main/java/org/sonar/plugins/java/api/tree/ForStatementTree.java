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

import javax.annotation.Nullable;

/**
 * 'for' statement.
 *
 * JLS 14.14
 *
 * <pre>
 *   for ( {@link #initializer()} ; {@link #condition()} ; {@link #update()} ) {@link #statement()}
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface ForStatementTree extends StatementTree {

  SyntaxToken forKeyword();

  SyntaxToken openParenToken();

  ListTree<StatementTree> initializer();

  SyntaxToken firstSemicolonToken();

  @Nullable
  ExpressionTree condition();

  SyntaxToken secondSemicolonToken();

  ListTree<StatementTree> update();

  SyntaxToken closeParenToken();

  StatementTree statement();

}
