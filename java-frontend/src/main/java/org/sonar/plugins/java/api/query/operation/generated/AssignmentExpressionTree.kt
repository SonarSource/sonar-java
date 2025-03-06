package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out AssignmentExpressionTree>.expression() = func(AssignmentExpressionTree::expression)
fun  OptionalBuilder<out AssignmentExpressionTree>.expression() = func(AssignmentExpressionTree::expression)
fun  ManyBuilder<out AssignmentExpressionTree>.expression() = func(AssignmentExpressionTree::expression)

fun  SingleBuilder<out AssignmentExpressionTree>.operatorToken() = func(AssignmentExpressionTree::operatorToken)
fun  OptionalBuilder<out AssignmentExpressionTree>.operatorToken() = func(AssignmentExpressionTree::operatorToken)
fun  ManyBuilder<out AssignmentExpressionTree>.operatorToken() = func(AssignmentExpressionTree::operatorToken)

fun  SingleBuilder<out AssignmentExpressionTree>.variable() = func(AssignmentExpressionTree::variable)
fun  OptionalBuilder<out AssignmentExpressionTree>.variable() = func(AssignmentExpressionTree::variable)
fun  ManyBuilder<out AssignmentExpressionTree>.variable() = func(AssignmentExpressionTree::variable)
