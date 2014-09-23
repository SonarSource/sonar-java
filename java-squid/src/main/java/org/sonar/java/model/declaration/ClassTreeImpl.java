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
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameterTree;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;

public class ClassTreeImpl extends JavaTree implements ClassTree {

  private final Kind kind;
  private ModifiersTree modifiers;
  private IdentifierTree simpleName;
  private final List<TypeParameterTree> typeParameters;
  @Nullable
  private final Tree superClass;
  private final List<Tree> superInterfaces;
  private final List<Tree> members;

  // FIXME(Godin): never should be null, i.e. should have default value
  @Nullable
  private Symbol.TypeSymbol symbol;

  public ClassTreeImpl(ModifiersTree modifiers, List<Tree> members, List<AstNode> children) {
    super(Kind.ANNOTATION_TYPE);
    this.kind = Preconditions.checkNotNull(Kind.ANNOTATION_TYPE);
    this.modifiers = modifiers;
    this.typeParameters = ImmutableList.<TypeParameterTree>of();
    this.superClass = null;
    this.superInterfaces = ImmutableList.<Tree>of();
    this.members = Preconditions.checkNotNull(members);

    for (AstNode child : children) {
      addChild(child);
    }
  }

  public ClassTreeImpl(
    AstNode astNode, Kind kind,
    ModifiersTree modifiers, @Nullable IdentifierTree simpleName, List<TypeParameterTree> typeParameters, @Nullable Tree superClass, List<Tree> superInterfaces, List<Tree> members) {
    super(astNode);
    this.kind = Preconditions.checkNotNull(kind);
    this.modifiers = Preconditions.checkNotNull(modifiers);
    this.simpleName = simpleName;
    this.typeParameters = typeParameters;
    this.superClass = superClass;
    this.superInterfaces = Preconditions.checkNotNull(superInterfaces);
    this.members = Preconditions.checkNotNull(members);
  }

  // TODO remove:
  public ClassTreeImpl(AstNode astNode, Kind kind, ModifiersTree modifiers, List<Tree> members) {
    this(astNode, kind, modifiers, null, ImmutableList.<TypeParameterTree>of(), null, ImmutableList.<Tree>of(), members);
  }

  public ClassTreeImpl complete(InternalSyntaxToken atToken, InternalSyntaxToken interfaceToken, IdentifierTree simpleName) {
    Preconditions.checkState(this.simpleName == null);
    this.simpleName = simpleName;

    prependChildren(atToken, interfaceToken, (AstNode) simpleName);

    return this;
  }

  // FIXME Remove
  public void setModifiers(ModifiersTree modifiers) {
    this.modifiers = modifiers;
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
  public List<TypeParameterTree> typeParameters() {
    return typeParameters;
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
  public SyntaxToken openBraceToken() {
    return getBrace(JavaPunctuator.LWING);
  }

  @Nullable
  private SyntaxToken getBrace(JavaPunctuator leftOrRightBrace) {
    if (is(Kind.ANNOTATION_TYPE)) {
      return new InternalSyntaxToken(getAstNode().getFirstChild(leftOrRightBrace).getToken());
    } else if (getAstNode().is(JavaGrammar.CLASS_BODY)) {
      // Enum constant body
      return new InternalSyntaxToken(getAstNode().getFirstChild(leftOrRightBrace).getToken());
    }
    return new InternalSyntaxToken(getAstNode().getFirstChild(JavaGrammar.CLASS_BODY, JavaGrammar.INTERFACE_BODY, JavaGrammar.ENUM_BODY)
      .getFirstChild(leftOrRightBrace).getToken());
  }

  @Override
  public List<Tree> members() {
    return members;
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return getBrace(JavaPunctuator.RWING);
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
        simpleName
        ),
      typeParameters.iterator(),
      Iterators.singletonIterator(superClass),
      superInterfaces.iterator(),
      members.iterator()
      );
  }

}
