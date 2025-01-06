/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.declaration;

import java.util.Arrays;
import java.util.List;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.AssessableExpressionTree;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

public class AnnotationTreeImpl extends AssessableExpressionTree implements AnnotationTree {

  private final SyntaxToken atToken;
  private final TypeTree annotationType;
  private final Arguments arguments;

  public AnnotationTreeImpl(InternalSyntaxToken atToken, TypeTree annotationType, ArgumentListTreeImpl arguments) {
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
  public List<Tree> children() {
    return Arrays.asList(atToken, annotationType, arguments);
  }

  @Override
  public SyntaxToken atToken() {
    return atToken;
  }
}
