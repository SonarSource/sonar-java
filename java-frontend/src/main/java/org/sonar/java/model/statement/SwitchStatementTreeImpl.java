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

import java.util.Collections;
import java.util.List;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class SwitchStatementTreeImpl extends JavaTree implements SwitchStatementTree {

  private final SwitchExpressionTree switchExpression;

  public SwitchStatementTreeImpl(SwitchExpressionTree switchExpression) {
    super(Kind.SWITCH_STATEMENT);
    this.switchExpression = switchExpression;
  }

  @Override
  public SwitchExpressionTree asSwitchExpression() {
    return switchExpression;
  }

  @Override
  public Kind kind() {
    return Kind.SWITCH_STATEMENT;
  }

  @Override
  public SyntaxToken switchKeyword() {
    return switchExpression.switchKeyword();
  }

  @Override
  public SyntaxToken openParenToken() {
    return switchExpression.openParenToken();
  }

  @Override
  public ExpressionTree expression() {
    return switchExpression.expression();
  }

  @Override
  public SyntaxToken closeParenToken() {
    return switchExpression.closeParenToken();
  }

  @Override
  public SyntaxToken openBraceToken() {
    return switchExpression.openBraceToken();
  }

  @Override
  public List<CaseGroupTree> cases() {
    return switchExpression.cases();
  }

  @Override
  public SyntaxToken closeBraceToken() {
    return switchExpression.closeBraceToken();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitSwitchStatement(this);
  }

  @Override
  public Iterable<Tree> children() {
    return Collections.singletonList(switchExpression);
  }

}
