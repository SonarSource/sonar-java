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
package org.sonar.java.model.expression;

import java.util.Arrays;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

public class InternalPrefixUnaryExpression extends InternalUnaryExpression {

  public InternalPrefixUnaryExpression(Kind kind, InternalSyntaxToken operatorToken, ExpressionTree expression) {
    super(kind, operatorToken, expression);
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(
      operatorToken,
      expression
    );
  }
}
