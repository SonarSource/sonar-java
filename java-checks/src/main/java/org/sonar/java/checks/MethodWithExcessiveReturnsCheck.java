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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "S1142")
public class MethodWithExcessiveReturnsCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAX = 3;

  @RuleProperty(description = "Maximum allowed return statements per method", defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private final Multiset<Tree> returnStatementCounter = HashMultiset.create();
  private final Deque<Tree> methods = new LinkedList<>();

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    returnStatementCounter.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.RETURN_STATEMENT, Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.RETURN_STATEMENT)) {
      returnStatementCounter.add(methods.peek());
    } else {
      methods.push(tree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    Tree reportTree = null;
    if (tree.is(Tree.Kind.METHOD)) {
      reportTree = ((MethodTree) tree).simpleName();
    } else if (tree.is(Tree.Kind.LAMBDA_EXPRESSION)) {
      reportTree = ((LambdaExpressionTree) tree).arrowToken();
    }
    if (reportTree != null) {
      int count = returnStatementCounter.count(tree);
      if (count > max) {
        reportIssue(reportTree, "Reduce the number of returns of this method " + count + ", down to the maximum allowed " + max + ".");
      }
      methods.pop();
    }
  }
}
