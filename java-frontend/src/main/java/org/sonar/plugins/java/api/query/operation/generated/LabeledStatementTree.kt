package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.LabeledStatementTree
import org.sonarsource.astquery.operation.composite.func

fun  SingleBuilder<out LabeledStatementTree>.colonToken() = func(LabeledStatementTree::colonToken)
fun  OptionalBuilder<out LabeledStatementTree>.colonToken() = func(LabeledStatementTree::colonToken)
fun  ManyBuilder<out LabeledStatementTree>.colonToken() = func(LabeledStatementTree::colonToken)

fun  SingleBuilder<out LabeledStatementTree>.label() = func(LabeledStatementTree::label)
fun  OptionalBuilder<out LabeledStatementTree>.label() = func(LabeledStatementTree::label)
fun  ManyBuilder<out LabeledStatementTree>.label() = func(LabeledStatementTree::label)

fun  SingleBuilder<out LabeledStatementTree>.statement() = func(LabeledStatementTree::statement)
fun  OptionalBuilder<out LabeledStatementTree>.statement() = func(LabeledStatementTree::statement)
fun  ManyBuilder<out LabeledStatementTree>.statement() = func(LabeledStatementTree::statement)

fun  SingleBuilder<out LabeledStatementTree>.symbol() = func(LabeledStatementTree::symbol)
fun  OptionalBuilder<out LabeledStatementTree>.symbol() = func(LabeledStatementTree::symbol)
fun  ManyBuilder<out LabeledStatementTree>.symbol() = func(LabeledStatementTree::symbol)
