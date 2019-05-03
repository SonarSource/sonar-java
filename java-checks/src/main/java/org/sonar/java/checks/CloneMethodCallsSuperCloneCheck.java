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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S1182")
public class CloneMethodCallsSuperCloneCheck extends IssuableSubscriptionVisitor {

  private boolean foundSuperClone;

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.METHOD, Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (isCloneMethod(tree)) {
      foundSuperClone = false;
    } else if (isSuperCloneCall(tree)) {
      foundSuperClone = true;
    }

  }

  @Override
  public void leaveNode(Tree tree) {
    if (isCloneMethod(tree) && !foundSuperClone) {
      reportIssue(((MethodTree) tree).simpleName(), "Use super.clone() to create and seed the cloned instance to be returned.");
    }
  }

  private static boolean isCloneMethod(Tree tree) {
    if (!tree.is(Kind.METHOD)) {
      return false;
    }
    MethodTree methodTree = (MethodTree) tree;
    return "clone".equals(methodTree.simpleName().name()) && methodTree.parameters().isEmpty() && methodTree.block() != null;
  }

  private static boolean isSuperCloneCall(Tree tree) {
    if (!tree.is(Kind.METHOD_INVOCATION)) {
      return false;
    }

    MethodInvocationTree mit = (MethodInvocationTree) tree;

    return mit.arguments().isEmpty() &&
      mit.methodSelect().is(Kind.MEMBER_SELECT) &&
      isSuperClone((MemberSelectExpressionTree) mit.methodSelect());
  }

  private static boolean isSuperClone(MemberSelectExpressionTree tree) {
    return "clone".equals(tree.identifier().name()) &&
      tree.expression().is(Kind.IDENTIFIER) &&
      "super".equals(((IdentifierTree) tree.expression()).name());
  }

}
