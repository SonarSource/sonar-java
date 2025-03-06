package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out SynchronizedStatementTree>.block() = func(SynchronizedStatementTree::block)
fun  OptionalBuilder<out SynchronizedStatementTree>.block() = func(SynchronizedStatementTree::block)
fun  ManyBuilder<out SynchronizedStatementTree>.block() = func(SynchronizedStatementTree::block)

fun  SingleBuilder<out SynchronizedStatementTree>.closeParenToken() = func(SynchronizedStatementTree::closeParenToken)
fun  OptionalBuilder<out SynchronizedStatementTree>.closeParenToken() = func(SynchronizedStatementTree::closeParenToken)
fun  ManyBuilder<out SynchronizedStatementTree>.closeParenToken() = func(SynchronizedStatementTree::closeParenToken)

fun  SingleBuilder<out SynchronizedStatementTree>.expression() = func(SynchronizedStatementTree::expression)
fun  OptionalBuilder<out SynchronizedStatementTree>.expression() = func(SynchronizedStatementTree::expression)
fun  ManyBuilder<out SynchronizedStatementTree>.expression() = func(SynchronizedStatementTree::expression)

fun  SingleBuilder<out SynchronizedStatementTree>.openParenToken() = func(SynchronizedStatementTree::openParenToken)
fun  OptionalBuilder<out SynchronizedStatementTree>.openParenToken() = func(SynchronizedStatementTree::openParenToken)
fun  ManyBuilder<out SynchronizedStatementTree>.openParenToken() = func(SynchronizedStatementTree::openParenToken)

fun  SingleBuilder<out SynchronizedStatementTree>.synchronizedKeyword() = func(SynchronizedStatementTree::synchronizedKeyword)
fun  OptionalBuilder<out SynchronizedStatementTree>.synchronizedKeyword() = func(SynchronizedStatementTree::synchronizedKeyword)
fun  ManyBuilder<out SynchronizedStatementTree>.synchronizedKeyword() = func(SynchronizedStatementTree::synchronizedKeyword)
