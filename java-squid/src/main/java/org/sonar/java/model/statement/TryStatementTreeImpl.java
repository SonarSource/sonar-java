/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model.statement;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class TryStatementTreeImpl extends JavaTree implements TryStatementTree {
  private final List<VariableTree> resources;
  private final BlockTree block;
  private final List<CatchTree> catches;
  @Nullable
  private final BlockTree finallyBlock;

  public TryStatementTreeImpl(AstNode astNode, List<VariableTree> resources, BlockTree block, List<CatchTree> catches, @Nullable BlockTree finallyBlock) {
    super(astNode);
    this.resources = Preconditions.checkNotNull(resources);
    this.block = Preconditions.checkNotNull(block);
    this.catches = Preconditions.checkNotNull(catches);
    this.finallyBlock = finallyBlock;
  }

  @Override
  public Kind getKind() {
    return Kind.TRY_STATEMENT;
  }

  @Override
  public SyntaxToken tryKeyword() {
    if (!resources.isEmpty()) {
      return new InternalSyntaxToken(astNode.getFirstChild(JavaGrammar.TRY_WITH_RESOURCES_STATEMENT).getFirstChild(JavaKeyword.TRY).getToken());
    } else {
      return new InternalSyntaxToken(astNode.getFirstChild(JavaKeyword.TRY).getToken());
    }
  }

  @Nullable
  @Override
  public SyntaxToken openParenToken() {
    if (!resources.isEmpty()) {
      return new InternalSyntaxToken(astNode
        .getFirstChild(JavaGrammar.RESOURCE_SPECIFICATION)
        .getFirstChild(JavaPunctuator.LPAR)
        .getToken());
    } else {
      return null;
    }
  }

  @Override
  public List<VariableTree> resources() {
    return resources;
  }

  @Nullable
  @Override
  public SyntaxToken closeParenToken() {
    if (!resources.isEmpty()) {
      return new InternalSyntaxToken(astNode
        .getFirstChild(JavaGrammar.RESOURCE_SPECIFICATION)
        .getFirstChild(JavaPunctuator.RPAR)
        .getToken());
    } else {
      return null;
    }
  }

  @Override
  public BlockTree block() {
    return block;
  }

  @Override
  public List<CatchTree> catches() {
    return catches;
  }

  @Nullable
  @Override
  public SyntaxToken finallyKeyword() {
    AstNode node = astNode.getFirstChild(JavaGrammar.FINALLY_);
    return node == null ? null : new InternalSyntaxToken(node.getFirstChild(JavaKeyword.FINALLY).getToken());
  }

  @Nullable
  @Override
  public BlockTree finallyBlock() {
    return finallyBlock;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTryStatement(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      resources.iterator(),
      Iterators.singletonIterator(block),
      catches.iterator(),
      Iterators.singletonIterator(finallyBlock)
    );
  }
}
