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
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Iterator;
import java.util.List;

public class BlockTreeImpl extends JavaTree implements BlockTree {
  private final Kind kind;
  private final List<StatementTree> body;

  public BlockTreeImpl(AstNode astNode, Kind kind, List<StatementTree> body) {
    super(astNode);
    this.kind = kind;
    this.body = Preconditions.checkNotNull(body);
  }

  @Override
  public Kind getKind() {
    return kind;
  }

  @Override
  public SyntaxToken openBraceToken() {
    return new InternalSyntaxToken(astNode.getFirstChild(JavaPunctuator.LWING).getToken());
  }

  @Override
  public List<StatementTree> body() {
    return body;
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return new InternalSyntaxToken(astNode.getFirstChild(JavaPunctuator.RWING).getToken());
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitBlock(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      // (Godin): workaround for generics
      Iterators.<Tree>emptyIterator(),
      body.iterator()
    );
  }
}
