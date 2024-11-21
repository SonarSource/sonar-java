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
package org.sonar.java.model.statement;

import java.util.List;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;

public class ExpressionStatementTreeImpl extends JavaTree implements ExpressionStatementTree {

  private final ExpressionTree expression;
  private final InternalSyntaxToken semicolonToken;

  public ExpressionStatementTreeImpl(ExpressionTree expression, /* FIXME */@Nullable InternalSyntaxToken semicolonToken) {
    this.expression = Objects.requireNonNull(expression);
    this.semicolonToken = semicolonToken;
  }

  @Override
  public Kind kind() {
    return Kind.EXPRESSION_STATEMENT;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  // FIXME There isn't always a semicolon, for example within "for" initializers
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitExpressionStatement(this);
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Collections.singletonList(expression),
      semicolonToken != null ? Collections.singletonList(semicolonToken) : Collections.<Tree>emptyList());
  }

}
