package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ArrayAccessExpressionTree>.dimension() = func(ArrayAccessExpressionTree::dimension)
fun  OptionalBuilder<out ArrayAccessExpressionTree>.dimension() = func(ArrayAccessExpressionTree::dimension)
fun  ManyBuilder<out ArrayAccessExpressionTree>.dimension() = func(ArrayAccessExpressionTree::dimension)

fun  SingleBuilder<out ArrayAccessExpressionTree>.expression() = func(ArrayAccessExpressionTree::expression)
fun  OptionalBuilder<out ArrayAccessExpressionTree>.expression() = func(ArrayAccessExpressionTree::expression)
fun  ManyBuilder<out ArrayAccessExpressionTree>.expression() = func(ArrayAccessExpressionTree::expression)
