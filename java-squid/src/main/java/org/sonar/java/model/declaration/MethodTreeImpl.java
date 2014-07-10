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
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class MethodTreeImpl extends JavaTree implements MethodTree {
  private final ModifiersTree modifiers;
  @Nullable
  private final Tree returnType;
  private final IdentifierTree simpleName;
  private final List<VariableTree> parameters;
  @Nullable
  private final BlockTree block;
  private final List<ExpressionTree> throwsClauses;
  private final ExpressionTree defaultValue;

  private Symbol.MethodSymbol symbol;

  public MethodTreeImpl(AstNode astNode, ModifiersTree modifiers, @Nullable Tree returnType, IdentifierTree simpleName, List<VariableTree> parameters,
                        @Nullable BlockTree block,
                        List<ExpressionTree> throwsClauses, @Nullable ExpressionTree defaultValue) {
    super(astNode);
    this.modifiers = Preconditions.checkNotNull(modifiers);
    this.returnType = returnType;
    this.simpleName = Preconditions.checkNotNull(simpleName);
    this.parameters = Preconditions.checkNotNull(parameters);
    this.block = block;
    this.throwsClauses = Preconditions.checkNotNull(throwsClauses);
    this.defaultValue = defaultValue;
  }

  @Override
  public Kind getKind() {
    return returnType == null ? Kind.CONSTRUCTOR : Kind.METHOD;
  }

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Override
  public List<Tree> typeParameters() {
    // TODO implement
    return ImmutableList.of();
  }

  @Nullable
  @Override
  public Tree returnType() {
    return returnType;
  }

  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Override
  public List<VariableTree> parameters() {
    return parameters;
  }

  @Override
  public List<ExpressionTree> throwsClauses() {
    return throwsClauses;
  }

  @Nullable
  @Override
  public BlockTree block() {
    return block;
  }

  @Nullable
  @Override
  public ExpressionTree defaultValue() {
    return defaultValue;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethod(this);
  }

  public Symbol.MethodSymbol getSymbol() {
    return symbol;
  }

  public void setSymbol(Symbol.MethodSymbol symbol) {
    Preconditions.checkState(this.symbol == null);
    this.symbol = symbol;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.forArray(
        modifiers,
        returnType,
        simpleName
      ),
      parameters.iterator(),
      Iterators.singletonIterator(block),
      throwsClauses.iterator(),
      Iterators.singletonIterator(defaultValue)
    );
  }
}
