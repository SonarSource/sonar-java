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

import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Iterator;
import java.util.List;

public class AnnotationTreeImpl extends AbstractTypedTree implements AnnotationTree {

  private final List<ExpressionTree> arguments;
  private final Tree annotationType;

  public AnnotationTreeImpl(AstNode astNode, Tree annotationType, List<ExpressionTree> arguments) {
    super(astNode);
    this.annotationType = annotationType;
    this.arguments = arguments;
  }

  @Override
  public Tree annotationType() {
    return annotationType;
  }

  @Override
  public List<ExpressionTree> arguments() {
    return arguments;
  }

  @Override
  public Kind getKind() {
    return Kind.ANNOTATION;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitAnnotation(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
      arguments.iterator(),
      Iterators.singletonIterator(annotationType)
    );
  }
}
