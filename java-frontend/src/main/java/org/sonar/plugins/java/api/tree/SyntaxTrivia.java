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
import org.sonar.plugins.java.api.location.Range;

/**
 * Represents a Trivia in the SyntaxTree.
 *
 * @since plugin 2.5
 */
@Beta
public interface SyntaxTrivia extends Tree {

  String comment();

  /**
   * @deprecated for removal, since = 7.3, use range().start().line()
   */
  @Deprecated(since = "7.3", forRemoval = true)
  int startLine();

  /**
   * Warning: this is not the column number starting at 1 but the column offset starting at 0
   * @return column offset starting at 0
   * @deprecated for removal, since = 7.3, "column()" can be replaced by range().start().columnOffset()
   * and "column() + 1" can be replaced by range().start().column()
   */
  @Deprecated(since = "7.3", forRemoval = true)
  int column();

  Range range();

}
