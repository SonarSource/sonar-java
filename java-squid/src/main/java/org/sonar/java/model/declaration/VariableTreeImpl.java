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
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;

public class VariableTreeImpl extends JavaTree implements VariableTree {
  private ModifiersTree modifiers;
  private TypeTree type;
  private IdentifierTree simpleName;
  @Nullable
  private ExpressionTree initializer;
  @Nullable
  private SyntaxToken endToken;

  // FIXME(Godin): never should be null, i.e. should have default value
  private JavaSymbol.VariableJavaSymbol symbol;

  // Syntax tree holders
  private int dims;
  private boolean vararg = false;

  public VariableTreeImpl(IdentifierTreeImpl simpleName, int dims, List<AstNode> additionalChildren) {
    super(Kind.VARIABLE);

    this.modifiers = ModifiersTreeImpl.emptyModifiers();
    this.simpleName = simpleName;
    this.dims = dims;
    this.initializer = null;

    addChild((AstNode) modifiers);
    addChild(simpleName);
    for (AstNode child : additionalChildren) {
      addChild(child);
    }
  }

  public VariableTreeImpl(InternalSyntaxToken equalToken, ExpressionTree initializer, AstNode... children) {
    super(Kind.VARIABLE);

    this.initializer = initializer;

    for (AstNode child : children) {
      addChild(child);
    }
  }

  public VariableTreeImpl(IdentifierTreeImpl simpleName) {
    this(simpleName, 0, ImmutableList.<AstNode>of());
    this.type = new InferedTypeTree();
  }

  public VariableTreeImpl(Kind kind, ModifiersTree modifiers, IdentifierTree simpleName, @Nullable ExpressionTree initializer) {
    super(kind);
    this.modifiers = Preconditions.checkNotNull(modifiers);
    this.simpleName = Preconditions.checkNotNull(simpleName);
    this.dims = -1;
    this.initializer = initializer;
  }

  public VariableTreeImpl(AstNode astNode, ModifiersTree modifiers, TypeTree type, IdentifierTree simpleName, @Nullable ExpressionTree initializer) {
    super(astNode);
    this.modifiers = Preconditions.checkNotNull(modifiers);
    this.type = Preconditions.checkNotNull(type);
    this.simpleName = Preconditions.checkNotNull(simpleName);
    this.dims = -1;
    this.initializer = initializer;
  }

  public VariableTreeImpl completeType(TypeTree type) {
    TypeTree actualType = type;

    // TODO Remove logic?
    for (int i = isVararg() ? 1 + dims() : dims(); i > 0; i--) {
      actualType = new ArrayTypeTreeImpl(null, actualType);
    }

    this.type = actualType;

    return this;
  }

  public VariableTreeImpl completeModifiers(ModifiersTreeImpl modifiers) {
    this.modifiers = modifiers;

    return this;
  }

  public VariableTreeImpl completeModifiersAndType(ModifiersTreeImpl modifiers, TypeTree type) {
    return completeModifiers(modifiers).
      completeType(type);
  }

  public VariableTreeImpl completeTypeAndInitializer(TypeTree type, ExpressionTree initializer) {
    this.initializer = initializer;

    return completeType(type);
  }

  public VariableTreeImpl completeIdentifierAndDims(IdentifierTreeImpl simpleName, int dims) {
    this.simpleName = simpleName;
    this.dims = dims;

    return this;
  }

  public int dims() {
    return dims;
  }

  public void setVararg(boolean vararg) {
    this.vararg = vararg;
  }

  public boolean isVararg() {
    return vararg;
  }

  @Override
  public Kind getKind() {
    return Kind.VARIABLE;
  }

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Override
  public TypeTree type() {
    return type;
  }

  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Nullable
  @Override
  public ExpressionTree initializer() {
    return initializer;
  }

  @Override
  public org.sonar.plugins.java.api.semantic.Symbol symbol() {
    return symbol;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitVariable(this);
  }

  public JavaSymbol.VariableJavaSymbol getSymbol() {
    return symbol;
  }

  public void setSymbol(JavaSymbol.VariableJavaSymbol symbol) {
    Preconditions.checkState(this.symbol == null);
    this.symbol = symbol;
  }

  @Override
  public int getLine() {
    return ((IdentifierTreeImpl) simpleName()).getLine();
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(
      modifiers,
      type,
      simpleName,
      initializer
      );
  }

  @CheckForNull
  @Override
  public SyntaxToken endToken() {
    return endToken;
  }

  public void setEndToken(SyntaxToken endToken) {
    this.endToken = endToken;
  }
}
