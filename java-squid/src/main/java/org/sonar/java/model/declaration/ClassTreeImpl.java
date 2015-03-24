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
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;

public class ClassTreeImpl extends JavaTree implements ClassTree {

  private final Kind kind;
  private final List<Tree> members;
  private ModifiersTree modifiers;
  private IdentifierTree simpleName;
  private TypeParameters typeParameters;
  @Nullable
  private TypeTree superClass;
  private List<TypeTree> superInterfaces;
  private JavaSymbol.TypeJavaSymbol symbol = Symbols.unknownSymbol;

  public ClassTreeImpl(Kind kind, List<Tree> members, List<AstNode> children) {
    super(kind);

    this.kind = kind;
    this.members = members;
    this.modifiers = ModifiersTreeImpl.EMPTY;
    this.typeParameters = new TypeParameterListTreeImpl();
    this.superInterfaces = ImmutableList.of();

    for (AstNode child : children) {
      addChild(child);
    }
  }

  public ClassTreeImpl(ModifiersTree modifiers, List<Tree> members, List<AstNode> children) {
    super(Kind.ANNOTATION_TYPE);
    this.kind = Preconditions.checkNotNull(Kind.ANNOTATION_TYPE);
    this.modifiers = modifiers;
    this.typeParameters = new TypeParameterListTreeImpl();
    this.superClass = null;
    this.superInterfaces = ImmutableList.of();
    this.members = Preconditions.checkNotNull(members);

    for (AstNode child : children) {
      addChild(child);
    }
  }

  public ClassTreeImpl completeModifiers(ModifiersTreeImpl modifiers) {
    this.modifiers = modifiers;
    return this;
  }

  public ClassTreeImpl completeIdentifier(IdentifierTree identifier) {
    this.simpleName = identifier;
    return this;
  }

  public ClassTreeImpl completeTypeParameters(TypeParameterListTreeImpl typeParameters) {
    this.typeParameters = typeParameters;
    return this;
  }

  public ClassTreeImpl completeSuperclass(TypeTree superClass) {
    this.superClass = superClass;
    return this;
  }

  public ClassTreeImpl completeInterfaces(QualifiedIdentifierListTreeImpl interfaces) {
    this.superInterfaces = interfaces;
    return this;
  }

  public ClassTreeImpl complete(InternalSyntaxToken atToken, InternalSyntaxToken interfaceToken, IdentifierTree simpleName) {
    Preconditions.checkState(this.simpleName == null);
    this.simpleName = simpleName;

    prependChildren(atToken, interfaceToken, (AstNode) simpleName);

    return this;
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
  public TypeParameters typeParameters() {
    return typeParameters;
  }

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Nullable
  @Override
  public TypeTree superClass() {
    return superClass;
  }

  @Override
  public List<TypeTree> superInterfaces() {
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
    } else if (getAstNode().is(Kind.CLASS, Kind.ENUM, Kind.INTERFACE)) {
      return new InternalSyntaxToken(getAstNode().getFirstChild(leftOrRightBrace).getToken());
    }
    return new InternalSyntaxToken(getAstNode().getFirstChild(JavaLexer.CLASS_BODY, JavaLexer.INTERFACE_BODY, JavaLexer.ENUM_BODY)
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
  public Symbol.TypeSymbol symbol() {
    return symbol;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitClass(this);
  }

  public void setSymbol(JavaSymbol.TypeJavaSymbol symbol) {
    Preconditions.checkState(this.symbol.equals(Symbols.unknownSymbol));
    this.symbol = symbol;
  }

  @Override
  public int getLine() {
    if (simpleName == null) {
      return super.getLine();
    }
    return ((IdentifierTreeImpl) simpleName).getLine();
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    Iterator<TypeParameters> typeParamIterator = Iterators.emptyIterator();
    if (typeParameters != null) {
      typeParamIterator = Iterators.singletonIterator(typeParameters);
    }
    return Iterators.concat(
      Iterators.forArray(
        modifiers,
        simpleName
        ),
      typeParamIterator,
      Iterators.singletonIterator(superClass),
      superInterfaces.iterator(),
      members.iterator()
      );
  }

}
