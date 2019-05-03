/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model.statement;

import com.google.common.collect.Lists;
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
    super(Kind.CATCH);

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
  public Iterable<Tree> children() {
    return Lists.newArrayList(
      catchToken,
      openParenToken,
      parameter,
      closeParenToken,
      block
    );
  }

}
