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
package org.sonar.java.model.expression;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;

public class MethodInvocationTreeImpl extends AbstractTypedTree implements MethodInvocationTree {

  private final ExpressionTree methodSelect;
  private final List<ExpressionTree> arguments;
  @Nullable
  private TypeArguments typeArguments;
  private Symbol symbol = Symbols.unknownSymbol;
  private SyntaxToken openParenToken;
  private SyntaxToken closeParenToken;

  public MethodInvocationTreeImpl(ExpressionTree methodSelect, @Nullable TypeArguments typeArguments,
    SyntaxToken openParenToken, List<ExpressionTree> arguments, SyntaxToken closeParenToken,
    AstNode... children) {
    super(Kind.METHOD_INVOCATION);
    this.methodSelect = Preconditions.checkNotNull(methodSelect);
    this.typeArguments = typeArguments;
    this.arguments = Preconditions.checkNotNull(arguments);
    this.openParenToken = openParenToken;
    this.closeParenToken = closeParenToken;

    for (AstNode child : children) {
      addChild(child);
    }
  }

  @Override
  public Kind getKind() {
    return Kind.METHOD_INVOCATION;
  }

  @Override
  public TypeArguments typeArguments() {
    return typeArguments;
  }

  @Override
  public ExpressionTree methodSelect() {
    return methodSelect;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public List<ExpressionTree> arguments() {
    return arguments;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public Symbol symbol() {
    return symbol;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethodInvocation(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.singletonIterator(methodSelect),
      Iterators.singletonIterator(typeArguments),
      arguments.iterator()
      );
  }

  public void setSymbol(Symbol symbol) {
    Preconditions.checkState(this.symbol.equals(Symbols.unknownSymbol));
    this.symbol = symbol;
  }
}
