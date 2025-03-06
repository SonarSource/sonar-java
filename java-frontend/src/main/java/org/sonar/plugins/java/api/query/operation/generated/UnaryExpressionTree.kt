package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.UnaryExpressionTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out UnaryExpressionTree>.expression() = func(UnaryExpressionTree::expression)
fun  OptionalBuilder<out UnaryExpressionTree>.expression() = func(UnaryExpressionTree::expression)
fun  ManyBuilder<out UnaryExpressionTree>.expression() = func(UnaryExpressionTree::expression)

fun  SingleBuilder<out UnaryExpressionTree>.operatorToken() = func(UnaryExpressionTree::operatorToken)
fun  OptionalBuilder<out UnaryExpressionTree>.operatorToken() = func(UnaryExpressionTree::operatorToken)
fun  ManyBuilder<out UnaryExpressionTree>.operatorToken() = func(UnaryExpressionTree::operatorToken)
