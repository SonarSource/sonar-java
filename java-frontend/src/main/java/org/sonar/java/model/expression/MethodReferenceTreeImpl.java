/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
import java.util.ArrayList;
import java.util.List;

public class MethodReferenceTreeImpl extends AbstractTypedTree implements MethodReferenceTree {

  private Tree expression;
  private SyntaxToken doubleColon;
  private IdentifierTree method;
  private TypeArguments typeArgument;

  public MethodReferenceTreeImpl(Tree expression, InternalSyntaxToken doubleColon) {
    super(JavaLexer.METHOD_REFERENCE);
    this.expression = expression;
    this.doubleColon = doubleColon;
  }

  public void complete(@Nullable TypeArguments typeArgument, IdentifierTreeImpl method) {
    this.typeArgument = typeArgument;
    this.method = method;
  }

  @Override
  public Kind kind() {
    return Kind.METHOD_REFERENCE;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitMethodReference(this);
  }

  @Override
  public Iterable<Tree> children() {
    List<Tree> res = new ArrayList<>();
    if(typeArgument != null) {
      res.add(typeArgument);
    }
    res.add(expression);
    res.add(doubleColon);
    res.add(method);
    return res;
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
