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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;

import java.util.Iterator;

public class MemberSelectExpressionTreeImpl extends AbstractTypedTree implements MemberSelectExpressionTree {

  private ExpressionTree expression;

  @Nullable
  private final ArrayTypeTreeImpl nestedDimensions;
  private InternalSyntaxToken dotToken;
  private final IdentifierTree identifier;

  public MemberSelectExpressionTreeImpl(@Nullable ArrayTypeTreeImpl nestedDimensions, InternalSyntaxToken dotToken, IdentifierTreeImpl identifier) {
    super(Kind.MEMBER_SELECT);

    this.nestedDimensions = nestedDimensions;
    this.dotToken = dotToken;
    this.identifier = identifier;
  }

  public MemberSelectExpressionTreeImpl(ExpressionTree expression, InternalSyntaxToken dotToken, IdentifierTree identifier) {
    super(Kind.MEMBER_SELECT);

    this.nestedDimensions = null;
    this.expression = Preconditions.checkNotNull(expression);
    this.dotToken = dotToken;
    this.identifier = Preconditions.checkNotNull(identifier);
  }

  public MemberSelectExpressionTreeImpl completeWithExpression(ExpressionTree expression) {
    Preconditions.checkState(this.expression == null);
    ExpressionTree result = expression;

    if (nestedDimensions != null) {
      nestedDimensions.setLastChildType((TypeTree) expression);
      result = nestedDimensions;
    }

    this.expression = result;

    return this;
  }

  @Override
  public Kind getKind() {
    return Kind.MEMBER_SELECT;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken operatorToken() {
    return dotToken;
  }

  @Override
  public IdentifierTree identifier() {
    return identifier;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMemberSelectExpression(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.<Tree>forArray(
      expression,
      dotToken,
      identifier);
  }

}
