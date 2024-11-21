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

import java.util.List;

/**
 * Block.
 * <p>
 * JLS 8.6. Instance Initializer ({@link Tree.Kind#INITIALIZER}):
 * <pre>
 *   { {@link #body()} }
 * </pre>
 * JLS 14.2 Block ({@link Tree.Kind#BLOCK}):
 * <pre>
 *   { {@link #body()} }
 * </pre>
 * </p>
 *
 * @since Java 1.3
 */
@Beta
public interface BlockTree extends StatementTree {

  SyntaxToken openBraceToken();

  List<StatementTree> body();

  SyntaxToken closeBraceToken();

}
