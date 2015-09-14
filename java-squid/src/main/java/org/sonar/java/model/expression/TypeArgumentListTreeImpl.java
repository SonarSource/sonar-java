/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.ast.parser.ListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;

import java.util.Iterator;
import java.util.List;

public class TypeArgumentListTreeImpl extends ListTreeImpl<Tree> implements TypeArguments {

  private final InternalSyntaxToken openBracketToken;
  private final InternalSyntaxToken closeBracketToken;

  public TypeArgumentListTreeImpl(InternalSyntaxToken openBracketToken, List<Tree> expressions, List<SyntaxToken> separators, InternalSyntaxToken closeBracketToken) {
    super(JavaLexer.TYPE_ARGUMENTS, expressions, separators);

    this.openBracketToken = openBracketToken;
    this.closeBracketToken = closeBracketToken;
  }

  @Override
  public SyntaxToken openBracketToken() {
    return openBracketToken;
  }

  @Override
  public SyntaxToken closeBracketToken() {
    return closeBracketToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTypeArguments(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
        Iterators.singletonIterator(openBracketToken),
        super.childrenIterator(),
        Iterators.singletonIterator(closeBracketToken));
  }

  @Override
  public boolean isLeaf() {
    return false;
  }

  @Override
  public Kind getKind() {
    return Kind.TYPE_ARGUMENTS;
  }
}
