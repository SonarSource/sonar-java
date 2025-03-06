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
package org.sonar.java.checks.queryAPI

import org.sonar.check.Rule
import org.sonar.java.checks.helpers.ReassignmentFinder
import org.sonar.plugins.java.api.JavaFileScannerContext
import org.sonar.plugins.java.api.query.QueryRule
import org.sonar.plugins.java.api.query.operation.composite.*
import org.sonar.plugins.java.api.query.operation.composite.ifTrueUse
import org.sonar.plugins.java.api.semantic.MethodMatchers
import org.sonar.plugins.java.api.query.operation.generated.TreeKind
import org.sonar.plugins.java.api.query.operation.generated.TreeKind.Companion.METHOD_INVOCATION
import org.sonar.plugins.java.api.query.operation.generated.TreeKind.Companion.NEW_CLASS
import org.sonar.plugins.java.api.query.operation.generated.arguments
import org.sonar.plugins.java.api.query.operation.generated.asConstant
import org.sonar.plugins.java.api.query.operation.generated.identifier
import org.sonar.plugins.java.api.query.operation.generated.listArguments
import org.sonar.plugins.java.api.query.operation.generated.methodSymbol
import org.sonar.plugins.java.api.query.operation.generated.name
import org.sonar.plugins.java.api.query.operation.generated.parameter
import org.sonar.plugins.java.api.query.operation.generated.resourceList
import org.sonar.plugins.java.api.query.operation.generated.symbol
import org.sonar.plugins.java.api.query.operation.generated.type
import org.sonar.plugins.java.api.query.operation.generated.typeAlternatives
import org.sonar.plugins.java.api.query.operation.ofKind
import org.sonar.plugins.java.api.query.operation.parents
import org.sonar.plugins.java.api.query.operation.report
import org.sonar.plugins.java.api.query.operation.subtree
import org.sonar.plugins.java.api.semantic.Symbol
import org.sonar.plugins.java.api.tree.Tree
import org.sonar.plugins.java.api.tree.TryStatementTree
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.composite.eq
import org.sonarsource.astquery.operation.composite.first
import org.sonarsource.astquery.operation.composite.noneExists
import org.sonarsource.astquery.operation.core.combine
import org.sonarsource.astquery.operation.core.filter
import org.sonarsource.astquery.operation.core.flatMap
import org.sonarsource.astquery.operation.core.union
import org.sonarsource.astquery.operation.core.where
import org.sonar.plugins.java.api.tree.*
import org.sonarsource.astquery.operation.composite.exists
import org.sonarsource.astquery.operation.composite.flatten
import org.sonarsource.astquery.operation.composite.isPresent
import org.sonarsource.astquery.operation.composite.zip
import org.sonarsource.astquery.operation.core.filterNonNull
import org.sonarsource.astquery.operation.core.groupFilterWith
import org.sonarsource.astquery.operation.core.map
import org.sonarsource.astquery.operation.core.scoped

@Rule(key = "S2077")
class SQLInjectionCheckKT : QueryRule {

