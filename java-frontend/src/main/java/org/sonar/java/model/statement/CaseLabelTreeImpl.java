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

import java.util.Collections;
import java.util.List;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class CaseLabelTreeImpl extends JavaTree implements CaseLabelTree {
  private final InternalSyntaxToken caseOrDefaultKeyword;
  private final List<ExpressionTree> expressions;
  private final boolean isFallThrough;
  private final InternalSyntaxToken colonOrArrowToken;

  public CaseLabelTreeImpl(InternalSyntaxToken caseOrDefaultKeyword, List<ExpressionTree> expressions, InternalSyntaxToken colonOrArrowToken) {
    this.caseOrDefaultKeyword = caseOrDefaultKeyword;
    this.expressions = expressions;
    this.isFallThrough = JavaPunctuator.COLON.getValue().equals(colonOrArrowToken.text());
    this.colonOrArrowToken = colonOrArrowToken;
  }

  @Override
  public Kind kind() {
    return Kind.CASE_LABEL;
  }

  @Override
  public SyntaxToken caseOrDefaultKeyword() {
    return caseOrDefaultKeyword;
  }

  @Override
  public boolean isFallThrough() {
    return isFallThrough;
  }

  @Override
  public List<ExpressionTree> expressions() {
    return expressions;
  }

  @Override
  public SyntaxToken colonOrArrowToken() {
    return colonOrArrowToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitCaseLabel(this);
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Collections.singletonList(caseOrDefaultKeyword),
      expressions,
      Collections.singletonList(colonOrArrowToken));
  }

}
