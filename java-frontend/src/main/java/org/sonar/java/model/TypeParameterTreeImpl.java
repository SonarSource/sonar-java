/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.model;

import org.sonar.java.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.ast.parser.BoundListTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameterTree;

import javax.annotation.Nullable;

public class TypeParameterTreeImpl extends JavaTree implements TypeParameterTree {

  private IdentifierTreeImpl identifier;
  @Nullable
  private final SyntaxToken extendsToken;
  private final BoundListTreeImpl bounds;

  @Nullable
  ITypeBinding typeBinding;

  public TypeParameterTreeImpl(IdentifierTreeImpl identifier) {
    this.identifier = identifier;
    this.extendsToken = null;
    this.bounds = BoundListTreeImpl.emptyList();
  }

  public TypeParameterTreeImpl(InternalSyntaxToken extendsToken, BoundListTreeImpl bounds) {
    this.extendsToken = extendsToken;
    this.bounds = bounds;
  }

  public TypeParameterTreeImpl complete(IdentifierTreeImpl identifier) {
    Preconditions.checkState(this.identifier == null);
    this.identifier = identifier;
    return this;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTypeParameter(this);
  }

  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @Nullable
  @Override
  public SyntaxToken extendToken() {
    return extendsToken;
  }

  @Override
  public ListTree<Tree> bounds() {
    return bounds;
  }

  @Override
  public Kind kind() {
    return Kind.TYPE_PARAMETER;
  }

  @Override
  public List<Tree> children() {
    List<Tree> list = new ArrayList<>();
    list.add(identifier);
    if (extendsToken != null) {
      list.add(extendsToken);
      list.add(bounds);
    }
    return Collections.unmodifiableList(list);
  }

  public Symbol symbol() {
    return typeBinding != null
      ? root.sema.typeSymbol(typeBinding)
      : Symbols.unknownSymbol;
  }
}
