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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.query.MethodInvocationQuery;
import org.sonar.plugins.java.api.query.MethodQuery;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S1182")
public class CloneMethodCallsSuperCloneCheck extends IssuableSubscriptionVisitor {

  private static class Context {
    boolean superCloneCallIsMissing = true;
  }

  private static final Kind[] OUTSIDE_METHOD_BODY_SCOPE = {Kind.LAMBDA_EXPRESSION, Kind.NEW_CLASS, Kind.CLASS, Kind.ENUM, Kind.INTERFACE, Kind.RECORD};

  private static final MethodInvocationQuery<Context> SUPER_CLONE_CALL_QUERY = new MethodQuery<Context>()
    .subtreesExcluding((ctx, tree) -> tree.is(OUTSIDE_METHOD_BODY_SCOPE))
    .filterMethodInvocationTree()
    .filter((ctx, mse) -> isSuperCloneCall(mse))
    .visit((ctx, it) -> ctx.superCloneCallIsMissing = false);

  @Override
  public List<Kind> nodesToVisit() {
    return List.of(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (isCloneMethod(methodTree) && SUPER_CLONE_CALL_QUERY.apply(new Context(), tree).superCloneCallIsMissing) {
      reportIssue(((MethodTree) tree).simpleName(), "Use super.clone() to create and seed the cloned instance to be returned.");
    }
  }

  private static boolean isCloneMethod(MethodTree methodTree) {
    return "clone".equals(methodTree.simpleName().name())
      && methodTree.parameters().isEmpty()
      && methodTree.block() != null;
  }

  private static boolean isSuperCloneCall(MethodInvocationTree mit) {
    return mit.arguments().isEmpty()
      && mit.methodSelect().is(Kind.MEMBER_SELECT)
      && isSuperClone((MemberSelectExpressionTree) mit.methodSelect());
  }

  private static boolean isSuperClone(MemberSelectExpressionTree tree) {
    return "clone".equals(tree.identifier().name())
      && tree.expression().is(Kind.IDENTIFIER)
      && "super".equals(((IdentifierTree) tree.expression()).name());
  }
}
