/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S2273")
public class WaitInSynchronizeCheck extends AbstractInSynchronizeChecker {

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!isInSyncBlock()) {
      IdentifierTree methodName;
      String lockName;
      if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
        methodName = mse.identifier();
        lockName = mse.expression().symbolType().name();
      } else {
        methodName = (IdentifierTree) mit.methodSelect();
        lockName = "this";
      }
      reportIssue(methodName, "Make this call to \"" + methodName + "()\" only inside a synchronized block to be sure to hold the monitor on \"" + lockName + "\" object.");
    }
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.<MethodMatcher>builder()
      .add(MethodMatcher.create().name("wait").withoutParameter())
      .add(MethodMatcher.create().name("wait").addParameter("long"))
      .add(MethodMatcher.create().name("wait").addParameter("long").addParameter("int"))
      .add(MethodMatcher.create().name("notify").withoutParameter())
      .add(MethodMatcher.create().name("notifyAll").withoutParameter()).build();
  }
}
