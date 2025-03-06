package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.TryStatementTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out TryStatementTree>.block() = func(TryStatementTree::block)
fun  OptionalBuilder<out TryStatementTree>.block() = func(TryStatementTree::block)
fun  ManyBuilder<out TryStatementTree>.block() = func(TryStatementTree::block)

fun  SingleBuilder<out TryStatementTree>.closeParenToken() = optFunc(TryStatementTree::closeParenToken)
fun  OptionalBuilder<out TryStatementTree>.closeParenToken() = optFunc(TryStatementTree::closeParenToken)
fun  ManyBuilder<out TryStatementTree>.closeParenToken() = optFunc(TryStatementTree::closeParenToken)

fun  SingleBuilder<out TryStatementTree>.finallyBlock() = optFunc(TryStatementTree::finallyBlock)
fun  OptionalBuilder<out TryStatementTree>.finallyBlock() = optFunc(TryStatementTree::finallyBlock)
fun  ManyBuilder<out TryStatementTree>.finallyBlock() = optFunc(TryStatementTree::finallyBlock)

fun  SingleBuilder<out TryStatementTree>.finallyKeyword() = optFunc(TryStatementTree::finallyKeyword)
fun  OptionalBuilder<out TryStatementTree>.finallyKeyword() = optFunc(TryStatementTree::finallyKeyword)
fun  ManyBuilder<out TryStatementTree>.finallyKeyword() = optFunc(TryStatementTree::finallyKeyword)

fun  SingleBuilder<out TryStatementTree>.listCatches() = func(TryStatementTree::catches)
fun  OptionalBuilder<out TryStatementTree>.listCatches() = func(TryStatementTree::catches)
fun  ManyBuilder<out TryStatementTree>.listCatches() = func(TryStatementTree::catches)
fun  SingleBuilder<out TryStatementTree>.catches() = listFunc(TryStatementTree::catches)
fun  OptionalBuilder<out TryStatementTree>.catches() = listFunc(TryStatementTree::catches)
fun  ManyBuilder<out TryStatementTree>.catches() = listFunc(TryStatementTree::catches)

fun  SingleBuilder<out TryStatementTree>.listResourceList() = func(TryStatementTree::resourceList)
fun  OptionalBuilder<out TryStatementTree>.listResourceList() = func(TryStatementTree::resourceList)
fun  ManyBuilder<out TryStatementTree>.listResourceList() = func(TryStatementTree::resourceList)
fun  SingleBuilder<out TryStatementTree>.resourceList() = listFunc(TryStatementTree::resourceList)
fun  OptionalBuilder<out TryStatementTree>.resourceList() = listFunc(TryStatementTree::resourceList)
fun  ManyBuilder<out TryStatementTree>.resourceList() = listFunc(TryStatementTree::resourceList)

fun  SingleBuilder<out TryStatementTree>.openParenToken() = optFunc(TryStatementTree::openParenToken)
fun  OptionalBuilder<out TryStatementTree>.openParenToken() = optFunc(TryStatementTree::openParenToken)
fun  ManyBuilder<out TryStatementTree>.openParenToken() = optFunc(TryStatementTree::openParenToken)

fun  SingleBuilder<out TryStatementTree>.tryKeyword() = func(TryStatementTree::tryKeyword)
fun  OptionalBuilder<out TryStatementTree>.tryKeyword() = func(TryStatementTree::tryKeyword)
fun  ManyBuilder<out TryStatementTree>.tryKeyword() = func(TryStatementTree::tryKeyword)
