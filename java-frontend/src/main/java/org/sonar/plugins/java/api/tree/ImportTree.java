/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.plugins.java.api.semantic.Symbol;

/**
 * Import declaration.
 *
 * JLS 7.5
 *
 * <pre>
 *   import {@link #qualifiedIdentifier()} ;
 *   import static {@link #qualifiedIdentifier()} ;
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface ImportTree extends ImportClauseTree {

  /**
   * @since Java 1.5
   */
  boolean isStatic();

  /**
   * @since Java 25
   */
  boolean isModule();

  SyntaxToken importKeyword();

  @Nullable
  SyntaxToken staticKeyword();

  @Nullable
  /**
   * @since Java 25
   */
  SyntaxToken moduleKeyword();

  Tree qualifiedIdentifier();

  SyntaxToken semicolonToken();

  @Nullable
  Symbol symbol();

}
