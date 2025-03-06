package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.DoWhileStatementTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out DoWhileStatementTree>.closeParenToken() = func(DoWhileStatementTree::closeParenToken)
fun  OptionalBuilder<out DoWhileStatementTree>.closeParenToken() = func(DoWhileStatementTree::closeParenToken)
fun  ManyBuilder<out DoWhileStatementTree>.closeParenToken() = func(DoWhileStatementTree::closeParenToken)

fun  SingleBuilder<out DoWhileStatementTree>.condition() = func(DoWhileStatementTree::condition)
fun  OptionalBuilder<out DoWhileStatementTree>.condition() = func(DoWhileStatementTree::condition)
fun  ManyBuilder<out DoWhileStatementTree>.condition() = func(DoWhileStatementTree::condition)

fun  SingleBuilder<out DoWhileStatementTree>.doKeyword() = func(DoWhileStatementTree::doKeyword)
fun  OptionalBuilder<out DoWhileStatementTree>.doKeyword() = func(DoWhileStatementTree::doKeyword)
fun  ManyBuilder<out DoWhileStatementTree>.doKeyword() = func(DoWhileStatementTree::doKeyword)

fun  SingleBuilder<out DoWhileStatementTree>.openParenToken() = func(DoWhileStatementTree::openParenToken)
fun  OptionalBuilder<out DoWhileStatementTree>.openParenToken() = func(DoWhileStatementTree::openParenToken)
fun  ManyBuilder<out DoWhileStatementTree>.openParenToken() = func(DoWhileStatementTree::openParenToken)

fun  SingleBuilder<out DoWhileStatementTree>.semicolonToken() = func(DoWhileStatementTree::semicolonToken)
fun  OptionalBuilder<out DoWhileStatementTree>.semicolonToken() = func(DoWhileStatementTree::semicolonToken)
fun  ManyBuilder<out DoWhileStatementTree>.semicolonToken() = func(DoWhileStatementTree::semicolonToken)

fun  SingleBuilder<out DoWhileStatementTree>.statement() = func(DoWhileStatementTree::statement)
fun  OptionalBuilder<out DoWhileStatementTree>.statement() = func(DoWhileStatementTree::statement)
fun  ManyBuilder<out DoWhileStatementTree>.statement() = func(DoWhileStatementTree::statement)

fun  SingleBuilder<out DoWhileStatementTree>.whileKeyword() = func(DoWhileStatementTree::whileKeyword)
fun  OptionalBuilder<out DoWhileStatementTree>.whileKeyword() = func(DoWhileStatementTree::whileKeyword)
fun  ManyBuilder<out DoWhileStatementTree>.whileKeyword() = func(DoWhileStatementTree::whileKeyword)
