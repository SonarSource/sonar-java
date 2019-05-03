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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1872")
public class ClassComparedByNameCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(MethodMatcher.create().typeDefinition("java.lang.String").name("equals").withAnyParameters());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (!mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      return;
    }

    ExpressionTree firstOperand = ExpressionUtils.skipParentheses(((MemberSelectExpressionTree) mit.methodSelect()).expression());
    ExpressionTree secondOperand = ExpressionUtils.skipParentheses(mit.arguments().get(0));

    // Only check comparison for string literals and use of class#getName methods to avoid FP. Ref: SONARJAVA-2603
    boolean firstOpIsClassGetNameMethod = useClassGetNameMethod(firstOperand);
    boolean secondOpIsClassGetNameMethod = useClassGetNameMethod(secondOperand);

    if (firstOpIsClassGetNameMethod && secondOpIsClassGetNameMethod) {
      reportIssue(mit, "Use \"isAssignableFrom\" instead.");
    } else if ((firstOpIsClassGetNameMethod && secondOperand.is(Tree.Kind.STRING_LITERAL))
      || (secondOpIsClassGetNameMethod && firstOperand.is(Tree.Kind.STRING_LITERAL))) {
      reportIssue(mit, "Use an \"instanceof\" comparison instead.");
    }
  }

  private static boolean useClassGetNameMethod(ExpressionTree expression) {
    ClassGetNameDetector visitor = new ClassGetNameDetector();
    expression.accept(visitor);
    return visitor.useClassGetName;
  }

  private static class ClassGetNameDetector extends BaseTreeVisitor {
    private boolean useClassGetName = false;

    private static final MethodMatcherCollection METHOD_MATCHERS = MethodMatcherCollection.create(
      MethodMatcher.create().typeDefinition("java.lang.Class").name("getName").withoutParameter(),
      MethodMatcher.create().typeDefinition("java.lang.Class").name("getSimpleName").withoutParameter());

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (METHOD_MATCHERS.anyMatch(tree)) {
        useClassGetName = true;
      }
      scan(tree.methodSelect());
    }
  }
}
