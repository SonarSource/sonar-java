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
package org.sonar.plugins.java.api;

import java.util.Objects;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Message and syntaxNode for a secondary location.
 *
 * @param msg        Message of the secondary location.
 * @param syntaxNode Syntax node on which to raise the secondary location.
 */
public record JavaFileLocation(String msg, Tree syntaxNode) {
  public JavaFileLocation(String msg, Tree syntaxNode) {
    this.msg = msg;
    this.syntaxNode = Objects.requireNonNull(syntaxNode);
  }

}
