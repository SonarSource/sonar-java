package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.WhileStatementTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out WhileStatementTree>.closeParenToken() = func(WhileStatementTree::closeParenToken)
fun  OptionalBuilder<out WhileStatementTree>.closeParenToken() = func(WhileStatementTree::closeParenToken)
fun  ManyBuilder<out WhileStatementTree>.closeParenToken() = func(WhileStatementTree::closeParenToken)

fun  SingleBuilder<out WhileStatementTree>.condition() = func(WhileStatementTree::condition)
fun  OptionalBuilder<out WhileStatementTree>.condition() = func(WhileStatementTree::condition)
fun  ManyBuilder<out WhileStatementTree>.condition() = func(WhileStatementTree::condition)

fun  SingleBuilder<out WhileStatementTree>.openParenToken() = func(WhileStatementTree::openParenToken)
fun  OptionalBuilder<out WhileStatementTree>.openParenToken() = func(WhileStatementTree::openParenToken)
fun  ManyBuilder<out WhileStatementTree>.openParenToken() = func(WhileStatementTree::openParenToken)

fun  SingleBuilder<out WhileStatementTree>.statement() = func(WhileStatementTree::statement)
fun  OptionalBuilder<out WhileStatementTree>.statement() = func(WhileStatementTree::statement)
fun  ManyBuilder<out WhileStatementTree>.statement() = func(WhileStatementTree::statement)

fun  SingleBuilder<out WhileStatementTree>.whileKeyword() = func(WhileStatementTree::whileKeyword)
fun  OptionalBuilder<out WhileStatementTree>.whileKeyword() = func(WhileStatementTree::whileKeyword)
fun  ManyBuilder<out WhileStatementTree>.whileKeyword() = func(WhileStatementTree::whileKeyword)
