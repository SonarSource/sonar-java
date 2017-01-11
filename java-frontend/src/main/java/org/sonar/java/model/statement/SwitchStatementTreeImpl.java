/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Collections;
import java.util.List;

public class SwitchStatementTreeImpl extends JavaTree implements SwitchStatementTree {

  private final ExpressionTree expression;
  private final List<CaseGroupTree> cases;
  private final InternalSyntaxToken switchKeyword;
  private final InternalSyntaxToken openParenToken;
  private final InternalSyntaxToken closeParenToken;
  private final InternalSyntaxToken openBraceToken;
  private final InternalSyntaxToken closeBraceToken;

  public SwitchStatementTreeImpl(InternalSyntaxToken switchKeyword, InternalSyntaxToken openParenToken, ExpressionTree expression, InternalSyntaxToken closeParenToken,
    InternalSyntaxToken openBraceToken, List<CaseGroupTreeImpl> groups, InternalSyntaxToken closeBraceToken) {
    super(Kind.SWITCH_STATEMENT);
    this.switchKeyword = switchKeyword;
    this.openParenToken = openParenToken;
    this.expression = Preconditions.checkNotNull(expression);
    this.closeParenToken = closeParenToken;
    this.openBraceToken = openBraceToken;
    this.cases = ImmutableList.<CaseGroupTree>builder().addAll(Preconditions.checkNotNull(groups)).build();
    this.closeBraceToken = closeBraceToken;
  }

  @Override
  public Kind kind() {
    return Kind.SWITCH_STATEMENT;
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
    visitor.visitSwitchStatement(this);
  }

  @Override
  public Iterable<Tree> children() {
    return Iterables.concat(
      Lists.newArrayList(switchKeyword, openParenToken, expression, closeParenToken, openBraceToken),
      cases,
      Collections.singletonList(closeBraceToken));
  }

}
