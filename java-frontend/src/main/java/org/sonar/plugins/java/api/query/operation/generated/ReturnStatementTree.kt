package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ReturnStatementTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ReturnStatementTree>.expression() = optFunc(ReturnStatementTree::expression)
fun  OptionalBuilder<out ReturnStatementTree>.expression() = optFunc(ReturnStatementTree::expression)
fun  ManyBuilder<out ReturnStatementTree>.expression() = optFunc(ReturnStatementTree::expression)

fun  SingleBuilder<out ReturnStatementTree>.returnKeyword() = func(ReturnStatementTree::returnKeyword)
fun  OptionalBuilder<out ReturnStatementTree>.returnKeyword() = func(ReturnStatementTree::returnKeyword)
fun  ManyBuilder<out ReturnStatementTree>.returnKeyword() = func(ReturnStatementTree::returnKeyword)

fun  SingleBuilder<out ReturnStatementTree>.semicolonToken() = func(ReturnStatementTree::semicolonToken)
fun  OptionalBuilder<out ReturnStatementTree>.semicolonToken() = func(ReturnStatementTree::semicolonToken)
fun  ManyBuilder<out ReturnStatementTree>.semicolonToken() = func(ReturnStatementTree::semicolonToken)
