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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;

public class ReturnStatementTreeImpl extends JavaTree implements ReturnStatementTree {
  private final InternalSyntaxToken returnKeyword;
  @Nullable
  private final ExpressionTree expression;
  private final InternalSyntaxToken semicolonToken;

  public ReturnStatementTreeImpl(InternalSyntaxToken returnKeyword, @Nullable ExpressionTree expression, InternalSyntaxToken semicolonToken) {
    this.returnKeyword = returnKeyword;
    this.expression = expression;
    this.semicolonToken = semicolonToken;
  }

  @Override
  public Kind kind() {
    return Kind.RETURN_STATEMENT;
  }

  @Override
  public SyntaxToken returnKeyword() {
    return returnKeyword;
  }

  @Nullable
  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitReturnStatement(this);
  }

  @Override
  public List<Tree> children() {
    List<Tree> list = new ArrayList<>();
    list.add(returnKeyword);
    if (expression != null) {
      list.add(expression);
    }
    list.add(semicolonToken);
    return Collections.unmodifiableList(list);
  }

}
