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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.semantic.Symbol;

/**
 * Lambda expression.
 *
 * For example:
 * <pre>{@code
 *   () -> { }
 *   x -> x + 1
 *   (x, y) -> { return x + y; }
 *   (List<String> ls) -> ls.size()
 * }</pre>
 *
 * @since Java 1.8
 */
@Beta
public interface LambdaExpressionTree extends ExpressionTree {

  @Nullable
  SyntaxToken openParenToken();

  List<VariableTree> parameters();

  @Nullable
  SyntaxToken closeParenToken();

  SyntaxToken arrowToken();

  Tree body();

  Symbol.MethodSymbol symbol();

  /**
   * Compute a CFG for the body of the lambda.
   *
   * @return the CFG corresponding to the expression or the body of the lambda.
   */
  ControlFlowGraph cfg();
}
