package org.sonar.java.checks.queryAPI

import org.sonar.check.Rule
import org.sonar.plugins.java.api.query.QueryRule
import org.sonar.plugins.java.api.query.operation.generated.TreeKind
import org.sonar.plugins.java.api.query.operation.generated.identifier
import org.sonar.plugins.java.api.query.operation.generated.name
import org.sonar.plugins.java.api.query.operation.generated.parameter
import org.sonar.plugins.java.api.query.operation.generated.type
import org.sonar.plugins.java.api.query.operation.generated.typeAlternatives
import org.sonar.plugins.java.api.query.operation.ofKind
import org.sonar.plugins.java.api.query.operation.report
import org.sonar.plugins.java.api.tree.Tree
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonarsource.astquery.operation.composite.eq
import org.sonarsource.astquery.operation.core.filter
import org.sonarsource.astquery.operation.core.union
import org.sonarsource.astquery.operation.core.where

@Rule(key = "S1696")
class CatchNPECheckKT : QueryRule {

  override fun createQuery(entry: ManyBuilder<Tree>) {
    val catchType = entry.ofKind(TreeKind.CATCH).parameter().type()

    // For Union Type, flatten the types
    val types = (catchType.ofKind(TreeKind.UNION_TYPE).typeAlternatives() union catchType)

    types.ofKind(TreeKind.IDENTIFIER)
      .where { it.name().eq("NullPointerException") }
      .report { context, tree ->
        context.reportIssue(this, tree, "Avoid catching NullPointerException.");
      }

    types.ofKind(TreeKind.MEMBER_SELECT)
      .filter { it.symbolType().`is`("java.lang.NullPointerException") }
      .identifier()
      .report { context, tree ->
        context.reportIssue(this, tree, "Avoid catching NullPointerException.");
      }
  }
}
