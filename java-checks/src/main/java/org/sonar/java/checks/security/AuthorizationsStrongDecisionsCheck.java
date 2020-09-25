/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5808")
public class AuthorizationsStrongDecisionsCheck extends IssuableSubscriptionVisitor {

  private static final String AUTHENTICATION = "org.springframework.security.core.Authentication";
  private static final String JAVA_OBJECT = "java.lang.Object";

  private static final MethodMatchers ACCESS_DECISION_VOTER_VOTE = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.access.AccessDecisionVoter")
    .names("vote")
    .addParametersMatcher(AUTHENTICATION, JAVA_OBJECT, "java.util.Collection")
    .build();

  private static final MethodMatchers PERMISSION_EVALUATOR_HAS_PERMISSION = MethodMatchers.create()
    .ofSubTypes("org.springframework.security.access.PermissionEvaluator")
    .names("hasPermission")
    .addParametersMatcher(AUTHENTICATION, JAVA_OBJECT, JAVA_OBJECT)
    .addParametersMatcher(AUTHENTICATION, "java.io.Serializable", "java.lang.String", JAVA_OBJECT)
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (ACCESS_DECISION_VOTER_VOTE.matches(methodTree)) {
      reportNoStrongDecision(methodTree,
        e -> e.is(Tree.Kind.IDENTIFIER) && "ACCESS_DENIED".equals(((IdentifierTree) e).name()), "vote", "ACCESS_DENIED");
    } else if (PERMISSION_EVALUATOR_HAS_PERMISSION.matches(methodTree)) {
      reportNoStrongDecision(methodTree, e -> e.asConstant(Boolean.class).filter(Boolean.FALSE::equals).isPresent(), "hasPermission", "false");
    }
  }

  private void reportNoStrongDecision(MethodTree methodTree, Predicate<ExpressionTree> isStrongDecision, String methodName, String strongDecision) {
    ReturnStatementVisitor returnStatementVisitor = new ReturnStatementVisitor(isStrongDecision);
    methodTree.accept(returnStatementVisitor);
    if (!returnStatementVisitor.takesStrongDecision()) {
      reportIssue(methodTree.simpleName(), String.format("\"%s\" method should return at least one time %s.", methodName, strongDecision));
    }
  }

  private static class ReturnStatementVisitor extends BaseTreeVisitor {

    private final Predicate<ExpressionTree> isStrongDecision;
    private boolean containsComplexReturn = false;
    private boolean takesStrongDecision = false;

    ReturnStatementVisitor(Predicate<ExpressionTree> isStrongDecision) {
      this.isStrongDecision = isStrongDecision;
    }

    public boolean takesStrongDecision() {
      // We don't compute the return value when the expression is complex (method call, expression),
      // and consider it as strong decision to avoid FP
      return takesStrongDecision || containsComplexReturn;
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      ExpressionTree expression = tree.expression();
      if (expression != null && (expression instanceof LiteralTree || expression.is(Tree.Kind.IDENTIFIER))) {
        if (isStrongDecision.test(expression)) {
          takesStrongDecision = true;
        }
      } else {
        containsComplexReturn = true;
      }
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip inner classes
    }
  }

}
