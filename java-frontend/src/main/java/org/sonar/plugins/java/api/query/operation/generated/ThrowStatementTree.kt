package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ThrowStatementTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ThrowStatementTree>.expression() = func(ThrowStatementTree::expression)
fun  OptionalBuilder<out ThrowStatementTree>.expression() = func(ThrowStatementTree::expression)
fun  ManyBuilder<out ThrowStatementTree>.expression() = func(ThrowStatementTree::expression)

fun  SingleBuilder<out ThrowStatementTree>.semicolonToken() = func(ThrowStatementTree::semicolonToken)
fun  OptionalBuilder<out ThrowStatementTree>.semicolonToken() = func(ThrowStatementTree::semicolonToken)
fun  ManyBuilder<out ThrowStatementTree>.semicolonToken() = func(ThrowStatementTree::semicolonToken)

fun  SingleBuilder<out ThrowStatementTree>.throwKeyword() = func(ThrowStatementTree::throwKeyword)
fun  OptionalBuilder<out ThrowStatementTree>.throwKeyword() = func(ThrowStatementTree::throwKeyword)
fun  ManyBuilder<out ThrowStatementTree>.throwKeyword() = func(ThrowStatementTree::throwKeyword)
