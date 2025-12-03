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
package org.sonar.java.model.expression;

import java.util.Arrays;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.sonar.java.cfg.CFG;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class LambdaExpressionTreeImpl extends AssessableExpressionTree implements LambdaExpressionTree {

  @Nullable
  private final InternalSyntaxToken openParenToken;
  private final List<VariableTree> parameters;
  @Nullable
  private final InternalSyntaxToken closeParenToken;
  private final InternalSyntaxToken arrowToken;
  private final Tree body;
  private CFG cfg;

  @Nullable
  public IMethodBinding methodBinding;

  public LambdaExpressionTreeImpl(@Nullable InternalSyntaxToken openParenToken, List<VariableTree> parameters, @Nullable InternalSyntaxToken closeParenToken,
    InternalSyntaxToken arrowToken, Tree body) {
    this.openParenToken = openParenToken;
    this.parameters = parameters;
    this.closeParenToken = closeParenToken;
    this.arrowToken = arrowToken;
    this.body = body;
  }

  @Override
  public Kind kind() {
    return Kind.LAMBDA_EXPRESSION;
  }

  @Nullable
  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public List<VariableTree> parameters() {
    return parameters;
  }

  @Nullable
  @Override
  public SyntaxToken closeParenToken() {
    return  closeParenToken;
  }

  @Override
  public SyntaxToken arrowToken() {
    return arrowToken;
  }

  @Override
  public Tree body() {
    return body;
  }

  @Override
  public Symbol.MethodSymbol symbol() {
    return methodBinding != null
      ? root.sema.methodSymbol(methodBinding)
      : Symbols.unknownMethodSymbol;
  }

  @Override
  public ControlFlowGraph cfg() {
    if (cfg == null) {
      cfg = CFG.buildCFG(body.is(Tree.Kind.BLOCK) ? ((BlockTree) body).body() : Collections.singletonList(body));
    }
    return cfg;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitLambdaExpression(this);
  }

  @Override
  public List<Tree> children() {
    boolean hasParentheses = openParenToken != null;
    return ListUtils.concat(
      hasParentheses ? Collections.singletonList(openParenToken) : Collections.<Tree>emptyList(),
      parameters,
      hasParentheses ? Collections.singletonList(closeParenToken) : Collections.<Tree>emptyList(),
      Arrays.asList(arrowToken, body)
    );
  }

}
