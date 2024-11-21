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
package org.sonar.java.ast.parser;

import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArgumentListTreeImpl extends ListTreeImpl<ExpressionTree> implements Arguments {

  @Nullable
  private InternalSyntaxToken openParenToken;
  @Nullable
  private InternalSyntaxToken closeParenToken;

  private ArgumentListTreeImpl(List<ExpressionTree> expressions, List<SyntaxToken> separators) {
    super(expressions, separators);
  }

  public static ArgumentListTreeImpl emptyList() {
    return new ArgumentListTreeImpl(new ArrayList<>(), new ArrayList<>());
  }

  public ArgumentListTreeImpl complete(@Nullable InternalSyntaxToken openParenToken, @Nullable InternalSyntaxToken closeParenToken) {
    this.openParenToken = openParenToken;
    this.closeParenToken = closeParenToken;
    return this;
  }

  @Nullable
  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Nullable
  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public Tree.Kind kind() {
    return Tree.Kind.ARGUMENTS;
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      openParenToken != null ? Collections.singletonList(openParenToken) : Collections.emptyList(),
      super.children(),
      closeParenToken != null ? Collections.singletonList(closeParenToken) : Collections.emptyList());
  }
}
