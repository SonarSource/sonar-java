/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.java.api.tree;

import com.google.common.annotations.Beta;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 'try' statement.
 *
 * JLS 14.20
 *
 * <pre>
 *   try {@link #block()} {@link #catches()} finally {@link #finallyBlock()}
 *   try ({@link #resources()}) {@link #block()} {@link #catches()} finally {@link #finallyBlock()}
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface TryStatementTree extends StatementTree {

  SyntaxToken tryKeyword();

  @Nullable
  SyntaxToken openParenToken();

  /**
   * @since Java 1.7
   */
  ListTree<VariableTree> resources();

  @Nullable
  SyntaxToken closeParenToken();

  BlockTree block();

  List<CatchTree> catches();

  @Nullable
  SyntaxToken finallyKeyword();

  @Nullable
  BlockTree finallyBlock();

}