  companion object {
    private const val JAVA_SQL_STATEMENT = "java.sql.Statement"
    private const val JAVA_SQL_CONNECTION = "java.sql.Connection"
    private const val SPRING_JDBC_OPERATIONS = "org.springframework.jdbc.core.JdbcOperations"

    private val SQL_INJECTION_SUSPECTS: MethodMatchers = MethodMatchers.or(
      MethodMatchers.create()
        .ofSubTypes("org.hibernate.Session")
        .names("createQuery", "createSQLQuery")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes(JAVA_SQL_STATEMENT)
        .names("executeQuery", "execute", "executeUpdate", "executeLargeUpdate", "addBatch")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes(JAVA_SQL_CONNECTION)
        .names("prepareStatement", "prepareCall", "nativeSQL")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofTypes("javax.persistence.EntityManager")
        .names("createNativeQuery", "createQuery")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes(SPRING_JDBC_OPERATIONS, "org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate")
        .names(
          "batchUpdate", "execute", "query", "queryForList", "queryForMap", "queryForObject",
          "queryForRowSet", "queryForInt", "queryForLong", "update", "queryForStream"
        )
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofTypes("org.springframework.jdbc.core.PreparedStatementCreatorFactory")
        .names(MethodMatchers.CONSTRUCTOR, "newPreparedStatementCreator")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes("javax.jdo.PersistenceManager")
        .names("newQuery")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes("javax.jdo.Query")
        .names("setFilter", "setGrouping")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes("org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl")
        .names("setAuthoritiesByUsernameQuery", "setGroupAuthoritiesByUsernameQuery", "setUsersByUsernameQuery")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes("org.springframework.security.provisioning.JdbcUserDetailsManager")
        .names(
          "setChangePasswordSql",
          "setCreateAuthoritySql",
          "setCreateUserSql",
          "setDeleteGroupAuthoritiesSql",
          "setDeleteGroupAuthoritySql",
          "setDeleteGroupMemberSql",
          "setDeleteGroupMembersSql",
          "setDeleteGroupSql",
          "setDeleteUserAuthoritiesSql",
          "setDeleteUserSql",
          "setFindAllGroupsSql",
          "setFindGroupIdSql",
          "setFindUsersInGroupSql",
          "setGroupAuthoritiesSql",
          "setInsertGroupAuthoritySql",
          "setInsertGroupMemberSql",
          "setInsertGroupSql",
          "setRenameGroupSql",
          "setUpdateUserSql",
          "setUserExistsSql"
        )
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofSubTypes("org.springframework.jdbc.core.simple.JdbcClient")
        .names("sql")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofTypes("org.springframework.data.r2dbc.repository.query.StringBasedR2dbcQuery")
        .names(MethodMatchers.CONSTRUCTOR)
        .withAnyParameters()
        .build()
    )

    private const val MAIN_MESSAGE = "Make sure using a dynamically formatted SQL query is safe here."

    private fun secondaryLocations(
      initializerOrExpression: ExpressionTree?,
      reassignments: List<AssignmentExpressionTree>,
      identifierName: String
    ): List<JavaFileScannerContext.Location> {
      val secondaryLocations = reassignments.map { assignment ->
          JavaFileScannerContext.Location(
            String.format("SQL Query is assigned to '%s'", getVariableName(assignment)),
            assignment.expression()
          )
        }.toMutableList()

      if (initializerOrExpression != null) {
        secondaryLocations.add(
          JavaFileScannerContext.Location(
            String.format(
              "SQL Query is dynamically formatted and assigned to '%s'",
              identifierName
            ),
            initializerOrExpression
          )
        )
      }
      return secondaryLocations
    }

    private fun getVariableName(assignment: AssignmentExpressionTree): String {
      val variable = assignment.variable()
      return (variable as IdentifierTree).name()
    }

    private fun isDynamicPlusAssignment(arg: ExpressionTree): Boolean {
      val value = arg.`is`(Tree.Kind.PLUS_ASSIGNMENT) && !(arg as AssignmentExpressionTree).expression()
        .asConstant().isPresent
      return value
    }
  }

  override fun createQuery(entry: ManyBuilder<Tree>) {
    val invocationArguments = entry
      .ofKind(METHOD_INVOCATION)
      .where { it.arguments().exists() } // HasArguments
      .filter { tree -> SQL_INJECTION_SUSPECTS.matches(tree) }
      .listArguments()

    val newClassArguments = entry
      .ofKind(NEW_CLASS)
      .where { it.arguments().exists() } // HasArguments
      .filter { tree -> SQL_INJECTION_SUSPECTS.matches(tree) }
      .listArguments()

    val sqlString = (invocationArguments union newClassArguments)
      .scoped {
        it.flatten()
          .filter { it.symbolType().`is`("java.lang.String") }
          .first()
      }

    val directConcat = sqlString
      .ofKind(TreeKind.PLUS)
      .where { it.asConstant().map { it.isEmpty } }

    val indirectConcat = sqlString
      .ofKind(TreeKind.IDENTIFIER)
      .groupFilterWith { arg ->
        // Get the initializer or expression of the symbol
        val initializer = arg
          .symbol()
          .map { symbol -> ReassignmentFinder.getInitializerOrExpression(symbol.declaration()) }

        // Find all reassignments of the symbol
        val reassignments = arg
          .symbol()
          .map { symbol -> ReassignmentFinder.getReassignments(symbol.owner()?.declaration(), symbol.usages()) }

        val hasValidInitializer = initializer
          .filterNonNull()
          .ofKind(TreeKind.PLUS)
          .where { it.asConstant().map { it.isEmpty } }
          .isPresent()

        val hasValidReassignment = reassignments.flatten()
          .filter { isDynamicPlusAssignment(it) }
          .exists()

        hasValidInitializer.combine(hasValidReassignment) { a, b -> a || b }
          .ifTrueUse(initializer.zip(reassignments))
      }

    directConcat.report { context, arg ->
      context.reportIssue(this, arg, MAIN_MESSAGE)
    }

    indirectConcat.report { context, (arg, params) ->
      val (initializer, reassignments) = params
      context.reportIssue(this, arg, MAIN_MESSAGE, secondaryLocations(initializer, reassignments, arg.name()), null)
    }
  }
}
