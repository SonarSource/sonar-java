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
package org.sonar.java.model.expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class MemberSelectExpressionTreeImpl extends AssessableExpressionTree implements MemberSelectExpressionTree, JavaTree.AnnotatedTypeTree {

  private ExpressionTree expression;

  private InternalSyntaxToken dotToken;
  private final IdentifierTree identifier;
  private List<AnnotationTree> annotations;

  public MemberSelectExpressionTreeImpl(ExpressionTree expression, InternalSyntaxToken dotToken, IdentifierTree identifier) {
    this.expression = Objects.requireNonNull(expression);
    this.dotToken = dotToken;
    this.identifier = Objects.requireNonNull(identifier);
    this.annotations = Collections.emptyList();
  }

  @Override
  public void complete(List<AnnotationTree> annotations) {
    this.annotations = annotations;
  }

  @Override
  public Kind kind() {
    return Kind.MEMBER_SELECT;
  }

  @Override
  public List<AnnotationTree> annotations() {
    return annotations;
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
  public List<Tree> children() {
    return ListUtils.concat(
      annotations,
      Arrays.asList(
        expression,
        dotToken,
        identifier));
  }
}
