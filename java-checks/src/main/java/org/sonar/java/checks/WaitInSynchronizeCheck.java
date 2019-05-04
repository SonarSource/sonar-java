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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2273")
public class WaitInSynchronizeCheck extends AbstractInSynchronizeChecker {

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!isInSyncBlock()) {
      IdentifierTree methodName = ExpressionUtils.methodName(mit);
      ExpressionTree methodSelect = mit.methodSelect();
      String lockName;
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        lockName = ((MemberSelectExpressionTree) methodSelect).expression().symbolType().name();
      } else {
        lockName = "this";
      }
      reportIssue(methodName, "Move this call to \"" + methodName + "()\" into a synchronized block to be sure the monitor on \"" + lockName + "\" is held.");
    }
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().name("wait").withoutParameter(),
      MethodMatcher.create().name("wait").addParameter("long"),
      MethodMatcher.create().name("wait").addParameter("long").addParameter("int"),
      MethodMatcher.create().name("notify").withoutParameter(),
      MethodMatcher.create().name("notifyAll").withoutParameter());
  }
}
