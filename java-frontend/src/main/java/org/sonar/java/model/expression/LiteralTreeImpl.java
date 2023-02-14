/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.model.expression;

import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import java.util.Collections;
import java.util.Objects;

public class LiteralTreeImpl extends AssessableExpressionTree implements LiteralTree {

  private final Kind kind;
  private final InternalSyntaxToken token;

  public LiteralTreeImpl(Kind kind, InternalSyntaxToken token) {
    this.kind = Objects.requireNonNull(kind);
    this.token = token;
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Override
  public String value() {
    return token.text();
  }

  @Override
  public SyntaxToken token() {
    return token;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitLiteral(this);
  }

  @Override
  public List<Tree> children() {
    return Collections.<Tree>singletonList(token);
  }

}
