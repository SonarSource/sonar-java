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
import java.util.Optional;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.ReassignmentFinder.getInitializerOrExpression;
import static org.sonar.java.checks.helpers.ReassignmentFinder.getReassignments;

@Rule(key = "S2077")
public class SQLInjectionCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_SQL_STATEMENT = "java.sql.Statement";
  private static final String JAVA_SQL_CONNECTION = "java.sql.Connection";
  private static final String SPRING_JDBC_OPERATIONS = "org.springframework.jdbc.core.JdbcOperations";

  private static final MethodMatcherCollection SQL_INJECTION_SUSPECTS = MethodMatcherCollection.create(
    MethodMatcher.create().callSite(TypeCriteria.subtypeOf("org.hibernate.Session")).name("createQuery").withAnyParameters(),
    MethodMatcher.create().callSite(TypeCriteria.subtypeOf("org.hibernate.Session")).name("createSQLQuery").withAnyParameters(),

    matcherBuilder(JAVA_SQL_STATEMENT).name("executeQuery").withAnyParameters(),
    matcherBuilder(JAVA_SQL_STATEMENT).name("execute").withAnyParameters(),
    matcherBuilder(JAVA_SQL_STATEMENT).name("executeUpdate").withAnyParameters(),
    matcherBuilder(JAVA_SQL_STATEMENT).name("executeLargeUpdate").withAnyParameters(),
    matcherBuilder(JAVA_SQL_STATEMENT).name("addBatch").withAnyParameters(),

    matcherBuilder(JAVA_SQL_CONNECTION).name("prepareStatement").withAnyParameters(),
    matcherBuilder(JAVA_SQL_CONNECTION).name("prepareCall").withAnyParameters(),
    matcherBuilder(JAVA_SQL_CONNECTION).name("nativeSQL").withAnyParameters(),

    MethodMatcher.create().typeDefinition("javax.persistence.EntityManager").name("createNativeQuery").withAnyParameters(),
    MethodMatcher.create().typeDefinition("javax.persistence.EntityManager").name("createQuery").withAnyParameters(),

    matcherBuilder(SPRING_JDBC_OPERATIONS).name("batchUpdate").withAnyParameters(),
    matcherBuilder(SPRING_JDBC_OPERATIONS).name("execute").withAnyParameters(),
    matcherBuilder(SPRING_JDBC_OPERATIONS).name("query").withAnyParameters(),
    matcherBuilder(SPRING_JDBC_OPERATIONS).name("queryForList").withAnyParameters(),
    matcherBuilder(SPRING_JDBC_OPERATIONS).name("queryForMap").withAnyParameters(),
    matcherBuilder(SPRING_JDBC_OPERATIONS).name("queryForObject").withAnyParameters(),
    matcherBuilder(SPRING_JDBC_OPERATIONS).name("queryForRowSet").withAnyParameters(),
    matcherBuilder(SPRING_JDBC_OPERATIONS).name("queryForInt").withAnyParameters(),
    matcherBuilder(SPRING_JDBC_OPERATIONS).name("queryForLong").withAnyParameters(),
    matcherBuilder(SPRING_JDBC_OPERATIONS).name("update").withAnyParameters(),
    MethodMatcher.create().typeDefinition("org.springframework.jdbc.core.PreparedStatementCreatorFactory").name("<init>").withAnyParameters(),
    MethodMatcher.create().typeDefinition("org.springframework.jdbc.core.PreparedStatementCreatorFactory").name("newPreparedStatementCreator").withAnyParameters(),

    matcherBuilder("javax.jdo.PersistenceManager").name("newQuery").withAnyParameters(),
    matcherBuilder("javax.jdo.Query").name("setFilter").withAnyParameters(),
    matcherBuilder("javax.jdo.Query").name("setGrouping").withAnyParameters()
  );

  private static MethodMatcher matcherBuilder(String typeFQN) {
    return MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(typeFQN));
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (anyMatch(tree)) {
      Optional<ExpressionTree> sqlStringArg = arguments(tree)
        .filter(arg -> arg.symbolType().is("java.lang.String"))
        .findFirst();
      sqlStringArg.filter(SQLInjectionCheck::isDynamicString)
        .ifPresent(arg -> reportIssue(arg, "Ensure that string concatenation is required and safe for this SQL query."));
    }
  }

  private static Stream<ExpressionTree> arguments(Tree methodTree) {
    if (methodTree.is(Tree.Kind.METHOD_INVOCATION)) {
      return ((MethodInvocationTree) methodTree).arguments().stream();
    }
    if (methodTree.is(Tree.Kind.NEW_CLASS)) {
      return ((NewClassTree) methodTree).arguments().stream();
    }
    return Stream.empty();
  }

  private static boolean anyMatch(Tree tree) {
    if (!hasArguments(tree)) {
      return false;
    }
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      return SQL_INJECTION_SUSPECTS.anyMatch((NewClassTree) tree);
    }
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      return SQL_INJECTION_SUSPECTS.anyMatch((MethodInvocationTree) tree);
    }
    return false;
  }

  private static boolean hasArguments(Tree tree) {
    return arguments(tree).findAny().isPresent();
  }

  private static boolean isDynamicString(ExpressionTree arg) {
    if (arg.is(Tree.Kind.PLUS_ASSIGNMENT)) {
      return ConstantUtils.resolveAsConstant(((AssignmentExpressionTree)arg).expression()) == null;
    }
    if (arg.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) arg).symbol();
      ExpressionTree initializerOrExpression = getInitializerOrExpression(symbol.declaration());
      return (initializerOrExpression != null && isDynamicString(initializerOrExpression)) || getReassignments(symbol.owner().declaration(), symbol.usages()).stream()
        .anyMatch(SQLInjectionCheck::isDynamicString);
    }
    return arg.is(Tree.Kind.PLUS) && ConstantUtils.resolveAsConstant(arg) == null;
  }
}
