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
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class SwitchExpressionTreeImpl extends SwitchTreeImpl implements SwitchExpressionTree {

  public SwitchExpressionTreeImpl(InternalSyntaxToken switchKeyword, InternalSyntaxToken openParenToken, ExpressionTree expression,
    InternalSyntaxToken closeParenToken, InternalSyntaxToken openBraceToken, List<CaseGroupTreeImpl> groups, InternalSyntaxToken closeBraceToken) {
    super(switchKeyword, openParenToken, expression, closeParenToken, openBraceToken, groups, closeBraceToken);
  }

  @Override
  public Kind kind() {
    return Kind.SWITCH_EXPRESSION;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitSwitchExpression(this);
  }

}
