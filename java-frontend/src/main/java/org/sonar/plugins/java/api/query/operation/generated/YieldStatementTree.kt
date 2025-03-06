package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.YieldStatementTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out YieldStatementTree>.expression() = func(YieldStatementTree::expression)
fun  OptionalBuilder<out YieldStatementTree>.expression() = func(YieldStatementTree::expression)
fun  ManyBuilder<out YieldStatementTree>.expression() = func(YieldStatementTree::expression)

fun  SingleBuilder<out YieldStatementTree>.semicolonToken() = func(YieldStatementTree::semicolonToken)
fun  OptionalBuilder<out YieldStatementTree>.semicolonToken() = func(YieldStatementTree::semicolonToken)
fun  ManyBuilder<out YieldStatementTree>.semicolonToken() = func(YieldStatementTree::semicolonToken)

fun  SingleBuilder<out YieldStatementTree>.yieldKeyword() = optFunc(YieldStatementTree::yieldKeyword)
fun  OptionalBuilder<out YieldStatementTree>.yieldKeyword() = optFunc(YieldStatementTree::yieldKeyword)
fun  ManyBuilder<out YieldStatementTree>.yieldKeyword() = optFunc(YieldStatementTree::yieldKeyword)
