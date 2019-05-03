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
package org.sonar.java.model.expression;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Objects;

public class MethodInvocationTreeImpl extends AbstractTypedTree implements MethodInvocationTree {

  private final ExpressionTree methodSelect;
  private final Arguments arguments;
  @Nullable
  private TypeArguments typeArguments;
  private Symbol symbol = Symbols.unknownSymbol;

  public MethodInvocationTreeImpl(ExpressionTree methodSelect, @Nullable TypeArguments typeArguments, ArgumentListTreeImpl arguments) {
    super(Kind.METHOD_INVOCATION);
    this.methodSelect = Objects.requireNonNull(methodSelect);
    this.typeArguments = typeArguments;
    this.arguments = Objects.requireNonNull(arguments);
  }

  @Override
  public Kind kind() {
    return Kind.METHOD_INVOCATION;
  }

  @Nullable
  @Override
  public TypeArguments typeArguments() {
    return typeArguments;
  }

  @Override
  public SyntaxToken firstToken() {
    if (typeArguments() != null && methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      SyntaxToken firstToken = expression.firstToken();
      if (firstToken != null) {
        return firstToken;
      }
    }
    return super.firstToken();
  }

  @Override
  public ExpressionTree methodSelect() {
    return methodSelect;
  }

  @Override
  public Arguments arguments() {
    return arguments;
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
  public Iterable<Tree> children() {
    return Iterables.concat(
      typeArguments != null ? Collections.singletonList(typeArguments) : Collections.<Tree>emptyList(),
      Lists.newArrayList(methodSelect, arguments));
  }

  public void setSymbol(Symbol symbol) {
    Preconditions.checkState(this.symbol.isUnknown());
    this.symbol = symbol;
  }
}
