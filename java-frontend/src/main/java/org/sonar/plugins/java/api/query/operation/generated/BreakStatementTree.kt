package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.BreakStatementTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out BreakStatementTree>.breakKeyword() = func(BreakStatementTree::breakKeyword)
fun  OptionalBuilder<out BreakStatementTree>.breakKeyword() = func(BreakStatementTree::breakKeyword)
fun  ManyBuilder<out BreakStatementTree>.breakKeyword() = func(BreakStatementTree::breakKeyword)

fun  SingleBuilder<out BreakStatementTree>.label() = optFunc(BreakStatementTree::label)
fun  OptionalBuilder<out BreakStatementTree>.label() = optFunc(BreakStatementTree::label)
fun  ManyBuilder<out BreakStatementTree>.label() = optFunc(BreakStatementTree::label)

fun  SingleBuilder<out BreakStatementTree>.semicolonToken() = func(BreakStatementTree::semicolonToken)
fun  OptionalBuilder<out BreakStatementTree>.semicolonToken() = func(BreakStatementTree::semicolonToken)
fun  ManyBuilder<out BreakStatementTree>.semicolonToken() = func(BreakStatementTree::semicolonToken)
