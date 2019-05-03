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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
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
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameters;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ClassTreeImpl extends JavaTree implements ClassTree {

  private final Kind kind;
  private final SyntaxToken openBraceToken;
  private final List<Tree> members;
  private final SyntaxToken closeBraceToken;
  private ModifiersTree modifiers;
  private SyntaxToken atToken;
  private SyntaxToken declarationKeyword;
  private IdentifierTree simpleName;
  private TypeParameters typeParameters;
  @Nullable
  private SyntaxToken extendsKeyword;
  @Nullable
  private TypeTree superClass;
  @Nullable
  private SyntaxToken implementsKeyword;
  private ListTree<TypeTree> superInterfaces;
  private JavaSymbol.TypeJavaSymbol symbol = Symbols.unknownSymbol;

  public ClassTreeImpl(Kind kind, SyntaxToken openBraceToken, List<Tree> members, SyntaxToken closeBraceToken) {
    super(kind);

    this.kind = kind;
    this.openBraceToken = openBraceToken;
    this.members = members;
    this.closeBraceToken = closeBraceToken;
    this.modifiers = ModifiersTreeImpl.emptyModifiers();
    this.typeParameters = new TypeParameterListTreeImpl();
    this.superInterfaces = QualifiedIdentifierListTreeImpl.emptyList();
  }

  public ClassTreeImpl(ModifiersTree modifiers, SyntaxToken openBraceToken, List<Tree> members, SyntaxToken closeBraceToken) {
    super(Kind.ANNOTATION_TYPE);
    this.kind = Objects.requireNonNull(Kind.ANNOTATION_TYPE);
    this.modifiers = modifiers;
    this.typeParameters = new TypeParameterListTreeImpl();
    this.superClass = null;
    this.superInterfaces = QualifiedIdentifierListTreeImpl.emptyList();
    this.openBraceToken = openBraceToken;
    this.members = Objects.requireNonNull(members);
    this.closeBraceToken = closeBraceToken;
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

  public ClassTreeImpl completeInterfaces(SyntaxToken keyword, QualifiedIdentifierListTreeImpl interfaces) {
    if (is(Kind.INTERFACE)) {
      extendsKeyword = keyword;
    } else {
      implementsKeyword = keyword;
    }
    this.superInterfaces = interfaces;
    return this;
  }

  public ClassTreeImpl complete(InternalSyntaxToken atToken, InternalSyntaxToken interfaceToken, IdentifierTree simpleName) {
    Preconditions.checkState(this.simpleName == null);
    completeIdentifier(simpleName);
    this.atToken = atToken;
    completeDeclarationKeyword(interfaceToken);
    return this;
  }

  public ClassTreeImpl completeDeclarationKeyword(SyntaxToken declarationKeyword) {
    this.declarationKeyword = declarationKeyword;
    return this;
  }

  @Override
  public Kind kind() {
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
  public ListTree<TypeTree> superInterfaces() {
    return superInterfaces;
  }

  @Override
  public SyntaxToken openBraceToken() {
    return openBraceToken;
  }

  @Override
  public List<Tree> members() {
    return members;
  }

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
    return declarationKeyword;
  }

  /**
   * Only used for annotation types, not part of API
   */
  @Nullable
  public SyntaxToken atToken() {
    return atToken;
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
  public Iterable<Tree> children() {
    return Iterables.concat(
      Collections.singletonList(modifiers),
      addIfNotNull(atToken),
      addIfNotNull(declarationKeyword),
      addIfNotNull(simpleName),
      Collections.singletonList(typeParameters),
      addIfNotNull(extendsKeyword),
      addIfNotNull(superClass),
      addIfNotNull(implementsKeyword),
      Collections.singletonList(superInterfaces),
      Collections.singletonList(openBraceToken),
      members,
      Collections.singletonList(closeBraceToken)
    );
  }

  private static Iterable<Tree> addIfNotNull(@Nullable Tree tree) {
    if (tree == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(tree);
  }

}
