package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.IfStatementTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out IfStatementTree>.closeParenToken() = func(IfStatementTree::closeParenToken)
fun  OptionalBuilder<out IfStatementTree>.closeParenToken() = func(IfStatementTree::closeParenToken)
fun  ManyBuilder<out IfStatementTree>.closeParenToken() = func(IfStatementTree::closeParenToken)

fun  SingleBuilder<out IfStatementTree>.condition() = func(IfStatementTree::condition)
fun  OptionalBuilder<out IfStatementTree>.condition() = func(IfStatementTree::condition)
fun  ManyBuilder<out IfStatementTree>.condition() = func(IfStatementTree::condition)

fun  SingleBuilder<out IfStatementTree>.elseKeyword() = optFunc(IfStatementTree::elseKeyword)
fun  OptionalBuilder<out IfStatementTree>.elseKeyword() = optFunc(IfStatementTree::elseKeyword)
fun  ManyBuilder<out IfStatementTree>.elseKeyword() = optFunc(IfStatementTree::elseKeyword)

fun  SingleBuilder<out IfStatementTree>.elseStatement() = optFunc(IfStatementTree::elseStatement)
fun  OptionalBuilder<out IfStatementTree>.elseStatement() = optFunc(IfStatementTree::elseStatement)
fun  ManyBuilder<out IfStatementTree>.elseStatement() = optFunc(IfStatementTree::elseStatement)

fun  SingleBuilder<out IfStatementTree>.ifKeyword() = func(IfStatementTree::ifKeyword)
fun  OptionalBuilder<out IfStatementTree>.ifKeyword() = func(IfStatementTree::ifKeyword)
fun  ManyBuilder<out IfStatementTree>.ifKeyword() = func(IfStatementTree::ifKeyword)

fun  SingleBuilder<out IfStatementTree>.openParenToken() = func(IfStatementTree::openParenToken)
fun  OptionalBuilder<out IfStatementTree>.openParenToken() = func(IfStatementTree::openParenToken)
fun  ManyBuilder<out IfStatementTree>.openParenToken() = func(IfStatementTree::openParenToken)

fun  SingleBuilder<out IfStatementTree>.thenStatement() = func(IfStatementTree::thenStatement)
fun  OptionalBuilder<out IfStatementTree>.thenStatement() = func(IfStatementTree::thenStatement)
fun  ManyBuilder<out IfStatementTree>.thenStatement() = func(IfStatementTree::thenStatement)
