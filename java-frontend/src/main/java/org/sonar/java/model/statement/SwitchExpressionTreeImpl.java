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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class SwitchExpressionTreeImpl extends JavaTree implements SwitchExpressionTree {

  private final ExpressionTree expression;
  private final List<CaseGroupTree> cases;
  private final InternalSyntaxToken switchKeyword;
  private final InternalSyntaxToken openParenToken;
  private final InternalSyntaxToken closeParenToken;
  private final InternalSyntaxToken openBraceToken;
  private final InternalSyntaxToken closeBraceToken;

  public SwitchExpressionTreeImpl(InternalSyntaxToken switchKeyword, InternalSyntaxToken openParenToken, ExpressionTree expression,
    InternalSyntaxToken closeParenToken, InternalSyntaxToken openBraceToken, List<CaseGroupTreeImpl> groups, InternalSyntaxToken closeBraceToken) {
    super(Kind.SWITCH_EXPRESSION);
    this.switchKeyword = switchKeyword;
    this.openParenToken = openParenToken;
    this.expression = Objects.requireNonNull(expression);
    this.closeParenToken = closeParenToken;
    this.openBraceToken = openBraceToken;
    this.cases = ImmutableList.<CaseGroupTree>builder().addAll(Objects.requireNonNull(groups)).build();
    this.closeBraceToken = closeBraceToken;
  }

  @Override
  public Type symbolType() {
    return Symbols.unknownType;
  }

  @Override
  public Kind kind() {
    return Kind.SWITCH_EXPRESSION;
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
  public void accept(TreeVisitor visitor) {
    visitor.visitSwitchExpression(this);
  }

  @Override
  public Iterable<Tree> children() {
    return Iterables.concat(
      Lists.newArrayList(switchKeyword, openParenToken, expression, closeParenToken, openBraceToken),
      cases,
      Collections.singletonList(closeBraceToken));
  }
}
