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
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Deque;
import java.util.List;

@Rule(key = "S2273",
    priority = Priority.CRITICAL,
    tags = {"bug", "multi-threading"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class WaitInSynchronizeCheck extends AbstractMethodDetection {

  private Deque<Boolean> withinSynchronizedBlock = Lists.newLinkedList();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    withinSynchronizedBlock.push(false);
    super.scanFile(context);
    withinSynchronizedBlock.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.SYNCHRONIZED_STATEMENT, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if(tree.is(Tree.Kind.METHOD_INVOCATION)) {
      super.visitNode(tree);
    } else if(tree.is(Tree.Kind.METHOD)) {
      withinSynchronizedBlock.push(((MethodTree)tree).modifiers().modifiers().contains(Modifier.SYNCHRONIZED));
    } else {
      withinSynchronizedBlock.push(true);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if(tree.is(Tree.Kind.METHOD, Tree.Kind.SYNCHRONIZED_STATEMENT)) {
      withinSynchronizedBlock.pop();
    }
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    if(!withinSynchronizedBlock.peek()) {
      String methodName;
      String lockName;
      if(mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
        methodName = mse.identifier().name();
        lockName = ((AbstractTypedTree) mse.expression()).getSymbolType().getSymbol().getName();
      } else {
        methodName = ((IdentifierTree)mit.methodSelect()).name();
        lockName = "this";
      }
      addIssue(mit, "Make this call to \"" + methodName + "()\" only inside a synchronized block to be sure to hold the monitor on \""+lockName+"\" object.");
    }
  }

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.<MethodInvocationMatcher>builder()
        .add(MethodInvocationMatcher.create().name("wait"))
        .add(MethodInvocationMatcher.create().name("wait").addParameter("long"))
        .add(MethodInvocationMatcher.create().name("wait").addParameter("long").addParameter("int"))
        .add(MethodInvocationMatcher.create().name("notify"))
        .add(MethodInvocationMatcher.create().name("notifyAll")).build();
  }
}
