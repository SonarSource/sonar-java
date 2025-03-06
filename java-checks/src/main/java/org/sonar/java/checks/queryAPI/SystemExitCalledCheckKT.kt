package org.sonar.java.checks.queryAPI

import org.sonar.check.Rule
import org.sonar.java.checks.helpers.MethodTreeUtils
import org.sonar.java.model.ExpressionUtils
import org.sonar.plugins.java.api.query.QueryRule
import org.sonar.plugins.java.api.query.operation.generated.TreeKind
import org.sonar.plugins.java.api.query.operation.ofKind
import org.sonar.plugins.java.api.query.operation.parents
import org.sonar.plugins.java.api.query.operation.report
import org.sonar.plugins.java.api.semantic.MethodMatchers
import org.sonar.plugins.java.api.tree.MethodInvocationTree
import org.sonar.plugins.java.api.tree.Tree
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonarsource.astquery.operation.composite.first
import org.sonarsource.astquery.operation.composite.notPresent
import org.sonarsource.astquery.operation.core.filter
import org.sonarsource.astquery.operation.core.where

@Rule(key = "S1147")
class SystemExitCalledCheckKT : QueryRule {

  private val exitMethods = MethodMatchers.create()
    .ofTypes("java.lang.System", "java.lang.Runtime")
    .names("exit", "halt")
    .addParametersMatcher("int")
    .build()

  override fun createQuery(entry: ManyBuilder<Tree>) {
    entry.ofKind(TreeKind.METHOD_INVOCATION)
      .filter(exitMethods::matches)
      .isNotInMainMethod()
      .report { context, invocation ->
        val methodName = ExpressionUtils.methodName(invocation).name()
        context.reportIssue(this, invocation.methodSelect(), "Remove this call to \"$methodName\" or ensure it is really required.")
      }
  }

  private fun ManyBuilder<MethodInvocationTree>.isNotInMainMethod() =
    where { tree ->
      tree.parents()
        .ofKind(TreeKind.METHOD,
          TreeKind.CLASS,
          TreeKind.INTERFACE,
          TreeKind.ENUM,
          TreeKind.ANNOTATION_TYPE,
          TreeKind.LAMBDA_EXPRESSION,
          TreeKind.CONSTRUCTOR,
          TreeKind.INITIALIZER,
          TreeKind.STATIC_INITIALIZER,
          TreeKind.RECORD)
        .first()
        .ofKind(TreeKind.METHOD)
        .filter { MethodTreeUtils.isMainMethod(it) }
        .notPresent()
    }
}
