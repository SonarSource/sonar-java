package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ContinueStatementTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out ContinueStatementTree>.continueKeyword() = func(ContinueStatementTree::continueKeyword)
fun  OptionalBuilder<out ContinueStatementTree>.continueKeyword() = func(ContinueStatementTree::continueKeyword)
fun  ManyBuilder<out ContinueStatementTree>.continueKeyword() = func(ContinueStatementTree::continueKeyword)

fun  SingleBuilder<out ContinueStatementTree>.label() = optFunc(ContinueStatementTree::label)
fun  OptionalBuilder<out ContinueStatementTree>.label() = optFunc(ContinueStatementTree::label)
fun  ManyBuilder<out ContinueStatementTree>.label() = optFunc(ContinueStatementTree::label)

fun  SingleBuilder<out ContinueStatementTree>.semicolonToken() = func(ContinueStatementTree::semicolonToken)
fun  OptionalBuilder<out ContinueStatementTree>.semicolonToken() = func(ContinueStatementTree::semicolonToken)
fun  ManyBuilder<out ContinueStatementTree>.semicolonToken() = func(ContinueStatementTree::semicolonToken)
