package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.BinaryExpressionTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out BinaryExpressionTree>.leftOperand() = func(BinaryExpressionTree::leftOperand)
fun  OptionalBuilder<out BinaryExpressionTree>.leftOperand() = func(BinaryExpressionTree::leftOperand)
fun  ManyBuilder<out BinaryExpressionTree>.leftOperand() = func(BinaryExpressionTree::leftOperand)

fun  SingleBuilder<out BinaryExpressionTree>.operatorToken() = func(BinaryExpressionTree::operatorToken)
fun  OptionalBuilder<out BinaryExpressionTree>.operatorToken() = func(BinaryExpressionTree::operatorToken)
fun  ManyBuilder<out BinaryExpressionTree>.operatorToken() = func(BinaryExpressionTree::operatorToken)

fun  SingleBuilder<out BinaryExpressionTree>.rightOperand() = func(BinaryExpressionTree::rightOperand)
fun  OptionalBuilder<out BinaryExpressionTree>.rightOperand() = func(BinaryExpressionTree::rightOperand)
fun  ManyBuilder<out BinaryExpressionTree>.rightOperand() = func(BinaryExpressionTree::rightOperand)
