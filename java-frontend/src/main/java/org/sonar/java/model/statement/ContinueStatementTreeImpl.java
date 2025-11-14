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

import java.util.List;
import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Collections;

public class ContinueStatementTreeImpl extends JavaTree implements ContinueStatementTree {
  
  private final InternalSyntaxToken continueKeyword;
  @Nullable
  private final IdentifierTree label;
  private final InternalSyntaxToken semicolonToken;

  public ContinueStatementTreeImpl(InternalSyntaxToken continueKeyword, @Nullable IdentifierTreeImpl label, InternalSyntaxToken semicolonToken) {
    this.continueKeyword = continueKeyword;
    this.label = label;
    this.semicolonToken = semicolonToken;
  }

  @Override
  public Kind kind() {
    return Kind.CONTINUE_STATEMENT;
  }

  @Override
  public SyntaxToken continueKeyword() {
    return continueKeyword;
  }

  @Nullable
  @Override
  public IdentifierTree label() {
    return label;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitContinueStatement(this);
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Collections.singletonList(continueKeyword),
      label != null ? Collections.singletonList(label) : Collections.<Tree>emptyList(),
      Collections.singletonList(semicolonToken));
  }

}
