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
package org.sonar.java.model.statement;

import java.util.Arrays;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Objects;

public class CatchTreeImpl extends JavaTree implements CatchTree {

  private final InternalSyntaxToken catchToken;
  private final InternalSyntaxToken openParenToken;
  private final VariableTree parameter;
  private final BlockTree block;
  private final InternalSyntaxToken closeParenToken;

  public CatchTreeImpl(InternalSyntaxToken catchToken, InternalSyntaxToken openParenToken, VariableTreeImpl parameter, InternalSyntaxToken closeParenToken, BlockTreeImpl block) {
    this.catchToken = catchToken;
    this.openParenToken = openParenToken;
    this.parameter = Objects.requireNonNull(parameter);
    this.closeParenToken = closeParenToken;
    this.block = Objects.requireNonNull(block);
  }

  @Override
  public Kind kind() {
    return Kind.CATCH;
  }

  @Override
  public SyntaxToken catchKeyword() {
    return catchToken;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public VariableTree parameter() {
    return parameter;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public BlockTree block() {
    return block;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitCatch(this);
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(
      catchToken,
      openParenToken,
      parameter,
      closeParenToken,
      block
    );
  }

}
