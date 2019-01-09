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
package org.sonar.java.model.declaration;

import com.google.common.collect.Lists;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

public class AnnotationTreeImpl extends AbstractTypedTree implements AnnotationTree {

  private final SyntaxToken atToken;
  private final TypeTree annotationType;
  private final Arguments arguments;

  public AnnotationTreeImpl(InternalSyntaxToken atToken, TypeTree annotationType, ArgumentListTreeImpl arguments) {
    super(Kind.ANNOTATION);
    this.atToken = atToken;
    this.annotationType = annotationType;
    this.arguments = arguments;
  }

  @Override
  public TypeTree annotationType() {
    return annotationType;
  }

  @Override
  public Arguments arguments() {
    return arguments;
  }

  @Override
  public Kind kind() {
    return Kind.ANNOTATION;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitAnnotation(this);
  }

  @Override
  public Iterable<Tree> children() {
    return Lists.newArrayList(atToken, annotationType, arguments);
  }

  @Override
  public SyntaxToken atToken() {
    return atToken;
  }
}
