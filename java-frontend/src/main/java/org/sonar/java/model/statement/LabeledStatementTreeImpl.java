/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model.statement;

import com.google.common.collect.Lists;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.JavaSymbol;
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
  private Symbol.LabelSymbol symbol;

  public LabeledStatementTreeImpl(IdentifierTree label, InternalSyntaxToken colonToken, StatementTree statement) {
    super(Kind.LABELED_STATEMENT);
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
    return symbol;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitLabeledStatement(this);
  }

  @Override
  public Iterable<Tree> children() {
    return Lists.newArrayList(
      label,
      colonToken,
      statement);
  }

  public void setSymbol(JavaSymbol.JavaLabelSymbol symbol) {
    this.symbol = symbol;
  }
}
