/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.PatternInstanceOfTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class InstanceOfTreeImpl extends AssessableExpressionTree implements InstanceOfTree, PatternInstanceOfTree {

  private final Tree.Kind kind;
  private final ExpressionTree expression;
  private final InternalSyntaxToken instanceofToken;

  @Nullable
  private final TypeTree type;
  @Nullable
  private final VariableTree variable;

  private InstanceOfTreeImpl(Tree.Kind kind, ExpressionTree expression, InternalSyntaxToken instanceofToken, @Nullable TypeTree type, @Nullable VariableTree variable) {
    this.kind = kind;
    this.expression = expression;
    this.instanceofToken = instanceofToken;
    this.type = type;
    this.variable = variable;
  }

  public InstanceOfTreeImpl(ExpressionTree expression, InternalSyntaxToken instanceofToken, TypeTree type) {
    this(Tree.Kind.INSTANCE_OF, expression, instanceofToken, type, null);
  }

  public InstanceOfTreeImpl(ExpressionTree expression, InternalSyntaxToken instanceofToken, VariableTree variable) {
    this(Tree.Kind.PATTERN_INSTANCE_OF, expression, instanceofToken, null, variable);
  }

  @Override
  public Tree.Kind kind() {
    return kind;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public SyntaxToken instanceofKeyword() {
    return instanceofToken;
  }

  /**
   * Only works for INSTANCE_OF, mutually exclusive with {@link #variable()}
   */
  @Override
  public TypeTree type() {
    return type;
  }

  /**
   * Only works for PATTERN_INSTANCE_OF, mutually exclusive with {@link #type()}
   */
  @Override
  public VariableTree variable() {
    return variable;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    if (kind == Tree.Kind.INSTANCE_OF) {
      visitor.visitInstanceOf(this);
    } else {
      visitor.visitPatternInstanceOf(this);
    }
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(expression, instanceofToken, (kind == Tree.Kind.INSTANCE_OF ? type : variable));
  }

}
