package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.NewClassTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.optListFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out NewClassTree>.classBody() = optFunc(NewClassTree::classBody)
fun  OptionalBuilder<out NewClassTree>.classBody() = optFunc(NewClassTree::classBody)
fun  ManyBuilder<out NewClassTree>.classBody() = optFunc(NewClassTree::classBody)

fun  SingleBuilder<out NewClassTree>.dotToken() = optFunc(NewClassTree::dotToken)
fun  OptionalBuilder<out NewClassTree>.dotToken() = optFunc(NewClassTree::dotToken)
fun  ManyBuilder<out NewClassTree>.dotToken() = optFunc(NewClassTree::dotToken)

fun  SingleBuilder<out NewClassTree>.enclosingExpression() = optFunc(NewClassTree::enclosingExpression)
fun  OptionalBuilder<out NewClassTree>.enclosingExpression() = optFunc(NewClassTree::enclosingExpression)
fun  ManyBuilder<out NewClassTree>.enclosingExpression() = optFunc(NewClassTree::enclosingExpression)

fun  SingleBuilder<out NewClassTree>.identifier() = func(NewClassTree::identifier)
fun  OptionalBuilder<out NewClassTree>.identifier() = func(NewClassTree::identifier)
fun  ManyBuilder<out NewClassTree>.identifier() = func(NewClassTree::identifier)

fun  SingleBuilder<out NewClassTree>.listArguments() = func(NewClassTree::arguments)
fun  OptionalBuilder<out NewClassTree>.listArguments() = func(NewClassTree::arguments)
fun  ManyBuilder<out NewClassTree>.listArguments() = func(NewClassTree::arguments)
fun  SingleBuilder<out NewClassTree>.arguments() = listFunc(NewClassTree::arguments)
fun  OptionalBuilder<out NewClassTree>.arguments() = listFunc(NewClassTree::arguments)
fun  ManyBuilder<out NewClassTree>.arguments() = listFunc(NewClassTree::arguments)

fun  SingleBuilder<out NewClassTree>.listTypeArguments() = optFunc(NewClassTree::typeArguments)
fun  OptionalBuilder<out NewClassTree>.listTypeArguments() = optFunc(NewClassTree::typeArguments)
fun  ManyBuilder<out NewClassTree>.listTypeArguments() = optFunc(NewClassTree::typeArguments)
fun  SingleBuilder<out NewClassTree>.typeArguments() = optListFunc(NewClassTree::typeArguments)
fun  OptionalBuilder<out NewClassTree>.typeArguments() = optListFunc(NewClassTree::typeArguments)
fun  ManyBuilder<out NewClassTree>.typeArguments() = optListFunc(NewClassTree::typeArguments)

fun  SingleBuilder<out NewClassTree>.methodSymbol() = func(NewClassTree::methodSymbol)
fun  OptionalBuilder<out NewClassTree>.methodSymbol() = func(NewClassTree::methodSymbol)
fun  ManyBuilder<out NewClassTree>.methodSymbol() = func(NewClassTree::methodSymbol)

fun  SingleBuilder<out NewClassTree>.newKeyword() = optFunc(NewClassTree::newKeyword)
fun  OptionalBuilder<out NewClassTree>.newKeyword() = optFunc(NewClassTree::newKeyword)
fun  ManyBuilder<out NewClassTree>.newKeyword() = optFunc(NewClassTree::newKeyword)
