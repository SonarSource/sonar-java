package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.ParenthesizedTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out ParenthesizedTree>.closeParenToken() = func(ParenthesizedTree::closeParenToken)
fun  OptionalBuilder<out ParenthesizedTree>.closeParenToken() = func(ParenthesizedTree::closeParenToken)
fun  ManyBuilder<out ParenthesizedTree>.closeParenToken() = func(ParenthesizedTree::closeParenToken)

fun  SingleBuilder<out ParenthesizedTree>.expression() = func(ParenthesizedTree::expression)
fun  OptionalBuilder<out ParenthesizedTree>.expression() = func(ParenthesizedTree::expression)
fun  ManyBuilder<out ParenthesizedTree>.expression() = func(ParenthesizedTree::expression)

fun  SingleBuilder<out ParenthesizedTree>.openParenToken() = func(ParenthesizedTree::openParenToken)
fun  OptionalBuilder<out ParenthesizedTree>.openParenToken() = func(ParenthesizedTree::openParenToken)
fun  ManyBuilder<out ParenthesizedTree>.openParenToken() = func(ParenthesizedTree::openParenToken)
