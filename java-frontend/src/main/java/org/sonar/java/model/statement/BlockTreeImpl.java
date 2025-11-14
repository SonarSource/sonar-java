/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BlockTreeImpl extends JavaTree implements BlockTree {

  private final Kind kind;
  private final InternalSyntaxToken openBraceToken;
  private final List<StatementTree> body;
  private final InternalSyntaxToken closeBraceToken;

  public BlockTreeImpl(InternalSyntaxToken openBraceToken, List<StatementTree> body, InternalSyntaxToken closeBraceToken) {
    this(Kind.BLOCK, openBraceToken, body, closeBraceToken);
  }

  public BlockTreeImpl(Kind kind, InternalSyntaxToken openBraceToken, List<StatementTree> body, InternalSyntaxToken closeBraceToken) {
    this.kind = kind;
    this.openBraceToken = openBraceToken;
    this.body = Objects.requireNonNull(body);
    this.closeBraceToken = closeBraceToken;
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Override
  public SyntaxToken openBraceToken() {
    return openBraceToken;
  }

  @Override
  public List<StatementTree> body() {
    return body;
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return closeBraceToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitBlock(this);
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Collections.singletonList(openBraceToken),
      body,
      Collections.singletonList(closeBraceToken));
  }

}
