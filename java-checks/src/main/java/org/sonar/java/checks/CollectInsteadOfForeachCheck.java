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
package org.sonar.java.checks;

import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2203")
public class CollectInsteadOfForeachCheck extends AbstractMethodDetection {

  private static final MethodMatchers FOREACH = MethodMatchers.create().ofTypes("java.util.stream.Stream").names("forEach").withAnyParameters().build();
  private static final MethodMatchers ADD = MethodMatchers.create().ofSubTypes("java.util.List").names("add").withAnyParameters().build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return FOREACH;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArgument = mit.arguments().get(0);
    if (firstArgument.is(Tree.Kind.METHOD_REFERENCE)) {
      handleMethodReference((MethodReferenceTree) firstArgument);
    } else if (firstArgument.is(Tree.Kind.LAMBDA_EXPRESSION)) {
      handleLambdaExpression((LambdaExpressionTree) firstArgument);
    }
  }

  private void handleMethodReference(MethodReferenceTree methodRef) {
    Tree expression = methodRef.expression();
    if (ADD.matches(methodRef.method().symbol())) {
      checkExpression(methodRef, expression);
    }
  }

  private void handleLambdaExpression(LambdaExpressionTree lambda) {
    Tree expr = lambda.body();
    if (expr.is(Tree.Kind.BLOCK)) {
      expr = expressionFromSingleStatementBlock(((BlockTree) expr).body());
    }
    if (expr != null && expr.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expr;
      if (ADD.matches(mit)) {
        ExpressionTree methodSelect = mit.methodSelect();
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          checkExpression(lambda, ((MemberSelectExpressionTree) methodSelect).expression());
        }
      }
    }
  }

  @CheckForNull
  private static ExpressionTree expressionFromSingleStatementBlock(List<StatementTree> body) {
    if (body.size() == 1) {
      StatementTree singleStatement = body.get(0);
      if (singleStatement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        return ((ExpressionStatementTree) singleStatement).expression();
      }
    }
    return null;
  }

  private void checkExpression(Tree reportTree, Tree expression) {
    Optional<String> listName = Optional.empty();
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      listName = Optional.of(((IdentifierTree) expression).name());
    } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      listName = Optional.of(((MemberSelectExpressionTree) expression).identifier().name());
    }
    listName.ifPresent(list -> context.reportIssue(this, reportTree, getMessage(reportTree, list)));
  }

  private static String getMessage(Tree reportTree, String listName) {
    String msg;
    if (reportTree.is(Tree.Kind.METHOD_REFERENCE)) {
      msg = "Use \"collect(Collectors.toList())\" instead of \"forEach(%s::add)\".";
    } else {
      msg = "Use \"collect(Collectors.toList())\" instead of adding elements in \"%s\" using \"forEach(...)\".";
    }
    return String.format(msg, listName);
  }

}
