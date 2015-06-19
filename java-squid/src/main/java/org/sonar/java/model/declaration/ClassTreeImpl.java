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
import com.sonar.sslr.api.AstNode;
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
  @Nullable
  private final SyntaxToken openBraceToken;
  private final List<Tree> members;
  @Nullable
  private final SyntaxToken closeBraceToken;
  private ModifiersTree modifiers;
  private SyntaxToken atToken;
  private SyntaxToken declarationKeyowrd;
  private IdentifierTree simpleName;
  private TypeParameters typeParameters;
  @Nullable
  private SyntaxToken extendsKeyword;
  @Nullable
  private TypeTree superClass;
  @Nullable
  private SyntaxToken implementsKeyowrd;
  private List<TypeTree> superInterfaces;
  private JavaSymbol.TypeJavaSymbol symbol = Symbols.unknownSymbol;

  public ClassTreeImpl(Kind kind, SyntaxToken openBraceToken, List<Tree> members, SyntaxToken closeBraceToken, List<? extends AstNode> children) {
    super(kind);

    this.kind = kind;
    this.openBraceToken = openBraceToken;
    this.members = members;
    this.closeBraceToken = closeBraceToken;
    this.modifiers = ModifiersTreeImpl.EMPTY;
    this.typeParameters = new TypeParameterListTreeImpl();
    this.superInterfaces = ImmutableList.of();

    for (AstNode child : children) {
      addChild(child);
    }
  }

  public ClassTreeImpl(ModifiersTree modifiers, SyntaxToken openBraceToken, List<Tree> members, SyntaxToken closeBraceToken, List<AstNode> children) {
    super(Kind.ANNOTATION_TYPE);
    this.kind = Preconditions.checkNotNull(Kind.ANNOTATION_TYPE);
    this.modifiers = modifiers;
    this.typeParameters = new TypeParameterListTreeImpl();
    this.superClass = null;
    this.superInterfaces = ImmutableList.of();
    this.openBraceToken = openBraceToken;
    this.members = Preconditions.checkNotNull(members);
    this.closeBraceToken = closeBraceToken;

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

  public ClassTreeImpl completeSuperclass(SyntaxToken extendsKeyword, TypeTree superClass) {
    this.extendsKeyword = extendsKeyword;
    this.superClass = superClass;
    return this;
  }

  public ClassTreeImpl completeInterfaces(SyntaxToken implementsKeyword, QualifiedIdentifierListTreeImpl interfaces) {
    this.implementsKeyowrd = implementsKeyword;
    this.superInterfaces = interfaces;
    return this;
  }

  public ClassTreeImpl compleInterfacesForInterface(SyntaxToken extendsKeyword, QualifiedIdentifierListTreeImpl interfaces) {
    this.extendsKeyword = extendsKeyword;
    this.superInterfaces = interfaces;
    return this;
  }

  public ClassTreeImpl complete(InternalSyntaxToken atToken, InternalSyntaxToken interfaceToken, IdentifierTree simpleName) {
    Preconditions.checkState(this.simpleName == null);
    completeIdentifier(simpleName);
    this.atToken = atToken;
    completeDeclarationKeyword(interfaceToken);

    prependChildren(atToken, interfaceToken, (AstNode) simpleName);

    return this;
  }

  public ClassTreeImpl completeDeclarationKeyword(SyntaxToken declarationKeyword) {
    this.declarationKeyowrd = declarationKeyword;
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

  @Nullable
  @Override
  public SyntaxToken openBraceToken() {
    return openBraceToken;
  }

  @Override
  public List<Tree> members() {
    return members;
  }

  @Nullable
  @Override
  public SyntaxToken closeBraceToken() {
    return closeBraceToken;
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return symbol;
  }

  @Nullable
  @Override
  public SyntaxToken declarationKeyword() {
    return declarationKeyowrd;
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
    ImmutableList.Builder<Tree> iteratorBuilder = ImmutableList.<Tree>builder();

    iteratorBuilder.add(modifiers);
    addIfNotNull(iteratorBuilder, atToken);
    addIfNotNull(iteratorBuilder, declarationKeyowrd);
    addIfNotNull(iteratorBuilder, simpleName);
    iteratorBuilder.add(typeParameters);
    addIfNotNull(iteratorBuilder, extendsKeyword);
    addIfNotNull(iteratorBuilder, superClass);
    addIfNotNull(iteratorBuilder, implementsKeyowrd);
    iteratorBuilder.addAll(superInterfaces);
    addIfNotNull(iteratorBuilder, openBraceToken);
    iteratorBuilder.addAll(members);
    addIfNotNull(iteratorBuilder, closeBraceToken);

    return iteratorBuilder.build().iterator();
  }

  private static ImmutableList.Builder<Tree> addIfNotNull(ImmutableList.Builder<Tree> builder, Tree tree) {
    if (tree != null) {
      builder.add(tree);
    }
    return builder;
  }

}
