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
package org.sonar.java.resolve;

import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;

import java.util.HashSet;
import java.util.Set;

public class LambdaBlockReturnVisitor extends BaseTreeVisitor {

  final Set<Type> types = new HashSet<>();

  @Override
  public void visitClass(ClassTree tree) {
    // skip visiting inner classes : only first level returns are interesting
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    // skip visiting lambdas : only first level returns are interesting
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    ExpressionTree expression = tree.expression();
    if (expression != null && !expression.symbolType().isUnknown()) {
      types.add(expression.symbolType());
    }
  }
}
