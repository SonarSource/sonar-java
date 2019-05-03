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

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S2225")
public class ToStringReturningNullCheck extends IssuableSubscriptionVisitor {

  private String interestingMethodName = null;

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.RETURN_STATEMENT);
  }
  
  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      interestingMethodName = interestingMethodName((MethodTree) tree);
    } else if (interestingMethodName != null) {
      ExpressionTree returnExpression = ExpressionUtils.skipParentheses(((ReturnStatementTree) tree).expression());
      if (returnExpression.is(Kind.NULL_LITERAL)) {
        if (interestingMethodName.equals("toString")) {
          reportIssue(returnExpression, "Return empty string instead.");
        } else {
          reportIssue(returnExpression, "Return a non null object.");
        }
      }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      interestingMethodName = null;
    }
  }

  private static String interestingMethodName(MethodTree method) {
    String methodName = method.simpleName().name();
    if (method.parameters().isEmpty() && ("toString".equals(methodName) || "clone".equals(methodName))) {
      return methodName;
    }
    return null;
  }

}
