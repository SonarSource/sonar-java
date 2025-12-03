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

import java.util.Arrays;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JLabelSymbol;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Objects;

public class LabeledStatementTreeImpl extends JavaTree implements LabeledStatementTree {
  private final IdentifierTree label;
  private final InternalSyntaxToken colonToken;
  private final StatementTree statement;

  public JLabelSymbol labelSymbol;

  public LabeledStatementTreeImpl(IdentifierTree label, InternalSyntaxToken colonToken, StatementTree statement) {
    this.label = Objects.requireNonNull(label);
    this.colonToken = colonToken;
    this.statement = Objects.requireNonNull(statement);
  }

  @Override
  public Kind kind() {
    return Kind.LABELED_STATEMENT;
  }

  @Override
  public IdentifierTree label() {
    return label;
  }

  @Override
  public SyntaxToken colonToken() {
    return colonToken;
  }

  @Override
  public StatementTree statement() {
    return statement;
  }

  @Override
  public Symbol.LabelSymbol symbol() {
    return labelSymbol;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitLabeledStatement(this);
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(label, colonToken, statement);
  }
}
