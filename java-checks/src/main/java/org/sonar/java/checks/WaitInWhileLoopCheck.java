/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Deque;
import java.util.List;

@Rule(
  key = "S2274",
  name = "\"Object.wait(...)\" and \"Condition.await(...)\" should be called inside a \"while\" loop",
  tags = {"bug", "cert", "multi-threading"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("20min")
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
      inWhileLoop.push(fst.initializer().isEmpty() && fst.condition()==null && fst.update().isEmpty());
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
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
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
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
        MethodMatcher.create().name("wait"),
        MethodMatcher.create().name("wait").addParameter("long"),
        MethodMatcher.create().name("wait").addParameter("long").addParameter("int"),
        MethodMatcher.create().typeDefinition("java.util.concurrent.locks.Condition").name("await").withNoParameterConstraint()
    );
  }
}
