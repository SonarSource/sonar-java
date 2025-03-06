package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ConditionalExpressionTree>.colonToken() = func(ConditionalExpressionTree::colonToken)
fun  OptionalBuilder<out ConditionalExpressionTree>.colonToken() = func(ConditionalExpressionTree::colonToken)
fun  ManyBuilder<out ConditionalExpressionTree>.colonToken() = func(ConditionalExpressionTree::colonToken)

fun  SingleBuilder<out ConditionalExpressionTree>.condition() = func(ConditionalExpressionTree::condition)
fun  OptionalBuilder<out ConditionalExpressionTree>.condition() = func(ConditionalExpressionTree::condition)
fun  ManyBuilder<out ConditionalExpressionTree>.condition() = func(ConditionalExpressionTree::condition)

fun  SingleBuilder<out ConditionalExpressionTree>.falseExpression() = func(ConditionalExpressionTree::falseExpression)
fun  OptionalBuilder<out ConditionalExpressionTree>.falseExpression() = func(ConditionalExpressionTree::falseExpression)
fun  ManyBuilder<out ConditionalExpressionTree>.falseExpression() = func(ConditionalExpressionTree::falseExpression)

fun  SingleBuilder<out ConditionalExpressionTree>.questionToken() = func(ConditionalExpressionTree::questionToken)
fun  OptionalBuilder<out ConditionalExpressionTree>.questionToken() = func(ConditionalExpressionTree::questionToken)
fun  ManyBuilder<out ConditionalExpressionTree>.questionToken() = func(ConditionalExpressionTree::questionToken)

fun  SingleBuilder<out ConditionalExpressionTree>.trueExpression() = func(ConditionalExpressionTree::trueExpression)
fun  OptionalBuilder<out ConditionalExpressionTree>.trueExpression() = func(ConditionalExpressionTree::trueExpression)
fun  ManyBuilder<out ConditionalExpressionTree>.trueExpression() = func(ConditionalExpressionTree::trueExpression)
