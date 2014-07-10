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
package org.sonar.java.model.declaration;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class ClassTreeImpl extends JavaTree implements ClassTree {
  private final Kind kind;
  private final ModifiersTree modifiers;
  private final IdentifierTree simpleName;
  @Nullable
  private final Tree superClass;
  private final List<Tree> superInterfaces;
  private final List<Tree> members;

  // FIXME(Godin): never should be null, i.e. should have default value
  @Nullable
  private Symbol.TypeSymbol symbol;

  public ClassTreeImpl(AstNode astNode, Kind kind, ModifiersTree modifiers, @Nullable IdentifierTree simpleName, @Nullable Tree superClass, List<Tree> superInterfaces,
                       List<Tree> members) {
    super(astNode);
    this.kind = Preconditions.checkNotNull(kind);
    this.modifiers = Preconditions.checkNotNull(modifiers);
    this.simpleName = simpleName;
    this.superClass = superClass;
    this.superInterfaces = Preconditions.checkNotNull(superInterfaces);
    this.members = Preconditions.checkNotNull(members);
  }

  // TODO remove:
  public ClassTreeImpl(AstNode astNode, Kind kind, ModifiersTree modifiers, List<Tree> members) {
    this(astNode, kind, modifiers, null, null, ImmutableList.<Tree>of(), members);
  }

  @Override
  public Kind getKind() {
    return kind;
  }

  @Nullable
  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Override
  public List<Tree> typeParameters() {
    // TODO implement
    return ImmutableList.of();
  }

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Nullable
  @Override
  public Tree superClass() {
    return superClass;
  }

  @Override
  public List<Tree> superInterfaces() {
    return superInterfaces;
  }

  @Override
  public List<Tree> members() {
    return members;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitClass(this);
  }

  @Nullable
  public Symbol.TypeSymbol getSymbol() {
    return symbol;
  }

  public void setSymbol(Symbol.TypeSymbol symbol) {
    Preconditions.checkState(this.symbol == null);
    this.symbol = symbol;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.forArray(
        modifiers,
        simpleName,
        superClass
      ),
      superInterfaces.iterator(),
      members.iterator()
    );
  }
}
