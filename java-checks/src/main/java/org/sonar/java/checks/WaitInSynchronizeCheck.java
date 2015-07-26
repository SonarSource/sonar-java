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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2273",
  name = "\"wait(...)\", \"notify()\" and \"notifyAll()\" methods should only be called when a lock is obviously held on an object",
  tags = {"bug", "multi-threading"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("20min")
public class WaitInSynchronizeCheck extends AbstractInSynchronizeChecker {

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!isInSyncBlock()) {
      String methodName;
      String lockName;
      if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
        methodName = mse.identifier().name();
        lockName = mse.expression().symbolType().name();
      } else {
        methodName = ((IdentifierTree) mit.methodSelect()).name();
        lockName = "this";
      }
      addIssue(mit, "Make this call to \"" + methodName + "()\" only inside a synchronized block to be sure to hold the monitor on \"" + lockName + "\" object.");
    }
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.<MethodMatcher>builder()
        .add(MethodMatcher.create().name("wait"))
        .add(MethodMatcher.create().name("wait").addParameter("long"))
        .add(MethodMatcher.create().name("wait").addParameter("long").addParameter("int"))
        .add(MethodMatcher.create().name("notify"))
        .add(MethodMatcher.create().name("notifyAll")).build();
  }
}
