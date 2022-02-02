/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
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
      reportNoStrongDecision(methodTree, AuthorizationsStrongDecisionsCheck::isStrongVoteDecision, "vote", "ACCESS_DENIED");
    } else if (PERMISSION_EVALUATOR_HAS_PERMISSION.matches(methodTree)) {
      reportNoStrongDecision(methodTree, AuthorizationsStrongDecisionsCheck::isStrongHasPermissionDecision, "hasPermission", "false");
    }
  }

  private void reportNoStrongDecision(MethodTree methodTree, Predicate<ExpressionTree> isStrongDecision, String methodName, String strongDecision) {
    ReturnStatementVisitor returnStatementVisitor = new ReturnStatementVisitor(isStrongDecision);
    methodTree.accept(returnStatementVisitor);
    if (!returnStatementVisitor.takesStrongDecision()) {
      reportIssue(methodTree.simpleName(), String.format("\"%s\" method should return at least one time %s.", methodName, strongDecision));
    }
  }

  private static boolean isStrongVoteDecision(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      expression = ((MemberSelectExpressionTree) expression).identifier();
    }
    if (expression instanceof LiteralTree || expression.is(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
      // Returning literals (even the value for DENIED) is considered as not strong.
      return false;
    } else if (expression.is(Tree.Kind.IDENTIFIER)) {
      String name = ((IdentifierTree) expression).name();
      if ("ACCESS_DENIED".equals(name)) {
        return true;
      } else if ("ACCESS_GRANTED".equals(name) || "ACCESS_ABSTAIN".equals(name)) {
        return false;
      }
    }
    // Expression is not a literal or a known identifier, we consider it as strong to avoid FPs.
    return true;
  }

  private static boolean isStrongHasPermissionDecision(ExpressionTree expression) {
    if (expression instanceof LiteralTree) {
      return expression.asConstant(Boolean.class).filter(Boolean.FALSE::equals).isPresent();
    }
    return true;
  }

  private static class ReturnStatementVisitor extends BaseTreeVisitor {

    private final Predicate<ExpressionTree> isStrongDecision;
    private boolean takesStrongDecision = false;

    ReturnStatementVisitor(Predicate<ExpressionTree> isStrongDecision) {
      this.isStrongDecision = isStrongDecision;
    }

    public boolean takesStrongDecision() {
      return takesStrongDecision;
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      ExpressionTree expression = tree.expression();
      if (expression != null && isStrongDecision.test(expression)) {
        takesStrongDecision = true;
      }
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      // Throwing an exception is considered as taking a strong decision
      takesStrongDecision = true;
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
