package org.sonar.java.checks.queryAPI

import org.sonar.check.Rule
import org.sonar.java.model.ExpressionUtils
import org.sonar.plugins.java.api.query.QueryRule
import org.sonar.plugins.java.api.query.operation.generated.TreeKind
import org.sonar.plugins.java.api.query.operation.generated.expression
import org.sonar.plugins.java.api.query.operation.generated.methodSelect
import org.sonar.plugins.java.api.query.operation.ofKind
import org.sonar.plugins.java.api.query.operation.report
import org.sonar.plugins.java.api.semantic.MethodMatchers
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree
import org.sonar.plugins.java.api.tree.MethodInvocationTree
import org.sonar.plugins.java.api.tree.Tree
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonarsource.astquery.operation.core.filter
import org.sonarsource.astquery.operation.core.isPresent
import org.sonarsource.astquery.operation.core.where

@Rule(key = "S4635")
class StringOffsetMethodsCheckKT : QueryRule {

  private val javaLangString: String = "java.lang.String"
  private val int: String = "int"
  private val substring: MethodMatchers = MethodMatchers.create()
    .ofTypes(javaLangString)
    .names("substring")
    .addParametersMatcher(int)
    .build()
  private val indexOf: MethodMatchers = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(javaLangString)
      .names("indexOf", "lastIndexOf")
      .addParametersMatcher(int)
      .build(),
    MethodMatchers.create()
      .ofTypes(javaLangString)
      .names("indexOf", "lastIndexOf", "startsWith")
      .addParametersMatcher(javaLangString)
      .build()
  )

  override fun createQuery(entry: ManyBuilder<Tree>) {
    entry
      .ofKind(TreeKind.METHOD_INVOCATION)
      .filter { invoke -> indexOf.matches(invoke.methodSymbol()) }
      .where { invoke ->
        invoke.methodSelect()
          .ofKind(TreeKind.MEMBER_SELECT)
          .expression()
          .ofKind(TreeKind.METHOD_INVOCATION)
          .filter { substring.matches(it) }
          .isPresent()
      }
      .report { context, issue ->
        context.reportIssue(
          this,
          ExpressionUtils.methodName((issue.methodSelect() as MemberSelectExpressionTree).expression() as MethodInvocationTree),
          issue,
          String.format("Replace \"%s\" with the overload that accepts an offset parameter.", issue.methodSymbol().name())
        )
      }
  }
}
