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
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class ArrayAccessExpressionTreeImpl extends AssessableExpressionTree implements ArrayAccessExpressionTree {

  private final ExpressionTree expression;
  private final ArrayDimensionTree dimension;

  public ArrayAccessExpressionTreeImpl(ExpressionTree expression, ArrayDimensionTree dimension) {
    this.expression = expression;
    this.dimension = dimension;
  }

  @Override
  public Kind kind() {
    return Kind.ARRAY_ACCESS_EXPRESSION;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitArrayAccessExpression(this);
  }

  @Override
  public ArrayDimensionTree dimension() {
    return dimension;
  }

  @Override
  public List<Tree> children() {
    return Arrays.asList(expression, dimension);
  }
}
