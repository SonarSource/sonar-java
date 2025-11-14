/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import javax.annotation.Nullable;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class AssertStatementTreeImpl extends JavaTree implements AssertStatementTree {

  private final InternalSyntaxToken assertToken;
  private final ExpressionTree condition;
  @Nullable
  private InternalSyntaxToken colonToken;
  @Nullable
  private ExpressionTree detail;
  private final InternalSyntaxToken semicolonToken;

  public AssertStatementTreeImpl(InternalSyntaxToken assertToken, ExpressionTree condition, InternalSyntaxToken semicolonToken) {
    this.assertToken = assertToken;
    this.condition = condition;
    this.colonToken = null;
    this.detail = null;
    this.semicolonToken = semicolonToken;
  }

  public AssertStatementTreeImpl complete(InternalSyntaxToken colonToken, ExpressionTree detail) {
    this.colonToken = colonToken;
    this.detail = detail;
    return this;
  }

  @Override
  public Kind kind() {
    return Kind.ASSERT_STATEMENT;
  }

  @Override
  public SyntaxToken assertKeyword() {
    return assertToken;
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Nullable
  @Override
  public SyntaxToken colonToken() {
    return colonToken;
  }

  @Nullable
  @Override
  public ExpressionTree detail() {
    return detail;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitAssertStatement(this);
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Arrays.asList(assertToken, condition),
      colonToken != null ? Arrays.asList(colonToken, detail) : Collections.<Tree>emptyList(),
      Collections.singletonList(semicolonToken));
  }

}
