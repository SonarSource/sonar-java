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

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2178")
public class NonShortCircuitLogicCheck extends IssuableSubscriptionVisitor {

  private static final Map<String, String> REPLACEMENTS =
    ImmutableMap.of(
      "&", "&&",
      "|", "||");

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.AND, Tree.Kind.OR);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) tree;
    if (isBoolean(binaryExpressionTree.leftOperand().symbolType())) {
      String operator = binaryExpressionTree.operatorToken().text();
      String replacement = REPLACEMENTS.get(operator);
      String sideEffectWarning = "";
      if (mayHaveSideEffect(binaryExpressionTree.rightOperand())) {
        sideEffectWarning = " and extract the right operand to a variable if it should always be evaluated";
      }
      reportIssue(binaryExpressionTree.operatorToken(), "Correct this \"" + operator + "\" to \"" + replacement + "\"" + sideEffectWarning + ".");
    }
  }

  private static boolean isBoolean(Type type) {
    return type.is("boolean") || type.is("java.lang.Boolean");
  }

  private static boolean mayHaveSideEffect(Tree tree) {
    MethodInvocationFinder methodInvocationFinder = new MethodInvocationFinder();
    tree.accept(methodInvocationFinder);
    return methodInvocationFinder.found;
  }

  private static class MethodInvocationFinder extends BaseTreeVisitor {

    boolean found = false;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      found = true;
    }
  }

}
