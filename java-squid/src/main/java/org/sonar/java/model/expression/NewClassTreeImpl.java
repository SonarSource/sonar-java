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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class NewClassTreeImpl extends AbstractTypedTree implements NewClassTree {

  private ExpressionTree enclosingExpression;
  private TypeTree identifier;
  @Nullable
  private final SyntaxToken openParenToken;
  private final List<ExpressionTree> arguments;
  @Nullable
  private final SyntaxToken closeParenToken;
  @Nullable
  private final ClassTree classBody;

  public NewClassTreeImpl(@Nullable SyntaxToken openParenToken, List arguments, @Nullable SyntaxToken closeParenToken, @Nullable ClassTreeImpl classBody, AstNode... children) {
    super(Kind.NEW_CLASS);
    this.enclosingExpression = null;
    this.openParenToken = openParenToken;
    this.arguments = Preconditions.checkNotNull(arguments);
    this.closeParenToken = closeParenToken;
    this.classBody = classBody;

    for (AstNode child : children) {
      addChild(child);
    }
  }

  public NewClassTreeImpl completeWithIdentifier(TypeTree identifier) {
    this.identifier = identifier;
    return this;
  }

  public NewClassTreeImpl completeWithEnclosingExpression(ExpressionTree enclosingExpression) {
    this.enclosingExpression = enclosingExpression;
    return this;
  }

  @Override
  public Kind getKind() {
    return Kind.NEW_CLASS;
  }

  @Nullable
  @Override
  public ExpressionTree enclosingExpression() {
    return enclosingExpression;
  }

  @Override
  public List<Tree> typeArguments() {
    // TODO implement
    return ImmutableList.of();
  }

  @Override
  public TypeTree identifier() {
    return identifier;
  }

  @Override
  public List<ExpressionTree> arguments() {
    return arguments;
  }

  @Nullable
  @Override
  public ClassTree classBody() {
    return classBody;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitNewClass(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.forArray(
        enclosingExpression,
        identifier
        ),
      arguments.iterator(),
      Iterators.singletonIterator(classBody)
      );
  }

  public IdentifierTree getConstructorIdentifier() {
    return getConstructorIdentifier(identifier());
  }

  private IdentifierTree getConstructorIdentifier(Tree constructorSelect) {
    IdentifierTree constructorIdentifier;
    if (constructorSelect.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) constructorSelect;
      constructorIdentifier = mset.identifier();
    } else if (constructorSelect.is(Tree.Kind.IDENTIFIER)) {
      constructorIdentifier = (IdentifierTree) constructorSelect;
    } else if (constructorSelect.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      constructorIdentifier = getConstructorIdentifier(((ParameterizedTypeTree) constructorSelect).type());
    } else {
      throw new IllegalStateException("Constructor select is not of the expected type " + constructorSelect);
    }
    return constructorIdentifier;
  }

  @Nullable
  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Nullable
  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }
}
