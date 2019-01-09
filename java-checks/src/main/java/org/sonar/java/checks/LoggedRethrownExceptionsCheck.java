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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
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
  private static final String JAVA_UTIL_LOGGING_LOGGER = "java.util.logging.Logger";
  private static final String SLF4J_LOGGER = "org.slf4j.Logger";
  private static final MethodMatcherCollection LOGGING_METHODS = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("config").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("info").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("log").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("logp").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("logrb").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("throwing").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("severe").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("warning").withAnyParameters(),
    MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name("debug").withAnyParameters(),
    MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name("error").withAnyParameters(),
    MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name("info").withAnyParameters(),
    MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name("trace").withAnyParameters(),
    MethodMatcher.create().typeDefinition(SLF4J_LOGGER).name("warn").withAnyParameters()
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    CatchTree catchTree = (CatchTree) tree;
    boolean isLogging = false;
    List<Location> secondaryLocations = new ArrayList<>();
    for (StatementTree statementTree : catchTree.block().body()) {
      IdentifierTree exceptionIdentifier = catchTree.parameter().simpleName();
      if (isLogging && statementTree.is(Tree.Kind.THROW_STATEMENT) &&
        isExceptionUsed(exceptionIdentifier, ((ThrowStatementTree) statementTree).expression())) {

        secondaryLocations.add(new Location("", ((ThrowStatementTree) statementTree).expression()));
        reportIssue(catchTree.parameter(), "Either log this exception and handle it, or rethrow it with some contextual information.", secondaryLocations, 0);
        return;
      } else if (isLoggingMethod(statementTree, exceptionIdentifier)) {
        secondaryLocations.add(new Location("", statementTree));
        isLogging = true;
      }
    }
  }

  private static boolean isLoggingMethod(StatementTree statementTree, IdentifierTree exceptionIdentifier) {
    if (!statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return false;
    }
    ExpressionTree expression = ((ExpressionStatementTree) statementTree).expression();
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      return LOGGING_METHODS.anyMatch(mit) && isExceptionUsed(exceptionIdentifier, mit);
    }
    return false;
  }

  private static boolean isExceptionUsed(IdentifierTree exceptionIdentifier, MethodInvocationTree mit) {
    ExceptionUsageVisitor visitor = new ExceptionUsageVisitor(exceptionIdentifier);
    mit.arguments().forEach(param -> param.accept(visitor));
    return visitor.isExceptionIdentifierUsed;
  }

  private static boolean isExceptionUsed(IdentifierTree exceptionIdentifier, ExpressionTree expressionTree) {
    ExceptionUsageVisitor visitor = new ExceptionUsageVisitor(exceptionIdentifier);
    expressionTree.accept(visitor);
    return visitor.isExceptionIdentifierUsed;
  }


  private static class ExceptionUsageVisitor extends BaseTreeVisitor {

    IdentifierTree exceptionIdentifier;
    boolean isExceptionIdentifierUsed = false;


    ExceptionUsageVisitor(IdentifierTree exceptionIdentifier) {
      this.exceptionIdentifier = exceptionIdentifier;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      if (!isExceptionIdentifierUsed && tree.name().equals(exceptionIdentifier.name())) {
        isExceptionIdentifierUsed = true;
      }
    }
  }
}
