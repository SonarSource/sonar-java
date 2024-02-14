/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

/**
 * Rule S6905 flags issues a query that selects all fields is executed.
 * The intent of this rule is to avoid wasting resources by fetching unnecessary fields.
 * <p>
 * The implemented logic checks if a query containing the "SELECT *" pattern is provided to a method that executes a query.
 */
@Rule(key = "S6905")
public class QueryOnlyRequiredFieldsCheck extends IssuableSubscriptionVisitor {

  public static final String JAVA_LANG_STRING = "java.lang.String";

  private static final MethodMatchers SQL_QUERY_METHODS = MethodMatchers.create()
    .ofTypes("java.sql.Connection", "java.sql.Statement", "java.sql.PreparedStatement", "java.sql.CallableStatement")
    .names("prepareStatement", "prepareCall", "execute", "executeQuery")
    .addParametersMatcher(JAVA_LANG_STRING)
    .addParametersMatcher(JAVA_LANG_STRING, "int")
    .addParametersMatcher(JAVA_LANG_STRING, "int[]")
    .addParametersMatcher(JAVA_LANG_STRING, "java.lang.String[]")
    .build();

  private static final Predicate<String> SELECT_FROM_REGEXP = compile("select\\s+\\*\\s+from", CASE_INSENSITIVE).asPredicate();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;

    if (SQL_QUERY_METHODS.matches(mit)) {
      mit.arguments().stream()
        .map(QueryOnlyRequiredFieldsCheck::extractQuery)
        .filter(Objects::nonNull)
        .filter(arg -> SELECT_FROM_REGEXP.test(arg.value()))
        .forEach(arg -> reportIssue(arg, "Don't use the query \"SELECT *\"."));
    }
  }

  private static LiteralTree extractQuery(ExpressionTree expression) {
    switch (expression.kind()) {
      case STRING_LITERAL:
        return (LiteralTree) expression;
      case IDENTIFIER:
        return Optional.of(expression)
          .map(IdentifierTree.class::cast)
          .map(IdentifierTree::symbol)
          .map(Symbol::declaration)
          .filter(VariableTree.class::isInstance)
          .map(VariableTree.class::cast)
          .map(VariableTree::initializer)
          .filter(LiteralTree.class::isInstance)
          .map(LiteralTree.class::cast)
          .orElse(null);
      default:
        return null;
    }
  }
}
