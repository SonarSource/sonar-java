package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ExpressionStatementTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ExpressionStatementTree>.expression() = func(ExpressionStatementTree::expression)
fun  OptionalBuilder<out ExpressionStatementTree>.expression() = func(ExpressionStatementTree::expression)
fun  ManyBuilder<out ExpressionStatementTree>.expression() = func(ExpressionStatementTree::expression)

fun  SingleBuilder<out ExpressionStatementTree>.semicolonToken() = func(ExpressionStatementTree::semicolonToken)
fun  OptionalBuilder<out ExpressionStatementTree>.semicolonToken() = func(ExpressionStatementTree::semicolonToken)
fun  ManyBuilder<out ExpressionStatementTree>.semicolonToken() = func(ExpressionStatementTree::semicolonToken)
