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

import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;

import javax.annotation.Nullable;

import java.util.Iterator;

public class MethodReferenceTreeImpl extends AbstractTypedTree implements MethodReferenceTree {

  private Tree expression;
  private SyntaxToken doubleColon;
  private IdentifierTree method;
  private TypeArguments typeArgument;

  public MethodReferenceTreeImpl(Tree expression, InternalSyntaxToken doubleColon) {
    super(JavaLexer.METHOD_REFERENCE);
    this.expression = expression;
    this.doubleColon = doubleColon;

    addChild((AstNode) expression);
    addChild(doubleColon);
  }

  public void complete(@Nullable TypeArguments typeArgument, IdentifierTreeImpl method) {
    this.typeArgument = typeArgument;
    this.method = method;

    if (typeArgument != null) {
      addChild((AstNode) typeArgument.openBracketToken());
      for (Tree tree : typeArgument) {
        addChild((AstNode) tree);
      }
      addChild((AstNode) typeArgument.closeBracketToken());
    }
    addChild(method);
  }

  @Override
  public Kind getKind() {
    return Kind.METHOD_REFERENCE;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethodReference(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
        Iterators.singletonIterator(expression),
        Iterators.singletonIterator(method));
  }

  @Override
  public Tree expression() {
    return expression;
  }

  @Override
  public SyntaxToken doubleColon() {
    return doubleColon;
  }

  @Override
  public IdentifierTree method() {
    return method;
  }

  @Nullable
  @Override
  public TypeArguments typeArguments() {
    return typeArgument;
  }
}
