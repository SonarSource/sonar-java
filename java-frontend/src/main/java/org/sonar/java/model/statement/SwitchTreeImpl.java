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
package org.sonar.java.model.statement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.AssessableExpressionTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class SwitchTreeImpl extends AssessableExpressionTree implements SwitchTree {

  private final InternalSyntaxToken switchKeyword;
  private final InternalSyntaxToken openParenToken;
  private final ExpressionTree expression;
  private final InternalSyntaxToken closeParenToken;
  private final InternalSyntaxToken openBraceToken;
  private final List<CaseGroupTree> cases;
  private final InternalSyntaxToken closeBraceToken;

  protected SwitchTreeImpl(InternalSyntaxToken switchKeyword, InternalSyntaxToken openParenToken, ExpressionTree expression,
    InternalSyntaxToken closeParenToken, InternalSyntaxToken openBraceToken, List<CaseGroupTreeImpl> groups, InternalSyntaxToken closeBraceToken) {
    this.switchKeyword = switchKeyword;
    this.openParenToken = openParenToken;
    this.expression = Objects.requireNonNull(expression);
    this.closeParenToken = closeParenToken;
    this.openBraceToken = openBraceToken;
    this.cases = Collections.unmodifiableList(Objects.requireNonNull(groups));
    this.closeBraceToken = closeBraceToken;
  }

  @Override
  public SyntaxToken switchKeyword() {
    return switchKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public SyntaxToken openBraceToken() {
    return openBraceToken;
  }

  @Override
  public List<CaseGroupTree> cases() {
    return cases;
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return closeBraceToken;
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Arrays.asList(switchKeyword, openParenToken, expression, closeParenToken, openBraceToken),
      cases,
      Collections.singletonList(closeBraceToken));
  }

}
