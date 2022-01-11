/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

import static org.sonar.plugins.java.api.tree.Tree.Kind.CLASS;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONSTRUCTOR;
import static org.sonar.plugins.java.api.tree.Tree.Kind.ENUM;
import static org.sonar.plugins.java.api.tree.Tree.Kind.IDENTIFIER;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD_INVOCATION;
import static org.sonar.plugins.java.api.tree.Tree.Kind.SYNCHRONIZED_STATEMENT;

@Rule(key = "S3067")
public class SynchronizationOnGetClassCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers GET_CLASS_MATCHER = MethodMatchers.create().ofAnyType().names("getClass").addWithoutParametersMatcher().build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
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
      MethodTree methodTree = findMethodTreeAncestor(expressionTree);
      if (methodTree != null) {
        return methodTree.symbol().owner().isFinal();
      }
      return findClassTreeAncestor(expressionTree).symbol().isFinal();
    }
    return ((MemberSelectExpressionTree) expressionTree).expression().symbolType().symbol().isFinal();
  }

  private static ClassTree findClassTreeAncestor(ExpressionTree expressionTree) {
    Tree parent = expressionTree.parent();
    while (!parent.is(CLASS, ENUM)) {
      parent = parent.parent();
    }
    return (ClassTree) parent;
  }

  private static MethodTree findMethodTreeAncestor(ExpressionTree expressionTree) {
    Tree parent = expressionTree.parent();
    while (parent != null) {
      if (parent.is(METHOD, CONSTRUCTOR)) {
        return (MethodTree) parent;
      }
      parent = parent.parent();
    }
    return null;
  }

}
