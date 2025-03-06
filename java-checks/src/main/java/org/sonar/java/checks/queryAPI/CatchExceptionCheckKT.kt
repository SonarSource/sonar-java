package org.sonar.java.checks.queryAPI

import org.sonar.check.Rule
import org.sonar.plugins.java.api.query.QueryRule
import org.sonar.plugins.java.api.query.operation.generated.TreeKind
import org.sonar.plugins.java.api.query.operation.generated.identifier
import org.sonar.plugins.java.api.query.operation.generated.methodSymbol
import org.sonar.plugins.java.api.query.operation.generated.name
import org.sonar.plugins.java.api.query.operation.generated.parameter
import org.sonar.plugins.java.api.query.operation.generated.resourceList
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

@Rule(key = "S2221")
class CatchExceptionCheckKT : QueryRule {

  override fun createQuery(entry: ManyBuilder<Tree>) {
    val catchType = entry.ofKind(TreeKind.CATCH)
      .where { catch ->
        val tryStatement = catch
          .parents()
          .ofKind(TreeKind.TRY_STATEMENT).first()

        tryStatement.noResources()
          .combine(tryStatement.noExceptionThrownInBlock()) { a, b -> a && b }
      }
      .parameter()
      .type()

    // For Union Type, flatten the types
    val types = (catchType.ofKind(TreeKind.UNION_TYPE).typeAlternatives() union catchType)

    types.ofKind(TreeKind.IDENTIFIER)
      .where { it.name().eq("Exception") }
      .report { context, tree ->
        context.reportIssue(this, tree, "Catch a list of specific exception subtypes instead.");
      }

    types.ofKind(TreeKind.MEMBER_SELECT)
      .filter { it.symbolType().`is`("java.lang.Exception") }
      .identifier()
      .report { context, tree ->
        context.reportIssue(this, tree, "Catch a list of specific exception subtypes instead.");
      }
  }

  private fun OptionalBuilder<TryStatementTree>.noResources() =
    resourceList().noneExists()

  private fun OptionalBuilder<TryStatementTree>.noExceptionThrownInBlock(): SingleBuilder<Boolean> {
    val symbols = subtree()
        .let {
          it.ofKind(TreeKind.METHOD_INVOCATION).methodSymbol() union
            it.ofKind(TreeKind.NEW_CLASS).methodSymbol()
        }

    // If the method is unknown, we can't check the thrown types we need to assume it does
    val unknownMethods = symbols.filter { it.isUnknown }
    val thowsException = symbols.filter { !it.isUnknown }
      .flatMap(Symbol.MethodSymbol::thrownTypes)
      .filter { type -> type.`is`("java.lang.Exception") }

    return (unknownMethods union thowsException).noneExists()
  }
}
