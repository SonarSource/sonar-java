/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.JavaFileScannerContext.Location;

@Rule(key = "S2139")
public class LoggedRethrownExceptionsCheck extends IssuableSubscriptionVisitor {
  private static final MethodMatcher JAVA_UTIL_LOGGER = MethodMatcher.create().typeDefinition("java.util.logging.Logger").name("log").withAnyParameters();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    CatchTree catchTree = (CatchTree) tree;
    boolean isRethrowing = false;
    boolean isLogging = false;
    List<Location> secondaryLocations = new ArrayList<>();
    for (StatementTree statementTree : catchTree.block().body()) {
      if (statementTree.is(Tree.Kind.THROW_STATEMENT)) {
        secondaryLocations.add(new Location("", ((ThrowStatementTree) statementTree).expression()));
        isRethrowing = true;
      } else if (isLoggingMethod(statementTree, catchTree.parameter().simpleName().name())) {
        secondaryLocations.add(new Location("", statementTree));
        isLogging = true;
      }
    }
    if (isLogging && isRethrowing) {
      reportIssue(catchTree.parameter(), "Either log this exception and handle it, or rethrow it with some contextual information.", secondaryLocations, 0);
    }
  }

  private static boolean isLoggingMethod(StatementTree statementTree, String exceptionIdentifier) {
    if (!statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return false;
    }
    ExpressionTree expression = ((ExpressionStatementTree) statementTree).expression();
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      return JAVA_UTIL_LOGGER.matches(mit) && mit.arguments().stream().anyMatch(
        param -> param.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree)param).name().equals(exceptionIdentifier));
    }
    return false;
  }
}
