package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.LambdaExpressionTree
import org.sonarsource.astquery.operation.composite.optFunc
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out LambdaExpressionTree>.arrowToken() = func(LambdaExpressionTree::arrowToken)
fun  OptionalBuilder<out LambdaExpressionTree>.arrowToken() = func(LambdaExpressionTree::arrowToken)
fun  ManyBuilder<out LambdaExpressionTree>.arrowToken() = func(LambdaExpressionTree::arrowToken)

fun  SingleBuilder<out LambdaExpressionTree>.body() = func(LambdaExpressionTree::body)
fun  OptionalBuilder<out LambdaExpressionTree>.body() = func(LambdaExpressionTree::body)
fun  ManyBuilder<out LambdaExpressionTree>.body() = func(LambdaExpressionTree::body)

fun  SingleBuilder<out LambdaExpressionTree>.cfg() = func(LambdaExpressionTree::cfg)
fun  OptionalBuilder<out LambdaExpressionTree>.cfg() = func(LambdaExpressionTree::cfg)
fun  ManyBuilder<out LambdaExpressionTree>.cfg() = func(LambdaExpressionTree::cfg)

fun  SingleBuilder<out LambdaExpressionTree>.closeParenToken() = optFunc(LambdaExpressionTree::closeParenToken)
fun  OptionalBuilder<out LambdaExpressionTree>.closeParenToken() = optFunc(LambdaExpressionTree::closeParenToken)
fun  ManyBuilder<out LambdaExpressionTree>.closeParenToken() = optFunc(LambdaExpressionTree::closeParenToken)

fun  SingleBuilder<out LambdaExpressionTree>.listParameters() = func(LambdaExpressionTree::parameters)
fun  OptionalBuilder<out LambdaExpressionTree>.listParameters() = func(LambdaExpressionTree::parameters)
fun  ManyBuilder<out LambdaExpressionTree>.listParameters() = func(LambdaExpressionTree::parameters)
fun  SingleBuilder<out LambdaExpressionTree>.parameters() = listFunc(LambdaExpressionTree::parameters)
fun  OptionalBuilder<out LambdaExpressionTree>.parameters() = listFunc(LambdaExpressionTree::parameters)
fun  ManyBuilder<out LambdaExpressionTree>.parameters() = listFunc(LambdaExpressionTree::parameters)

fun  SingleBuilder<out LambdaExpressionTree>.openParenToken() = optFunc(LambdaExpressionTree::openParenToken)
fun  OptionalBuilder<out LambdaExpressionTree>.openParenToken() = optFunc(LambdaExpressionTree::openParenToken)
fun  ManyBuilder<out LambdaExpressionTree>.openParenToken() = optFunc(LambdaExpressionTree::openParenToken)

fun  SingleBuilder<out LambdaExpressionTree>.symbol() = func(LambdaExpressionTree::symbol)
fun  OptionalBuilder<out LambdaExpressionTree>.symbol() = func(LambdaExpressionTree::symbol)
fun  ManyBuilder<out LambdaExpressionTree>.symbol() = func(LambdaExpressionTree::symbol)
