package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.AssertStatementTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out AssertStatementTree>.assertKeyword() = func(AssertStatementTree::assertKeyword)
fun  OptionalBuilder<out AssertStatementTree>.assertKeyword() = func(AssertStatementTree::assertKeyword)
fun  ManyBuilder<out AssertStatementTree>.assertKeyword() = func(AssertStatementTree::assertKeyword)

fun  SingleBuilder<out AssertStatementTree>.colonToken() = optFunc(AssertStatementTree::colonToken)
fun  OptionalBuilder<out AssertStatementTree>.colonToken() = optFunc(AssertStatementTree::colonToken)
fun  ManyBuilder<out AssertStatementTree>.colonToken() = optFunc(AssertStatementTree::colonToken)

fun  SingleBuilder<out AssertStatementTree>.condition() = func(AssertStatementTree::condition)
fun  OptionalBuilder<out AssertStatementTree>.condition() = func(AssertStatementTree::condition)
fun  ManyBuilder<out AssertStatementTree>.condition() = func(AssertStatementTree::condition)

fun  SingleBuilder<out AssertStatementTree>.detail() = optFunc(AssertStatementTree::detail)
fun  OptionalBuilder<out AssertStatementTree>.detail() = optFunc(AssertStatementTree::detail)
fun  ManyBuilder<out AssertStatementTree>.detail() = optFunc(AssertStatementTree::detail)

fun  SingleBuilder<out AssertStatementTree>.semicolonToken() = func(AssertStatementTree::semicolonToken)
fun  OptionalBuilder<out AssertStatementTree>.semicolonToken() = func(AssertStatementTree::semicolonToken)
fun  ManyBuilder<out AssertStatementTree>.semicolonToken() = func(AssertStatementTree::semicolonToken)
