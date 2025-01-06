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

import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.EmptyStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Collections;

public class EmptyStatementTreeImpl extends JavaTree implements EmptyStatementTree {
  private final InternalSyntaxToken semicolonToken;

  public EmptyStatementTreeImpl(InternalSyntaxToken semicolonToken) {
    this.semicolonToken = semicolonToken;
  }

  @Override
  public Kind kind() {
    return Kind.EMPTY_STATEMENT;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitEmptyStatement(this);
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public List<Tree> children() {
    return Collections.<Tree>singletonList(semicolonToken);
  }

}
