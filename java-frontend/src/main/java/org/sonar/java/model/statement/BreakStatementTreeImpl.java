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

import com.google.common.collect.Iterables;
import java.util.Collections;
import javax.annotation.Nullable;
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
    super(Kind.BREAK_STATEMENT);
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
    return labelOrValue instanceof IdentifierTree ? (IdentifierTree) labelOrValue : null;
  }

  @Nullable
  @Override
  public ExpressionTree value() {
    return labelOrValue;
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
  public Iterable<Tree> children() {
    return Iterables.concat(
      Collections.singletonList(breakToken),
      labelOrValue != null ? Collections.singletonList(labelOrValue) : Collections.<Tree>emptyList(),
      Collections.<Tree>singletonList(semicolonToken));
  }

}
