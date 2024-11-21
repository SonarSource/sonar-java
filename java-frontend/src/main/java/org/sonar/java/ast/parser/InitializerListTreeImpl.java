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
package org.sonar.java.ast.parser;

import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;

import java.util.ArrayList;
import java.util.List;

public class InitializerListTreeImpl extends ListTreeImpl<ExpressionTree> {
  private InitializerListTreeImpl(List<ExpressionTree> list, List<SyntaxToken> separators) {
    super(list, separators);
  }

  public static InitializerListTreeImpl emptyList() {
    return new InitializerListTreeImpl(new ArrayList<>(), new ArrayList<>());
  }
}
