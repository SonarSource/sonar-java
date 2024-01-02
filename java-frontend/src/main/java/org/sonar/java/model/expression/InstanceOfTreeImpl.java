/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypePatternTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public class InstanceOfTreeImpl extends AssessableExpressionTree implements InstanceOfTree, PatternInstanceOfTree {

  private final Tree.Kind kind;
  private final ExpressionTree expression;
  private final InternalSyntaxToken instanceofToken;

  @Nullable
  private final TypeTree type;
  @Nullable
  private final PatternTree pattern;

  private InstanceOfTreeImpl(Tree.Kind kind, ExpressionTree expression, InternalSyntaxToken instanceofToken, @Nullable TypeTree type, @Nullable PatternTree pattern) {
    this.kind = kind;
    this.expression = expression;
    this.instanceofToken = instanceofToken;
    this.type = type;
    this.pattern = pattern;
  }

  public InstanceOfTreeImpl(ExpressionTree expression, InternalSyntaxToken instanceofToken, TypeTree type) {
    this(Tree.Kind.INSTANCE_OF, expression, instanceofToken, type, null);
  }

  public InstanceOfTreeImpl(ExpressionTree expression, InternalSyntaxToken instanceofToken, PatternTree pattern) {
    this(Kind.PATTERN_INSTANCE_OF, expression, instanceofToken, null, pattern);
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
   * Only works for INSTANCE_OF, mutually exclusive with {@link #pattern()}
   */
  @Override
  public TypeTree type() {
    return type;
  }


  /**
   * Deprecated, will be dropped
   */
  @Override
  public VariableTree variable() {
    if (pattern != null && pattern.is(Tree.Kind.TYPE_PATTERN)) {
      // in practice, we can not have another type than a TYPE_PATTERN for a PATTERN_INSTANCE_OF tree.
      // ECJ does not yet support the other patterns in this case, and so the variable will always be there.
      // it's a bug, supposed to be fix by this PR: https://github.com/eclipse-jdt/eclipse.jdt.core/pull/437
      return ((TypePatternTree) pattern).patternVariable();
    }
    return null;
  }

  /**
   * Only works for PATTERN_INSTANCE_OF, mutually exclusive with {@link #type()}
   */
  @Override
  public PatternTree pattern() {
    return pattern;
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
    return Arrays.asList(expression, instanceofToken, (kind == Tree.Kind.INSTANCE_OF ? type : pattern));
  }

}
