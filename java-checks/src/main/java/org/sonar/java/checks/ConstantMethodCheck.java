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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S3400")
public class ConstantMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    BlockTree body = methodTree.block();
    if (!methodTree.modifiers().annotations().isEmpty() || ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.DEFAULT)) {
      return;
    }
    if (Boolean.FALSE.equals(methodTree.isOverriding()) && body != null && body.body().size() == 1) {
      StatementTree uniqueStatement = body.body().get(0);
      if (uniqueStatement.is(Kind.RETURN_STATEMENT)) {
        ExpressionTree returnedExpression = ((ReturnStatementTree) uniqueStatement).expression();
        if (isConstant(returnedExpression)) {
          reportIssue(returnedExpression, "Remove this method and declare a constant for this value.");
        }
      }
    }
  }

  private static boolean isConstant(@Nullable ExpressionTree returnedExpression) {
    return returnedExpression != null
            && returnedExpression.is(Kind.INT_LITERAL, Kind.LONG_LITERAL,
            Kind.CHAR_LITERAL, Kind.STRING_LITERAL,
            Kind.DOUBLE_LITERAL, Kind.FLOAT_LITERAL);
  }
}
