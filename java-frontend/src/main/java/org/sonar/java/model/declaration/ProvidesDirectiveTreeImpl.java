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
package org.sonar.java.model.declaration;

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.ProvidesDirectiveTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.Arrays;
import java.util.Collections;

public class ProvidesDirectiveTreeImpl extends ModuleDirectiveTreeImpl implements ProvidesDirectiveTree {

  private final TypeTree typeName;
  private final InternalSyntaxToken withKeyword;
  private final ListTree<TypeTree> typeNames;

  public ProvidesDirectiveTreeImpl(InternalSyntaxToken providesKeyword, TypeTree typeName, InternalSyntaxToken withKeyword,
    ListTree<TypeTree> typeNames, InternalSyntaxToken semicolonToken) {
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
  public TypeTree typeName() {
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
    return Collections.unmodifiableList(Arrays.asList(
      directiveKeyword(),
      typeName,
      withKeyword,
      typeNames,
      semicolonToken()));
  }

}
