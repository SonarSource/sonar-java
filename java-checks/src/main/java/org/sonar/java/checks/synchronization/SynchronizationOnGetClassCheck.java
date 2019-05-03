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
package org.sonar.java.checks.synchronization;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

import static org.sonar.plugins.java.api.tree.Tree.Kind.CONSTRUCTOR;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IDENTIFIER;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD_INVOCATION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SYNCHRONIZED_STATEMENT;

@Rule(key = "S3067")
public class SynchronizationOnGetClassCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher GET_CLASS_MATCHER = MethodMatcher.create().name("getClass").withoutParameter();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    SynchronizedStatementTree synchronizedTree = (SynchronizedStatementTree) tree;
    if (synchronizedTree.expression().is(METHOD_INVOCATION)) {
      MethodInvocationTree synchronizedExpr = (MethodInvocationTree) synchronizedTree.expression();
      if (GET_CLASS_MATCHER.matches(synchronizedExpr) && !isEnclosingClassFinal(synchronizedExpr.methodSelect())) {
        reportIssue(synchronizedExpr, "Synchronize on the static class name instead.");
      }
    }
  }

  private static boolean isEnclosingClassFinal(ExpressionTree expressionTree) {
    if (expressionTree.is(IDENTIFIER)) {
      Tree parent = expressionTree.parent();
      while (!parent.is(METHOD, CONSTRUCTOR)) {
        parent = parent.parent();
      }
      return ((MethodTree) parent).symbol().owner().isFinal();
    }
    return ((MemberSelectExpressionTree) expressionTree).expression().symbolType().symbol().isFinal();
  }
}
