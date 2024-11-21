/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.List;
import java.util.Objects;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class BinaryExpressionTreeImpl extends AssessableExpressionTree implements BinaryExpressionTree {

  private final Kind kind;

  private final ExpressionTree leftOperand;
  private final InternalSyntaxToken operator;
  private final ExpressionTree rightOperand;

  public BinaryExpressionTreeImpl(Kind kind, ExpressionTree leftOperand, InternalSyntaxToken operator, ExpressionTree rightOperand) {
    this.kind = Objects.requireNonNull(kind);
    this.leftOperand = Objects.requireNonNull(leftOperand);
    this.operator = operator;
    this.rightOperand = Objects.requireNonNull(rightOperand);
  }

  @Override
  public ExpressionTree leftOperand() {
    return leftOperand;
  }

  @Override
  public SyntaxToken operatorToken() {
    return operator;
  }

  @Override
  public ExpressionTree rightOperand() {
    return rightOperand;
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitBinaryExpression(this);
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(
      leftOperand,
      operator,
      rightOperand);
  }

}
