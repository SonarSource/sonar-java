/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.model.declaration;

import com.google.common.collect.ImmutableList;

import org.sonar.java.ast.parser.ListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.ProvidesDirectiveTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

public class ProvidesDirectiveTreeImpl extends ModuleDirectiveTreeImpl implements ProvidesDirectiveTree {

  private final ExpressionTree typeName;
  private final InternalSyntaxToken withKeyword;
  private final ListTreeImpl<TypeTree> typeNames;

  public ProvidesDirectiveTreeImpl(InternalSyntaxToken providesKeyword, ExpressionTree typeName, InternalSyntaxToken withKeyword,
    ListTreeImpl<TypeTree> typeNames, InternalSyntaxToken semicolonToken) {
    super(Tree.Kind.PROVIDES_DIRECTIVE, providesKeyword, semicolonToken);
    this.typeName =typeName;
    this.withKeyword = withKeyword;
    this.typeNames = typeNames;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitProvidesDirective(this);
  }

  @Override
  public Kind kind() {
    return Tree.Kind.PROVIDES_DIRECTIVE;
  }

  @Override
  public ExpressionTree typeName() {
    return typeName;
  }

  @Override
  public SyntaxToken withKeyword() {
    return withKeyword;
  }

  @Override
  public ListTree<TypeTree> typeNames() {
    return typeNames;
  }

  @Override
  protected Iterable<Tree> children() {
    ImmutableList.Builder<Tree> iteratorBuilder = ImmutableList.<Tree>builder();
    iteratorBuilder.add(directiveKeyword(), typeName, withKeyword);
    iteratorBuilder.addAll(typeNames.children());
    iteratorBuilder.add(semicolonToken());
    return iteratorBuilder.build();
  }

}
