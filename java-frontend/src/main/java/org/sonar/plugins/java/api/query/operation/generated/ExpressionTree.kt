package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ExpressionTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ExpressionTree>.asConstant() = func(ExpressionTree::asConstant)
fun  OptionalBuilder<out ExpressionTree>.asConstant() = func(ExpressionTree::asConstant)
fun  ManyBuilder<out ExpressionTree>.asConstant() = func(ExpressionTree::asConstant)

fun  SingleBuilder<out ExpressionTree>.symbolType() = func(ExpressionTree::symbolType)
fun  OptionalBuilder<out ExpressionTree>.symbolType() = func(ExpressionTree::symbolType)
fun  ManyBuilder<out ExpressionTree>.symbolType() = func(ExpressionTree::symbolType)
