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
import org.sonar.java.checks.helpers.MethodTreeUtils
import org.sonar.java.reporting.FluentReporting
import org.sonar.plugins.java.api.JavaFileScannerContext
import org.sonar.plugins.java.api.query.QueryRule
import org.sonar.plugins.java.api.query.operation.generated.*
import org.sonar.plugins.java.api.query.operation.ofKind
import org.sonar.plugins.java.api.query.operation.report
import org.sonar.plugins.java.api.query.operation.subtree
import org.sonar.plugins.java.api.semantic.Symbol
import org.sonar.plugins.java.api.semantic.Type
import org.sonar.plugins.java.api.tree.MethodTree
import org.sonar.plugins.java.api.tree.Tree
import org.sonar.plugins.java.api.tree.TypeTree
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonarsource.astquery.operation.composite.exclude
import org.sonarsource.astquery.operation.composite.isPresent
import org.sonarsource.astquery.operation.core.*


@DeprecatedRuleKey(ruleKey = "S00112", repositoryKey = "squid")
@Rule(key = "S112")
class RawExceptionCheckKT : QueryRule {

  override fun createQuery(entry: ManyBuilder<Tree>) {

    val methods = entry
      .ofKind(TreeKind.METHOD, TreeKind.CONSTRUCTOR)
      .filter { (it.`is`(Tree.Kind.CONSTRUCTOR) || isNotOverridden(it)) }
      .filter { isNotMainMethod(it) }
      .filter { hasNoUnknownMethod(it) }

    val invalidThrownTypes = methods.scoped { method ->
      val thrownTypesInMethod = method
        .subtree()
        .let {
          it.ofKind(TreeKind.METHOD_INVOCATION).methodSymbol() union
            it.ofKind(TreeKind.NEW_CLASS).methodSymbol()
        }
        .filter { !it.isUnknown }
        .flatMap(Symbol.MethodSymbol::thrownTypes)
        .aggregate()

      method
        .throwsClauses()
        .filterRawException()
        .where { type ->
          type.symbolType()
            .exclude(thrownTypesInMethod)
            .isPresent()
        }
    }


    val directThrows = entry
      .ofKind(TreeKind.THROW_STATEMENT)
      .expression()
      .ofKind(TreeKind.NEW_CLASS)
      .identifier()
      .filter { isRawException(it.symbolType()) }

    invalidThrownTypes.report { context, tree -> reportIssue(context, tree) }
    directThrows.report { context, tree -> reportIssue(context, tree) }
  }

  private fun reportIssue(context: JavaFileScannerContext, tree: Tree) {
    (context as FluentReporting).newIssue()
      .forRule(this)
      .onTree(tree)
      .withMessage("Define and throw a dedicated exception instead of using a generic one.")
      .report()
  }

  companion object {
    private val RAW_EXCEPTIONS: List<String> = mutableListOf(
      "java.lang.Throwable",
      "java.lang.Error",
      "java.lang.Exception",
      "java.lang.RuntimeException"
    )

    private fun hasNoUnknownMethod(method: MethodTree): Boolean {
      val unknownMethodVisitor = MethodTreeUtils.MethodInvocationCollector { obj: Symbol.MethodSymbol -> obj.isUnknown }
      method.accept(unknownMethodVisitor)
      return unknownMethodVisitor.invocationTree.isEmpty()
    }

    private fun ManyBuilder<TypeTree>.filterRawException() =
      filter { isRawException(it.symbolType()) }

    private fun isRawException(type: Type): Boolean {
      return RAW_EXCEPTIONS.stream().anyMatch { type.`is`(it) }
    }

    private fun isNotOverridden(tree: MethodTree): Boolean {
      return java.lang.Boolean.FALSE == tree.isOverriding()
    }

    private fun isNotMainMethod(tree: MethodTree): Boolean {
      return !MethodTreeUtils.isMainMethod(tree)
    }
  }
}
