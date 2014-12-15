/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Deque;
import java.util.List;

@Rule(key = "S2274",
    priority = Priority.CRITICAL,
    tags = {"bug", "multi-threading"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class WaitInWhileLoopCheck extends AbstractMethodDetection {

  private Deque<Boolean> inWhileLoop = Lists.newLinkedList();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    inWhileLoop.push(false);
    super.scanFile(context);
    inWhileLoop.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      super.visitNode(tree);
    } else if(tree.is(Tree.Kind.FOR_STATEMENT)) {
      ForStatementTree fst = (ForStatementTree) tree;
      inWhileLoop.push(fst.initializer().isEmpty() && fst.update().isEmpty() && fst.initializer().isEmpty());
    } else {
      inWhileLoop.push(true);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.FOR_STATEMENT)) {
      inWhileLoop.pop();
    }
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    if (!inWhileLoop.peek()) {
      String methodName;
      if(mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
        methodName = mse.identifier().name();
      } else {
        methodName = ((IdentifierTree)mit.methodSelect()).name();
      }
      addIssue(mit, "Remove this call to \""+methodName+"\" or move it into a \"while\" loop.");
    }
  }

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
        MethodInvocationMatcher.create().name("wait"),
        MethodInvocationMatcher.create().name("wait").addParameter("long"),
        MethodInvocationMatcher.create().name("wait").addParameter("long").addParameter("int"),
        MethodInvocationMatcher.create().typeDefinition("java.util.concurrent.locks.Condition").name("await").withNoParameterConstraint()
    );
  }
}
