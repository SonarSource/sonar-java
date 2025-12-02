/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
import javax.annotation.Nullable;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class BreakStatementTreeImpl extends JavaTree implements BreakStatementTree {
  private final InternalSyntaxToken breakToken;
  @Nullable
  private final ExpressionTree labelOrValue;
  private final InternalSyntaxToken semicolonToken;

  public BreakStatementTreeImpl(InternalSyntaxToken breakToken, @Nullable ExpressionTree labelOrValue, InternalSyntaxToken semicolonToken) {
    this.breakToken = breakToken;
    this.labelOrValue = labelOrValue;
    this.semicolonToken = semicolonToken;
  }

  @Override
  public Kind kind() {
    return Kind.BREAK_STATEMENT;
  }

  @Override
  public SyntaxToken breakKeyword() {
    return breakToken;
  }

  @Nullable
  @Override
  public IdentifierTree label() {
    return labelOrValue instanceof IdentifierTree identifierTree ? identifierTree : null;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitBreakStatement(this);
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Collections.singletonList(breakToken),
      labelOrValue != null ? Collections.singletonList(labelOrValue) : Collections.<Tree>emptyList(),
      Collections.<Tree>singletonList(semicolonToken));
  }

}
